"""QR code generation and display window for TouchPC connection info."""

from __future__ import annotations

import logging
import threading
import tkinter as tk
from typing import Optional
from urllib.parse import quote

import qrcode
from PIL import Image, ImageTk

logger = logging.getLogger(__name__)


class QRWindow:
    """Generates and displays a QR code containing the server connection URL.

    The QR code encodes a URI of the form::

        touchpc://<ip>:<port>?name=<server_name>

    The window is shown using tkinter, running on its own thread to avoid
    blocking the asyncio event loop.
    """

    def __init__(self) -> None:
        self._root: Optional[tk.Tk] = None
        self._thread: Optional[threading.Thread] = None
        self._photo_image: Optional[ImageTk.PhotoImage] = None

    @staticmethod
    def generate_qr(ip: str, port: int, server_name: str) -> Image.Image:
        """Build a QR code PIL Image for the given connection info.

        Returns
        -------
        PIL.Image.Image
            The rendered QR code (RGB mode).
        """
        uri = f"touchpc://{ip}:{port}?name={quote(server_name)}"
        qr = qrcode.QRCode(
            version=None,
            error_correction=qrcode.constants.ERROR_CORRECT_M,
            box_size=10,
            border=4,
        )
        qr.add_data(uri)
        qr.make(fit=True)
        img = qr.make_image(fill_color="black", back_color="white").convert("RGB")
        return img

    def show(self, ip: str, port: int, server_name: str) -> None:
        """Display the QR code window.

        If the window is already open it is brought to the front.
        """
        # Close any existing window first
        self.hide()

        self._thread = threading.Thread(
            target=self._run_window,
            args=(ip, port, server_name),
            daemon=True,
        )
        self._thread.start()

    def hide(self) -> None:
        """Close the QR window if it is open."""
        if self._root is not None:
            try:
                self._root.after(0, self._root.destroy)
            except Exception:
                pass
            self._root = None

    def _run_window(self, ip: str, port: int, server_name: str) -> None:
        """Tkinter main loop -- runs in its own thread."""
        try:
            root = tk.Tk()
            self._root = root
            root.title("TouchPC - Scan to Connect")
            root.resizable(False, False)

            qr_img = self.generate_qr(ip, port, server_name)
            # Resize for display
            display_size = 300
            qr_img = qr_img.resize((display_size, display_size), Image.NEAREST)

            self._photo_image = ImageTk.PhotoImage(qr_img)

            label_title = tk.Label(
                root,
                text="Scan this QR code with the TouchPC app",
                font=("Arial", 14, "bold"),
                pady=10,
            )
            label_title.pack()

            label_img = tk.Label(root, image=self._photo_image)
            label_img.pack(padx=20)

            label_info = tk.Label(
                root,
                text=f"Server: {server_name}\nAddress: {ip}:{port}",
                font=("Arial", 11),
                pady=10,
            )
            label_info.pack()

            btn_close = tk.Button(root, text="Close", command=root.destroy, width=12)
            btn_close.pack(pady=(0, 15))

            # Center on screen
            root.update_idletasks()
            w = root.winfo_width()
            h = root.winfo_height()
            x = (root.winfo_screenwidth() - w) // 2
            y = (root.winfo_screenheight() - h) // 2
            root.geometry(f"+{x}+{y}")

            root.mainloop()
        except Exception:
            logger.exception("Error in QR window")
        finally:
            self._root = None
            self._photo_image = None
