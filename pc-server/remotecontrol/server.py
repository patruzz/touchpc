"""Main orchestrator for the TouchPC Remote Control Server."""

from __future__ import annotations

import asyncio
import logging
from typing import Dict, Optional, Set

from .config import DEFAULT_PORT
from .discovery.mdns_advertiser import MdnsAdvertiser
from .executor.pynput_executor import PynputExecutor
from .gui.qr_window import QRWindow
from .gui.tray_icon import TrayIcon
from .protocol.commands import (
    Command,
    Handshake,
    HandshakeAck,
    HeartbeatPing,
    HeartbeatPong,
)
from .protocol.deserializer import ProtocolError, deserialize, serialize
from .security.pin_auth import PinAuthenticator
from .security.rate_limiter import RateLimiter
from .transport.websocket_server import WebSocketTransportServer
from .utils.network_utils import get_local_ip
from .utils.platform_utils import get_hostname, get_screen_size

logger = logging.getLogger(__name__)


class _ClientState:
    """Per-client session state."""
    __slots__ = ("authenticated", "remote_addr")

    def __init__(self, remote_addr: str) -> None:
        self.authenticated: bool = False
        self.remote_addr: str = remote_addr


class TouchPCServer:
    """Top-level server that wires together transport, executor, discovery,
    authentication, and GUI components.

    Parameters
    ----------
    port:
        TCP port for the WebSocket server.
    enable_gui:
        Whether to show the system-tray icon and QR window.
    enable_mdns:
        Whether to advertise the service via mDNS / Zeroconf.
    """

    def __init__(
        self,
        port: int = DEFAULT_PORT,
        enable_gui: bool = True,
        enable_mdns: bool = True,
    ) -> None:
        self._port = port
        self._enable_gui = enable_gui
        self._enable_mdns = enable_mdns

        # Core components
        self._transport = WebSocketTransportServer()
        self._executor = PynputExecutor()
        self._pin_auth = PinAuthenticator()
        self._rate_limiter = RateLimiter()

        # Optional components
        self._mdns: Optional[MdnsAdvertiser] = None
        self._tray: Optional[TrayIcon] = None
        self._qr_window: Optional[QRWindow] = None

        # Client tracking
        self._clients: Dict[str, _ClientState] = {}
        self._authenticated_clients: Set[str] = set()

        # Event loop reference (set in start())
        self._loop: Optional[asyncio.AbstractEventLoop] = None
        self._shutdown_event: Optional[asyncio.Event] = None

    # ------------------------------------------------------------------
    # Lifecycle
    # ------------------------------------------------------------------

    async def start(self) -> None:
        """Start all server components and block until shutdown."""
        self._loop = asyncio.get_running_loop()
        self._shutdown_event = asyncio.Event()

        server_name = get_hostname()
        local_ip = get_local_ip()
        screen_w, screen_h = get_screen_size()

        logger.info("TouchPC Server v1.0.0")
        logger.info("Server name : %s", server_name)
        logger.info("Local IP    : %s", local_ip)
        logger.info("Port        : %d", self._port)
        logger.info("Screen      : %dx%d", screen_w, screen_h)
        logger.info("PIN         : %s", self._pin_auth.current_pin)

        # Wire transport callbacks
        self._transport.on_message_callback = self._handle_message
        self._transport.on_connect_callback = self._handle_connect
        self._transport.on_disconnect_callback = self._handle_disconnect

        # Start WebSocket server
        await self._transport.start("0.0.0.0", self._port)

        # mDNS
        if self._enable_mdns:
            self._mdns = MdnsAdvertiser()
            try:
                self._mdns.register_service(self._port, server_name)
            except Exception:
                logger.exception("Failed to register mDNS service")
                self._mdns = None

        # GUI
        if self._enable_gui:
            self._qr_window = QRWindow()

            def _show_qr() -> None:
                if self._qr_window is not None:
                    self._qr_window.show(local_ip, self._port, server_name)

            def _show_pin() -> None:
                logger.info("Current PIN: %s", self._pin_auth.current_pin)
                # Also print to console for non-GUI environments
                print(f"\n  Current PIN: {self._pin_auth.current_pin}\n")

            def _quit() -> None:
                if self._loop is not None and self._shutdown_event is not None:
                    self._loop.call_soon_threadsafe(self._shutdown_event.set)

            self._tray = TrayIcon(
                on_show_qr=_show_qr,
                on_show_pin=_show_pin,
                on_quit=_quit,
            )
            self._tray.start()

        logger.info("Server started. Waiting for connections...")

        # Block until shutdown is requested
        await self._shutdown_event.wait()
        await self.stop()

    async def stop(self) -> None:
        """Gracefully shut down all components."""
        logger.info("Shutting down...")

        await self._transport.stop()

        if self._mdns is not None:
            self._mdns.unregister_service()

        if self._tray is not None:
            self._tray.stop()

        if self._qr_window is not None:
            self._qr_window.hide()

        self._clients.clear()
        self._authenticated_clients.clear()

        logger.info("Server stopped.")

    # ------------------------------------------------------------------
    # Transport callbacks
    # ------------------------------------------------------------------

    async def _handle_connect(self, client_id: str, remote_addr: str) -> None:
        """Called when a new WebSocket connection is established."""
        logger.info("New connection: %s from %s", client_id, remote_addr)
        self._clients[client_id] = _ClientState(remote_addr=remote_addr)

    async def _handle_disconnect(self, client_id: str) -> None:
        """Called when a client disconnects."""
        logger.info("Client disconnected: %s", client_id)
        self._clients.pop(client_id, None)
        was_authenticated = client_id in self._authenticated_clients
        self._authenticated_clients.discard(client_id)

        if was_authenticated and not self._authenticated_clients:
            # Last authenticated client left
            if self._tray is not None:
                self._tray.set_status(connected=False)

    async def _handle_message(self, client_id: str, data: bytes) -> None:
        """Deserialize an incoming binary message and route it."""
        try:
            command = deserialize(data)
        except ProtocolError as exc:
            logger.warning("Protocol error from %s: %s", client_id, exc)
            return

        # Always allow heartbeat through regardless of auth state
        if isinstance(command, HeartbeatPing):
            await self._handle_heartbeat(client_id, command)
            return

        # Handshake
        if isinstance(command, Handshake):
            await self._handle_handshake(client_id, command)
            return

        # All other commands require authentication
        if client_id not in self._authenticated_clients:
            logger.warning(
                "Unauthenticated command (%s) from %s, ignoring",
                type(command).__name__,
                client_id,
            )
            return

        # Execute the input command
        try:
            self._executor.execute(command)
        except Exception:
            logger.exception("Error executing %s", type(command).__name__)

    # ------------------------------------------------------------------
    # Handshake
    # ------------------------------------------------------------------

    async def _handle_handshake(self, client_id: str, command: Handshake) -> None:
        """Verify the client PIN and send a HandshakeAck."""
        state = self._clients.get(client_id)
        if state is None:
            return

        # Rate limiting by IP
        ip = state.remote_addr.split(":")[0]
        if self._rate_limiter.is_locked(ip):
            logger.warning("Rate-limited handshake attempt from %s", ip)
            ack = HandshakeAck(
                success=False,
                screen_width=0,
                screen_height=0,
                server_name="",
            )
            await self._transport.send(client_id, serialize(ack))
            return

        success = self._pin_auth.verify_handshake(command.pin_hash, command.nonce)

        if success:
            self._rate_limiter.record_success(ip)
            state.authenticated = True
            self._authenticated_clients.add(client_id)
            screen_w, screen_h = get_screen_size()
            server_name = get_hostname()

            ack = HandshakeAck(
                success=True,
                screen_width=screen_w,
                screen_height=screen_h,
                server_name=server_name,
            )

            if self._tray is not None:
                self._tray.set_status(connected=True)

            logger.info("Client %s authenticated successfully", client_id)
        else:
            self._rate_limiter.record_failure(ip)
            ack = HandshakeAck(
                success=False,
                screen_width=0,
                screen_height=0,
                server_name="",
            )
            logger.warning("Client %s failed authentication", client_id)

        await self._transport.send(client_id, serialize(ack))

    # ------------------------------------------------------------------
    # Heartbeat
    # ------------------------------------------------------------------

    async def _handle_heartbeat(self, client_id: str, command: HeartbeatPing) -> None:
        """Respond to a HeartbeatPing with a HeartbeatPong."""
        pong = HeartbeatPong(timestamp=command.timestamp)
        await self._transport.send(client_id, serialize(pong))
