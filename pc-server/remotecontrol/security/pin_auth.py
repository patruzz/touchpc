"""PIN-based authentication for TouchPC handshake."""

from __future__ import annotations

import hashlib
import logging
import os
import random
import string

from ..config import PIN_LENGTH

logger = logging.getLogger(__name__)


class PinAuthenticator:
    """Generates a numeric PIN and verifies client handshake hashes.

    The verification scheme works as follows:

    1. The server generates a random 6-digit PIN and displays it to the user.
    2. The client obtains the PIN from the user, generates a 16-byte nonce,
       computes ``SHA-256(pin_utf8_bytes + nonce)`` and sends the hash + nonce
       in the Handshake message.
    3. The server re-computes the same hash with its own copy of the PIN and
       compares.
    """

    def __init__(self) -> None:
        self._current_pin: str = self.generate_pin()
        logger.info("Generated new PIN: %s", self._current_pin)

    @property
    def current_pin(self) -> str:
        """Return the current active PIN string."""
        return self._current_pin

    def regenerate_pin(self) -> str:
        """Replace the current PIN with a freshly generated one."""
        self._current_pin = self.generate_pin()
        logger.info("Regenerated PIN: %s", self._current_pin)
        return self._current_pin

    @staticmethod
    def generate_pin() -> str:
        """Generate a cryptographically random numeric PIN.

        Returns
        -------
        str
            A string of ``PIN_LENGTH`` random digits.
        """
        return "".join(random.SystemRandom().choices(string.digits, k=PIN_LENGTH))

    @staticmethod
    def generate_nonce() -> bytes:
        """Generate 16 cryptographically random bytes for use as a nonce."""
        return os.urandom(16)

    def verify_handshake(self, pin_hash: bytes, nonce: bytes) -> bool:
        """Check whether the client-provided hash matches our PIN.

        Parameters
        ----------
        pin_hash:
            32-byte SHA-256 digest sent by the client.
        nonce:
            16-byte nonce sent alongside the hash.

        Returns
        -------
        bool
            ``True`` if the hash matches, ``False`` otherwise.
        """
        expected = hashlib.sha256(
            self._current_pin.encode("utf-8") + nonce
        ).digest()
        match = expected == pin_hash
        if not match:
            logger.warning("PIN verification failed")
        else:
            logger.info("PIN verification succeeded")
        return match
