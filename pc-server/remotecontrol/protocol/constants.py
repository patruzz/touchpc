"""Binary protocol message IDs and constants for TouchPC communication."""

# --- Message IDs ---

# Mouse
MSG_MOUSE_MOVE_REL: int = 0x01
MSG_MOUSE_BUTTON: int = 0x10
MSG_SCROLL: int = 0x20

# Drag
MSG_DRAG_START: int = 0x30
MSG_DRAG_MOVE: int = 0x31
MSG_DRAG_END: int = 0x32

# Keyboard
MSG_KEY_EVENT: int = 0x40
MSG_KEY_TEXT: int = 0x41

# Gestures
MSG_GESTURE_PINCH: int = 0x50
MSG_GESTURE_THREE_FINGER_SWIPE: int = 0x51

# Custom shortcuts
MSG_CUSTOM_SHORTCUT: int = 0x60

# Heartbeat
MSG_HEARTBEAT_PING: int = 0xE0
MSG_HEARTBEAT_PONG: int = 0xE1

# Handshake
MSG_HANDSHAKE: int = 0xF0
MSG_HANDSHAKE_ACK: int = 0xF1

# --- Mouse Buttons ---
MOUSE_BUTTON_LEFT: int = 0
MOUSE_BUTTON_RIGHT: int = 1
MOUSE_BUTTON_MIDDLE: int = 2

# --- Button Actions ---
BUTTON_ACTION_PRESS: int = 0
BUTTON_ACTION_RELEASE: int = 1
BUTTON_ACTION_CLICK: int = 2

# --- Key Actions ---
KEY_ACTION_PRESS: int = 0
KEY_ACTION_RELEASE: int = 1

# --- Modifier Bitmasks ---
MODIFIER_CTRL: int = 0x01
MODIFIER_SHIFT: int = 0x02
MODIFIER_ALT: int = 0x04
MODIFIER_META: int = 0x08

# --- Three-Finger Swipe Directions ---
SWIPE_UP: int = 0
SWIPE_DOWN: int = 1
SWIPE_LEFT: int = 2
SWIPE_RIGHT: int = 3
