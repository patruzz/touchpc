"""mDNS/Zeroconf service advertisement for TouchPC server discovery."""

from __future__ import annotations

import logging
import socket
from typing import Optional

from zeroconf import ServiceInfo, Zeroconf

from ..config import SERVICE_TYPE
from ..utils.network_utils import get_local_ip

logger = logging.getLogger(__name__)


class MdnsAdvertiser:
    """Publishes a ``_touchpc._tcp.local.`` mDNS service so that Android
    clients on the same LAN can discover the server automatically."""

    def __init__(self) -> None:
        self._zeroconf: Optional[Zeroconf] = None
        self._service_info: Optional[ServiceInfo] = None

    def register_service(self, port: int, server_name: str) -> None:
        """Register the TouchPC service on the network.

        Parameters
        ----------
        port:
            TCP port the WebSocket server is listening on.
        server_name:
            Human-readable name of this server instance (usually the hostname).
        """
        ip = get_local_ip()
        ip_bytes = socket.inet_aton(ip)

        # Zeroconf requires the service name to end with the service type
        full_name = f"{server_name}.{SERVICE_TYPE}"

        self._service_info = ServiceInfo(
            type_=SERVICE_TYPE,
            name=full_name,
            addresses=[ip_bytes],
            port=port,
            properties={
                "version": "1",
                "name": server_name,
            },
            server=f"{server_name}.local.",
        )

        self._zeroconf = Zeroconf()
        self._zeroconf.register_service(self._service_info)
        logger.info(
            "mDNS service registered: %s at %s:%d",
            full_name,
            ip,
            port,
        )

    def unregister_service(self) -> None:
        """Remove the service from the network and close Zeroconf."""
        if self._zeroconf is not None and self._service_info is not None:
            self._zeroconf.unregister_service(self._service_info)
            logger.info("mDNS service unregistered")
        if self._zeroconf is not None:
            self._zeroconf.close()
            self._zeroconf = None
        self._service_info = None
