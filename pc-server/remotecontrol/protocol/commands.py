"""Dataclass definitions for all TouchPC protocol commands."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Union


# ---------- Base ----------

@dataclass(slots=True)
class Command:
    """Base class for all protocol commands."""


# ---------- Mouse ----------

@dataclass(slots=True)
class MouseMoveRel(Command):
    dx: int
    dy: int


@dataclass(slots=True)
class MouseButton(Command):
    button: int   # 0=LEFT, 1=RIGHT, 2=MIDDLE
    action: int   # 0=PRESS, 1=RELEASE, 2=CLICK


@dataclass(slots=True)
class Scroll(Command):
    dx: int
    dy: int


# ---------- Drag ----------

@dataclass(slots=True)
class DragStart(Command):
    x: int
    y: int


@dataclass(slots=True)
class DragMove(Command):
    dx: int
    dy: int


@dataclass(slots=True)
class DragEnd(Command):
    pass


# ---------- Keyboard ----------

@dataclass(slots=True)
class KeyEvent(Command):
    keycode: int
    action: int       # 0=PRESS, 1=RELEASE
    modifiers: int    # bitmask: CTRL=0x01, SHIFT=0x02, ALT=0x04, META=0x08


@dataclass(slots=True)
class KeyText(Command):
    text: str


# ---------- Gestures ----------

@dataclass(slots=True)
class GesturePinch(Command):
    scale: float
    center_x: int
    center_y: int


@dataclass(slots=True)
class GestureThreeFingerSwipe(Command):
    direction: int   # 0=up, 1=down, 2=left, 3=right


# ---------- Custom Shortcuts ----------

@dataclass(slots=True)
class CustomShortcut(Command):
    shortcut_id: str


# ---------- Heartbeat ----------

@dataclass(slots=True)
class HeartbeatPing(Command):
    timestamp: int


@dataclass(slots=True)
class HeartbeatPong(Command):
    timestamp: int


# ---------- Handshake ----------

@dataclass(slots=True)
class Handshake(Command):
    version: int
    pin_hash: bytes
    nonce: bytes


@dataclass(slots=True)
class HandshakeAck(Command):
    success: bool
    screen_width: int
    screen_height: int
    server_name: str


# Convenience type alias
AnyCommand = Union[
    MouseMoveRel, MouseButton, Scroll,
    DragStart, DragMove, DragEnd,
    KeyEvent, KeyText,
    GesturePinch, GestureThreeFingerSwipe,
    CustomShortcut,
    HeartbeatPing, HeartbeatPong,
    Handshake, HandshakeAck,
]
