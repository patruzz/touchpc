"""Abstract base class for TouchPC transport servers."""

from __future__ import annotations

import abc
from typing import Awaitable, Callable, Optional


# Callback signatures
OnMessageCallback = Callable[[str, bytes], Awaitable[None]]       # (client_id, data)
OnConnectCallback = Callable[[str, str], Awaitable[None]]         # (client_id, remote_addr)
OnDisconnectCallback = Callable[[str], Awaitable[None]]           # (client_id,)


class TransportServer(abc.ABC):
    """Base contract that every transport backend must implement."""

    def __init__(self) -> None:
        self._on_message: Optional[OnMessageCallback] = None
        self._on_connect: Optional[OnConnectCallback] = None
        self._on_disconnect: Optional[OnDisconnectCallback] = None

    # -- callback registration ------------------------------------------------

    @property
    def on_message_callback(self) -> Optional[OnMessageCallback]:
        return self._on_message

    @on_message_callback.setter
    def on_message_callback(self, cb: OnMessageCallback) -> None:
        self._on_message = cb

    @property
    def on_connect_callback(self) -> Optional[OnConnectCallback]:
        return self._on_connect

    @on_connect_callback.setter
    def on_connect_callback(self, cb: OnConnectCallback) -> None:
        self._on_connect = cb

    @property
    def on_disconnect_callback(self) -> Optional[OnDisconnectCallback]:
        return self._on_disconnect

    @on_disconnect_callback.setter
    def on_disconnect_callback(self, cb: OnDisconnectCallback) -> None:
        self._on_disconnect = cb

    # -- abstract interface ---------------------------------------------------

    @abc.abstractmethod
    async def start(self, host: str, port: int) -> None:
        """Start listening for connections on *host*:*port*."""

    @abc.abstractmethod
    async def stop(self) -> None:
        """Gracefully shut down the transport and disconnect all clients."""

    @abc.abstractmethod
    async def send(self, client_id: str, data: bytes) -> None:
        """Send raw *data* to the client identified by *client_id*."""

    @abc.abstractmethod
    async def disconnect_client(self, client_id: str) -> None:
        """Force-close the connection to *client_id*."""
