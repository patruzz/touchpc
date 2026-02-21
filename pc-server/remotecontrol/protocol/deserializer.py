"""Binary serialization / deserialization for the TouchPC protocol.

Wire format
-----------
Byte 0 : message ID  (uint8)
Byte 1 : payload length (uint8, 0-255)
Byte 2+: payload
"""

from __future__ import annotations

import struct
from typing import Union

from .constants import (
    MSG_CUSTOM_SHORTCUT,
    MSG_DRAG_END,
    MSG_DRAG_MOVE,
    MSG_DRAG_START,
    MSG_GESTURE_PINCH,
    MSG_GESTURE_THREE_FINGER_SWIPE,
    MSG_HANDSHAKE,
    MSG_HANDSHAKE_ACK,
    MSG_HEARTBEAT_PING,
    MSG_HEARTBEAT_PONG,
    MSG_KEY_EVENT,
    MSG_KEY_TEXT,
    MSG_MOUSE_BUTTON,
    MSG_MOUSE_MOVE_REL,
    MSG_SCROLL,
)
from .commands import (
    Command,
    CustomShortcut,
    DragEnd,
    DragMove,
    DragStart,
    GesturePinch,
    GestureThreeFingerSwipe,
    Handshake,
    HandshakeAck,
    HeartbeatPing,
    HeartbeatPong,
    KeyEvent,
    KeyText,
    MouseButton,
    MouseMoveRel,
    Scroll,
)


class ProtocolError(Exception):
    """Raised when a binary message cannot be parsed."""


# ---------------------------------------------------------------------------
# Deserialization (bytes -> Command)
# ---------------------------------------------------------------------------

def deserialize(data: bytes) -> Command:
    """Parse a binary message into the corresponding Command dataclass.

    Parameters
    ----------
    data:
        Raw bytes received on the transport layer.  Must be at least 2 bytes
        (message-id + payload-length).

    Returns
    -------
    Command
        A concrete Command subclass instance.

    Raises
    ------
    ProtocolError
        If the message is malformed or uses an unknown message ID.
    """
    if len(data) < 2:
        raise ProtocolError(f"Message too short ({len(data)} bytes)")

    msg_id: int = data[0]
    payload_len: int = data[1]
    payload: bytes = data[2:]

    if len(payload) < payload_len:
        raise ProtocolError(
            f"Payload underrun: expected {payload_len} bytes, got {len(payload)}"
        )

    # Trim to declared length (ignore any trailing bytes)
    payload = payload[:payload_len]

    return _PARSERS[msg_id](payload) if msg_id in _PARSERS else _unknown(msg_id)


def _unknown(msg_id: int) -> Command:
    raise ProtocolError(f"Unknown message ID: 0x{msg_id:02X}")


# ---- individual parsers --------------------------------------------------

def _parse_mouse_move_rel(p: bytes) -> MouseMoveRel:
    _check_len(p, 4, "MOUSE_MOVE_REL")
    dx, dy = struct.unpack("!hh", p[:4])
    return MouseMoveRel(dx=dx, dy=dy)


def _parse_mouse_button(p: bytes) -> MouseButton:
    _check_len(p, 2, "MOUSE_BUTTON")
    return MouseButton(button=p[0], action=p[1])


def _parse_scroll(p: bytes) -> Scroll:
    _check_len(p, 4, "SCROLL")
    dx, dy = struct.unpack("!hh", p[:4])
    return Scroll(dx=dx, dy=dy)


def _parse_drag_start(p: bytes) -> DragStart:
    _check_len(p, 4, "DRAG_START")
    x, y = struct.unpack("!hh", p[:4])
    return DragStart(x=x, y=y)


def _parse_drag_move(p: bytes) -> DragMove:
    _check_len(p, 4, "DRAG_MOVE")
    dx, dy = struct.unpack("!hh", p[:4])
    return DragMove(dx=dx, dy=dy)


def _parse_drag_end(p: bytes) -> DragEnd:
    return DragEnd()


def _parse_key_event(p: bytes) -> KeyEvent:
    _check_len(p, 4, "KEY_EVENT")
    keycode, action, modifiers = struct.unpack("!HBB", p[:4])
    return KeyEvent(keycode=keycode, action=action, modifiers=modifiers)


def _parse_key_text(p: bytes) -> KeyText:
    text = p.decode("utf-8")
    return KeyText(text=text)


def _parse_gesture_pinch(p: bytes) -> GesturePinch:
    _check_len(p, 8, "GESTURE_PINCH")
    scale, cx, cy = struct.unpack("!fhh", p[:8])
    return GesturePinch(scale=scale, center_x=cx, center_y=cy)


