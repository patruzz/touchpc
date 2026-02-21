"""Entry point for the TouchPC Remote Control Server.

Usage::

    python -m remotecontrol [--port PORT] [--no-gui] [--no-mdns] [-v]
"""

from __future__ import annotations

import asyncio
import argparse
import logging

from .server import TouchPCServer
from .config import DEFAULT_PORT


def main() -> None:
    parser = argparse.ArgumentParser(description="TouchPC Remote Control Server")
    parser.add_argument(
        "--port",
        type=int,
        default=DEFAULT_PORT,
        help=f"WebSocket listen port (default: {DEFAULT_PORT})",
    )
    parser.add_argument(
        "--no-gui",
        action="store_true",
        help="Disable system tray icon and QR window",
    )
    parser.add_argument(
        "--no-mdns",
        action="store_true",
        help="Disable mDNS / Zeroconf service advertisement",
    )
    parser.add_argument(
        "--verbose", "-v",
        action="store_true",
        help="Enable debug logging",
    )
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    )

    server = TouchPCServer(
        port=args.port,
        enable_gui=not args.no_gui,
        enable_mdns=not args.no_mdns,
    )

    try:
        asyncio.run(server.start())
    except KeyboardInterrupt:
        logging.getLogger(__name__).info("Interrupted by user")


if __name__ == "__main__":
    main()
