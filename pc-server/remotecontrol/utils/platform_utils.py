"""Platform-detection and screen-size utilities."""

from __future__ import annotations

import platform
import socket
from typing import Tuple


def get_platform() -> str:
    """Return a normalised platform string: ``'windows'``, ``'linux'``, or ``'macos'``."""
    system = platform.system().lower()
    if system == "darwin":
        return "macos"
    if system == "windows":
        return "windows"
    return "linux"


def get_screen_size() -> Tuple[int, int]:
    """Return ``(width, height)`` of the primary monitor in pixels.

    Uses platform-specific APIs to avoid requiring heavy GUI toolkits.
    Falls back to (1920, 1080) if detection fails.
    """
    plat = get_platform()

    if plat == "windows":
        try:
            import ctypes

            user32 = ctypes.windll.user32  # type: ignore[attr-defined]
            user32.SetProcessDPIAware()
            width = user32.GetSystemMetrics(0)
            height = user32.GetSystemMetrics(1)
            return (width, height)
        except Exception:
            pass

    if plat == "macos":
        try:
            from AppKit import NSScreen  # type: ignore[import-untyped]

            frame = NSScreen.mainScreen().frame()
            return (int(frame.size.width), int(frame.size.height))
        except Exception:
            pass

    if plat == "linux":
        try:
            import subprocess

            output = subprocess.check_output(
                ["xrandr", "--query"], text=True, timeout=5
            )
            for line in output.splitlines():
                if " connected primary " in line or " connected " in line:
                    # Example: "1920x1080+0+0"
                    for token in line.split():
                        if "x" in token and "+" in token:
                            res = token.split("+")[0]
                            w, h = res.split("x")
                            return (int(w), int(h))
        except Exception:
            pass

    # Fallback
    return (1920, 1080)


def get_hostname() -> str:
    """Return the machine's hostname."""
    return socket.gethostname()
