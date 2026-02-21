"""Execute incoming TouchPC commands using pynput controllers."""

from __future__ import annotations

import logging
from typing import Dict, Optional

from pynput.keyboard import Controller as KbController, Key
from pynput.mouse import Button, Controller as MouseController

from ..protocol.commands import (
    Command,
    CustomShortcut,
    DragEnd,
    DragMove,
    DragStart,
    GesturePinch,
    GestureThreeFingerSwipe,
    KeyEvent,
    KeyText,
    MouseButton,
    MouseMoveRel,
    Scroll,
)
from ..protocol.constants import (
    BUTTON_ACTION_CLICK,
    BUTTON_ACTION_PRESS,
    BUTTON_ACTION_RELEASE,
    KEY_ACTION_PRESS,
    KEY_ACTION_RELEASE,
    MODIFIER_ALT,
    MODIFIER_CTRL,
    MODIFIER_META,
    MODIFIER_SHIFT,
    MOUSE_BUTTON_LEFT,
    MOUSE_BUTTON_MIDDLE,
    MOUSE_BUTTON_RIGHT,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    SWIPE_UP,
)
from .key_mapping import get_pynput_key
from .mouse_smoother import MouseSmoother

logger = logging.getLogger(__name__)

_BUTTON_MAP: Dict[int, Button] = {
    MOUSE_BUTTON_LEFT: Button.left,
    MOUSE_BUTTON_RIGHT: Button.right,
    MOUSE_BUTTON_MIDDLE: Button.middle,
}

_MODIFIER_KEYS: list[tuple[int, Key]] = [
    (MODIFIER_CTRL, Key.ctrl_l),
    (MODIFIER_SHIFT, Key.shift_l),
    (MODIFIER_ALT, Key.alt_l),
    (MODIFIER_META, Key.cmd),
]

# Predefined custom shortcuts
_SHORTCUT_MAP: Dict[str, list] = {
    "copy": [Key.ctrl_l, "c"],
    "paste": [Key.ctrl_l, "v"],
    "cut": [Key.ctrl_l, "x"],
    "undo": [Key.ctrl_l, "z"],
    "redo": [Key.ctrl_l, "y"],
    "select_all": [Key.ctrl_l, "a"],
    "save": [Key.ctrl_l, "s"],
    "find": [Key.ctrl_l, "f"],
    "tab_close": [Key.ctrl_l, "w"],
    "tab_new": [Key.ctrl_l, "t"],
    "alt_tab": [Key.alt_l, Key.tab],
    "alt_f4": [Key.alt_l, Key.f4],
    "win_d": [Key.cmd, "d"],
    "screenshot": [Key.print_screen],
}