def _parse_gesture_three_finger_swipe(p: bytes) -> GestureThreeFingerSwipe:
    _check_len(p, 1, "GESTURE_THREE_FINGER_SWIPE")
    return GestureThreeFingerSwipe(direction=p[0])


def _parse_custom_shortcut(p: bytes) -> CustomShortcut:
    shortcut_id = p.decode("utf-8")
    return CustomShortcut(shortcut_id=shortcut_id)


def _parse_heartbeat_ping(p: bytes) -> HeartbeatPing:
    _check_len(p, 8, "HEARTBEAT_PING")
    (timestamp,) = struct.unpack("!q", p[:8])
    return HeartbeatPing(timestamp=timestamp)


def _parse_heartbeat_pong(p: bytes) -> HeartbeatPong:
    _check_len(p, 8, "HEARTBEAT_PONG")
    (timestamp,) = struct.unpack("!q", p[:8])
    return HeartbeatPong(timestamp=timestamp)


def _parse_handshake(p: bytes) -> Handshake:
    _check_len(p, 49, "HANDSHAKE")  # 1 + 32 + 16
    version = p[0]
    pin_hash = p[1:33]
    nonce = p[33:49]
    return Handshake(version=version, pin_hash=pin_hash, nonce=nonce)


def _parse_handshake_ack(p: bytes) -> HandshakeAck:
    if len(p) < 5:
        raise ProtocolError("HANDSHAKE_ACK payload too short")
    success = bool(p[0])
    screen_width, screen_height = struct.unpack("!HH", p[1:5])
    server_name = p[5:].decode("utf-8")
    return HandshakeAck(
        success=success,
        screen_width=screen_width,
        screen_height=screen_height,
        server_name=server_name,
    )


# ---- helpers -------------------------------------------------------------

def _check_len(payload: bytes, expected: int, label: str) -> None:
    if len(payload) < expected:
        raise ProtocolError(
            f"{label} payload too short: expected {expected}, got {len(payload)}"
        )


# ---- parser dispatch table -----------------------------------------------

_PARSERS: dict[int, object] = {
    MSG_MOUSE_MOVE_REL: _parse_mouse_move_rel,
    MSG_MOUSE_BUTTON: _parse_mouse_button,
    MSG_SCROLL: _parse_scroll,
    MSG_DRAG_START: _parse_drag_start,
    MSG_DRAG_MOVE: _parse_drag_move,
    MSG_DRAG_END: _parse_drag_end,
    MSG_KEY_EVENT: _parse_key_event,
    MSG_KEY_TEXT: _parse_key_text,
    MSG_GESTURE_PINCH: _parse_gesture_pinch,
    MSG_GESTURE_THREE_FINGER_SWIPE: _parse_gesture_three_finger_swipe,
    MSG_CUSTOM_SHORTCUT: _parse_custom_shortcut,
    MSG_HEARTBEAT_PING: _parse_heartbeat_ping,
    MSG_HEARTBEAT_PONG: _parse_heartbeat_pong,
    MSG_HANDSHAKE: _parse_handshake,
    MSG_HANDSHAKE_ACK: _parse_handshake_ack,
}


# ---------------------------------------------------------------------------
# Serialization (Command -> bytes)  -- used for server responses
# ---------------------------------------------------------------------------

def serialize(command: Command) -> bytes:
    """Serialize a Command into the binary wire format.

    Currently only server-originated messages need serialization:
    HandshakeAck and HeartbeatPong.

    Returns
    -------
    bytes
        The full message (header + payload).
    """
    if isinstance(command, HandshakeAck):
        return _serialize_handshake_ack(command)
    if isinstance(command, HeartbeatPong):
        return _serialize_heartbeat_pong(command)
    raise ProtocolError(f"Cannot serialize command type: {type(command).__name__}")


def _serialize_handshake_ack(cmd: HandshakeAck) -> bytes:
    name_bytes = cmd.server_name.encode("utf-8")
    payload = (
        struct.pack("!B", int(cmd.success))
        + struct.pack("!HH", cmd.screen_width, cmd.screen_height)
        + name_bytes
    )
    return bytes([MSG_HANDSHAKE_ACK, len(payload)]) + payload


def _serialize_heartbeat_pong(cmd: HeartbeatPong) -> bytes:
    payload = struct.pack("!q", cmd.timestamp)
    return bytes([MSG_HEARTBEAT_PONG, len(payload)]) + payload
