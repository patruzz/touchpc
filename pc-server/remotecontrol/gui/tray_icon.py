"""System tray icon for the TouchPC server using pystray."""

from __future__ import annotations

import logging
import threading
from typing import Callable, Optional

from PIL import Image, ImageDraw
import pystray

logger = logging.getLogger(__name__)

# Icon dimensions
_ICON_SIZE = 64


def _create_icon_image(connected: bool) -> Image.Image:
    """Create a simple circular icon.

    Green circle when a client is connected, grey when idle.
    """
    img = Image.new("RGBA", (_ICON_SIZE, _ICON_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    color = (0, 200, 80, 255) if connected else (160, 160, 160, 255)
    margin = 4
    draw.ellipse(
        [margin, margin, _ICON_SIZE - margin, _ICON_SIZE - margin],
        fill=color,
        outline=(40, 40, 40, 255),
        width=2,
    )
    # Small "T" letter in center
    text_color = (255, 255, 255, 255)
    try:
        draw.text(
            (_ICON_SIZE // 2 - 5, _ICON_SIZE // 2 - 8),
            "T",
            fill=text_color,
        )
    except Exception:
        pass  # font rendering may not be available in all environments
    return img


class TrayIcon:
    """System tray icon with context menu for the TouchPC server.

    The tray runs on its own thread so it does not block the asyncio loop.
    """

    def __init__(
        self,
        on_show_qr: Optional[Callable[[], None]] = None,
        on_show_pin: Optional[Callable[[], None]] = None,
        on_quit: Optional[Callable[[], None]] = None,
    ) -> None:
        self._on_show_qr = on_show_qr
        self._on_show_pin = on_show_pin
        self._on_quit = on_quit
        self._connected = False
        self._icon: Optional[pystray.Icon] = None
        self._thread: Optional[threading.Thread] = None

    def start(self) -> None:
        """Create the tray icon and start its event loop in a daemon thread."""
        menu = pystray.Menu(
            pystray.MenuItem("Show QR Code", self._menu_show_qr),
            pystray.MenuItem("Show PIN", self._menu_show_pin),
            pystray.Menu.SEPARATOR,
            pystray.MenuItem("Quit", self._menu_quit),
        )

        self._icon = pystray.Icon(
            name="TouchPC",
            icon=_create_icon_image(self._connected),
            title="TouchPC Server",
            menu=menu,
        )

        self._thread = threading.Thread(target=self._icon.run, daemon=True)
        self._thread.start()
        logger.info("Tray icon started")

    def stop(self) -> None:
        """Stop the tray icon."""
        if self._icon is not None:
            self._icon.stop()
            self._icon = None
        logger.info("Tray icon stopped")

    def set_status(self, connected: bool) -> None:
        """Update the icon colour to reflect connection state."""
        self._connected = connected
        if self._icon is not None:
            self._icon.icon = _create_icon_image(connected)
            self._icon.title = (
                "TouchPC Server - Connected" if connected else "TouchPC Server - Idle"
            )

    # -- menu callbacks -------------------------------------------------------

    def _menu_show_qr(self, icon: pystray.Icon, item: pystray.MenuItem) -> None:
        if self._on_show_qr is not None:
            self._on_show_qr()

    def _menu_show_pin(self, icon: pystray.Icon, item: pystray.MenuItem) -> None:
        if self._on_show_pin is not None:
            self._on_show_pin()

    def _menu_quit(self, icon: pystray.Icon, item: pystray.MenuItem) -> None:
        if self._on_quit is not None:
            self._on_quit()
        self.stop()