class PynputExecutor:
    """Translates protocol commands into OS-level mouse/keyboard actions."""

    def __init__(self) -> None:
        self._mouse = MouseController()
        self._keyboard = KbController()
        self._smoother = MouseSmoother()
        self._dragging: bool = False

    # -- dispatch -------------------------------------------------------------

    def execute(self, command: Command) -> None:
        """Execute *command* by dispatching to the appropriate handler."""
        handler = _DISPATCH.get(type(command))
        if handler is not None:
            handler(self, command)
        else:
            logger.warning("No executor handler for %s", type(command).__name__)

    # -- mouse ----------------------------------------------------------------

    def _handle_mouse_move_rel(self, cmd: MouseMoveRel) -> None:
        steps = self._smoother.smooth(cmd.dx, cmd.dy)
        for dx, dy in steps:
            self._mouse.move(dx, dy)

    def _handle_mouse_button(self, cmd: MouseButton) -> None:
        btn: Optional[Button] = _BUTTON_MAP.get(cmd.button)
        if btn is None:
            logger.warning("Unknown mouse button: %d", cmd.button)
            return

        if cmd.action == BUTTON_ACTION_PRESS:
            self._mouse.press(btn)
        elif cmd.action == BUTTON_ACTION_RELEASE:
            self._mouse.release(btn)
        elif cmd.action == BUTTON_ACTION_CLICK:
            self._mouse.click(btn)
        else:
            logger.warning("Unknown button action: %d", cmd.action)

    def _handle_scroll(self, cmd: Scroll) -> None:
        self._mouse.scroll(cmd.dx, cmd.dy)

    # -- drag -----------------------------------------------------------------

    def _handle_drag_start(self, cmd: DragStart) -> None:
        # Move cursor to absolute position then press left button
        current_x, current_y = self._mouse.position
        self._mouse.move(cmd.x - int(current_x), cmd.y - int(current_y))
        self._mouse.press(Button.left)
        self._dragging = True

    def _handle_drag_move(self, cmd: DragMove) -> None:
        if self._dragging:
            steps = self._smoother.smooth(cmd.dx, cmd.dy)
            for dx, dy in steps:
                self._mouse.move(dx, dy)

    def _handle_drag_end(self, _cmd: DragEnd) -> None:
        if self._dragging:
            self._mouse.release(Button.left)
            self._dragging = False

    # -- keyboard -------------------------------------------------------------

    def _handle_key_event(self, cmd: KeyEvent) -> None:
        key = get_pynput_key(cmd.keycode)

        # Press modifier keys first
        held_modifiers: list[Key] = []
        for mask, mod_key in _MODIFIER_KEYS:
            if cmd.modifiers & mask:
                self._keyboard.press(mod_key)
                held_modifiers.append(mod_key)

        if cmd.action == KEY_ACTION_PRESS:
            self._keyboard.press(key)
        elif cmd.action == KEY_ACTION_RELEASE:
            self._keyboard.release(key)

        # Release modifiers in reverse order
        for mod_key in reversed(held_modifiers):
            self._keyboard.release(mod_key)

    def _handle_key_text(self, cmd: KeyText) -> None:
        self._keyboard.type(cmd.text)

    # -- gestures -------------------------------------------------------------

    def _handle_gesture_pinch(self, cmd: GesturePinch) -> None:
        """Pinch-to-zoom: mapped to Ctrl+scroll."""
        self._keyboard.press(Key.ctrl_l)
        # Positive scale -> zoom in (scroll up), negative -> zoom out (scroll down)
        scroll_amount = 1 if cmd.scale > 1.0 else -1
        self._mouse.scroll(0, scroll_amount)
        self._keyboard.release(Key.ctrl_l)

    def _handle_gesture_three_finger_swipe(self, cmd: GestureThreeFingerSwipe) -> None:
        """Three-finger swipe: mapped to OS-level gestures via key combos."""
        if cmd.direction == SWIPE_UP:
            # Task view / expose
            self._keyboard.press(Key.cmd)
            self._keyboard.press(Key.tab)
            self._keyboard.release(Key.tab)
            self._keyboard.release(Key.cmd)
        elif cmd.direction == SWIPE_DOWN:
            # Show desktop (Win+D)
            from pynput.keyboard import KeyCode as _KC

            d_key = _KC.from_char("d")
            self._keyboard.press(Key.cmd)
            self._keyboard.press(d_key)
            self._keyboard.release(d_key)
            self._keyboard.release(Key.cmd)
        elif cmd.direction == SWIPE_LEFT:
            # Switch virtual desktop left
            self._keyboard.press(Key.cmd)
            self._keyboard.press(Key.ctrl_l)
            self._keyboard.press(Key.left)
            self._keyboard.release(Key.left)
            self._keyboard.release(Key.ctrl_l)
            self._keyboard.release(Key.cmd)
        elif cmd.direction == SWIPE_RIGHT:
            # Switch virtual desktop right
            self._keyboard.press(Key.cmd)
            self._keyboard.press(Key.ctrl_l)
            self._keyboard.press(Key.right)
            self._keyboard.release(Key.right)
            self._keyboard.release(Key.ctrl_l)
            self._keyboard.release(Key.cmd)

    # -- custom shortcuts -----------------------------------------------------

    def _handle_custom_shortcut(self, cmd: CustomShortcut) -> None:
        keys = _SHORTCUT_MAP.get(cmd.shortcut_id)
        if keys is None:
            logger.warning("Unknown custom shortcut: %s", cmd.shortcut_id)
            return

        # Press all keys in order, then release in reverse
        pressed: list = []
        for k in keys:
            if isinstance(k, str):
                from pynput.keyboard import KeyCode as _KC

                k = _KC.from_char(k)
            self._keyboard.press(k)
            pressed.append(k)

        for k in reversed(pressed):
            self._keyboard.release(k)


# Dispatch table: command type -> bound method name
# Built after the class is complete.
_DISPATCH: Dict[type, object] = {
    MouseMoveRel: PynputExecutor._handle_mouse_move_rel,
    MouseButton: PynputExecutor._handle_mouse_button,
    Scroll: PynputExecutor._handle_scroll,
    DragStart: PynputExecutor._handle_drag_start,
    DragMove: PynputExecutor._handle_drag_move,
    DragEnd: PynputExecutor._handle_drag_end,
    KeyEvent: PynputExecutor._handle_key_event,
    KeyText: PynputExecutor._handle_key_text,
    GesturePinch: PynputExecutor._handle_gesture_pinch,
    GestureThreeFingerSwipe: PynputExecutor._handle_gesture_three_finger_swipe,
    CustomShortcut: PynputExecutor._handle_custom_shortcut,
}
