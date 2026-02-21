"""Rate limiter for failed authentication attempts."""

from __future__ import annotations

import logging
import time
from typing import Dict

logger = logging.getLogger(__name__)


class _AttemptRecord:
    """Track failures for a single IP address."""
    __slots__ = ("failures", "lockout_until")

    def __init__(self) -> None:
        self.failures: int = 0
        self.lockout_until: float = 0.0


class RateLimiter:
    """Tracks failed authentication attempts per IP and imposes a time-based
    lockout after too many consecutive failures.

    Parameters
    ----------
    max_attempts:
        Number of consecutive failures before the IP is locked out.
    lockout_time:
        Duration (in seconds) that the lockout remains active.
    """

    def __init__(self, max_attempts: int = 5, lockout_time: float = 300.0) -> None:
        self._max_attempts = max_attempts
        self._lockout_time = lockout_time
        self._records: Dict[str, _AttemptRecord] = {}

    def is_locked(self, ip: str) -> bool:
        """Return ``True`` if *ip* is currently locked out."""
        record = self._records.get(ip)
        if record is None:
            return False
        if record.lockout_until > 0 and time.monotonic() < record.lockout_until:
            return True
        # Lockout has expired -- reset
        if record.lockout_until > 0 and time.monotonic() >= record.lockout_until:
            record.failures = 0
            record.lockout_until = 0.0
        return False

    def record_failure(self, ip: str) -> None:
        """Record a failed auth attempt from *ip*.

        If the number of consecutive failures reaches ``max_attempts`` the IP
        is locked out for ``lockout_time`` seconds.
        """
        record = self._records.setdefault(ip, _AttemptRecord())
        record.failures += 1
        logger.debug("Auth failure #%d from %s", record.failures, ip)

        if record.failures >= self._max_attempts:
            record.lockout_until = time.monotonic() + self._lockout_time
            logger.warning(
                "IP %s locked out for %.0f seconds after %d failures",
                ip,
                self._lockout_time,
                record.failures,
            )

    def record_success(self, ip: str) -> None:
        """Clear the failure counter for *ip* after a successful auth."""
        record = self._records.get(ip)
        if record is not None:
            record.failures = 0
            record.lockout_until = 0.0
            logger.debug("Auth success from %s, counter reset", ip)
