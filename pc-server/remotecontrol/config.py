"""Configuration constants for TouchPC Remote Control Server."""

# Network
DEFAULT_PORT: int = 9876

# mDNS / Zeroconf
SERVICE_TYPE: str = "_touchpc._tcp.local."

# Heartbeat
HEARTBEAT_INTERVAL: float = 5.0  # seconds between pings
HEARTBEAT_TIMEOUT: float = 15.0  # seconds before considering client dead

# Reconnection
MAX_RECONNECT_ATTEMPTS: int = 10

# Security
PIN_LENGTH: int = 6

# Input
MOUSE_POLL_RATE: int = 125  # Hz
