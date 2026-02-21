"""Network helper utilities."""

from __future__ import annotations

import socket


def get_local_ip() -> str:
    """Return the primary non-loopback IPv4 address of this machine.

    The trick is to open a UDP socket aimed at a public address (we never
    actually send data) and read back the local address the OS chose.
    """
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            # Does not actually send anything
            s.connect(("8.8.8.8", 80))
            ip: str = s.getsockname()[0]
            return ip
    except OSError:
        return "127.0.0.1"


def is_port_available(port: int, host: str = "0.0.0.0") -> bool:
    """Check whether *port* can be bound on *host*.

    Returns ``True`` if the port is free, ``False`` otherwise.
    """
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            s.bind((host, port))
            return True
    except OSError:
        return False
