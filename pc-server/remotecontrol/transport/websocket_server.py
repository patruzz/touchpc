"""WebSocket transport server implementation using the ``websockets`` library."""

from __future__ import annotations

import asyncio
import logging
import uuid
from typing import Dict, Optional

import websockets
import websockets.asyncio.server as ws_server

from ..config import HEARTBEAT_TIMEOUT
from .base import TransportServer

logger = logging.getLogger(__name__)


class _ClientInfo:
    """Metadata kept for every connected WebSocket client."""

    __slots__ = ("ws", "remote_addr", "last_activity")

    def __init__(self, ws: ws_server.ServerConnection, remote_addr: str) -> None:
        self.ws = ws
        self.remote_addr = remote_addr
        self.last_activity: float = asyncio.get_event_loop().time()


class WebSocketTransportServer(TransportServer):
    """Concrete transport backed by a WebSocket server.

    * Binary message mode.
    * Tracks connected clients by a UUID-based client_id.
    * Runs a background task that evicts clients whose last activity
      exceeds ``HEARTBEAT_TIMEOUT``.
    """

    def __init__(self) -> None:
        super().__init__()
        self._clients: Dict[str, _ClientInfo] = {}
        self._server: Optional[ws_server.WebSocketServer] = None
        self._monitor_task: Optional[asyncio.Task[None]] = None

    # -- public interface -----------------------------------------------------

    async def start(self, host: str, port: int) -> None:
        self._server = await ws_server.serve(
            self._handler,
            host,
            port,
        )
        self._monitor_task = asyncio.create_task(self._heartbeat_monitor())
        logger.info("WebSocket server listening on %s:%d", host, port)

    async def stop(self) -> None:
        if self._monitor_task is not None:
            self._monitor_task.cancel()
            try:
                await self._monitor_task
            except asyncio.CancelledError:
                pass
            self._monitor_task = None

        if self._server is not None:
            self._server.close()
            await self._server.wait_closed()
            self._server = None

        self._clients.clear()
        logger.info("WebSocket server stopped")

    async def send(self, client_id: str, data: bytes) -> None:
        info = self._clients.get(client_id)
        if info is None:
            logger.warning("send: unknown client %s", client_id)
            return
        try:
            await info.ws.send(data)
        except websockets.exceptions.ConnectionClosed:
            logger.debug("send: connection already closed for %s", client_id)

    async def disconnect_client(self, client_id: str) -> None:
        info = self._clients.pop(client_id, None)
        if info is not None:
            await info.ws.close()
            logger.info("Disconnected client %s", client_id)

    # -- internal -------------------------------------------------------------

    async def _handler(self, ws: ws_server.ServerConnection) -> None:
        client_id = str(uuid.uuid4())
        remote = ws.remote_address
        remote_addr = f"{remote[0]}:{remote[1]}" if isinstance(remote, tuple) else str(remote)

        self._clients[client_id] = _ClientInfo(ws, remote_addr)
        logger.info("Client connected: %s from %s", client_id, remote_addr)

        if self._on_connect is not None:
            await self._on_connect(client_id, remote_addr)

        try:
            async for message in ws:
                if isinstance(message, bytes):
                    info = self._clients.get(client_id)
                    if info is not None:
                        info.last_activity = asyncio.get_event_loop().time()
                    if self._on_message is not None:
                        await self._on_message(client_id, message)
                else:
                    logger.warning("Ignoring text message from %s", client_id)
        except websockets.exceptions.ConnectionClosed as exc:
            logger.debug("Connection closed for %s: %s", client_id, exc)
        finally:
            self._clients.pop(client_id, None)
            logger.info("Client disconnected: %s", client_id)
            if self._on_disconnect is not None:
                await self._on_disconnect(client_id)

    async def _heartbeat_monitor(self) -> None:
        """Periodically check for clients that have gone silent."""
        while True:
            await asyncio.sleep(HEARTBEAT_TIMEOUT / 2)
            now = asyncio.get_event_loop().time()
            stale: list[str] = []
            for cid, info in list(self._clients.items()):
                if now - info.last_activity > HEARTBEAT_TIMEOUT:
                    stale.append(cid)

            for cid in stale:
                logger.warning("Heartbeat timeout for client %s, disconnecting", cid)
                await self.disconnect_client(cid)
                if self._on_disconnect is not None:
                    await self._on_disconnect(cid)
