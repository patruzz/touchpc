"""Mapping from USB HID keycodes used by the TouchPC protocol to pynput key objects."""

from __future__ import annotations

from typing import Union

from pynput.keyboard import Key, KeyCode

# ---------------------------------------------------------------------------
# USB HID keycode -> pynput Key / KeyCode
# ---------------------------------------------------------------------------

# Letters a-z  (USB HID 0x04 - 0x1D)
_LETTER_MAP: dict[int, KeyCode] = {
    0x04 + i: KeyCode.from_char(chr(ord("a") + i)) for i in range(26)
}

# Digits 0-9  (USB HID 0x1E = '1' ... 0x26 = '9', 0x27 = '0')
_DIGIT_MAP: dict[int, KeyCode] = {
    0x1E + i: KeyCode.from_char(chr(ord("1") + i)) for i in range(9)
}
_DIGIT_MAP[0x27] = KeyCode.from_char("0")

# Function keys F1-F12  (USB HID 0x3A - 0x45)
_FKEY_MAP: dict[int, Key] = {
    0x3A: Key.f1,
    0x3B: Key.f2,
    0x3C: Key.f3,
    0x3D: Key.f4,
    0x3E: Key.f5,
    0x3F: Key.f6,
    0x40: Key.f7,
    0x41: Key.f8,
    0x42: Key.f9,
    0x43: Key.f10,
    0x44: Key.f11,
    0x45: Key.f12,
}

# Special / navigation keys
_SPECIAL_MAP: dict[int, Key] = {
    0x28: Key.enter,
    0x29: Key.esc,
    0x2A: Key.backspace,
    0x2B: Key.tab,
    0x2C: Key.space,
    0x4A: Key.home,
    0x4B: Key.page_up,
    0x4C: Key.delete,
    0x4D: Key.end,
    0x4E: Key.page_down,
    0x4F: Key.right,
    0x50: Key.left,
    0x51: Key.down,
    0x52: Key.up,
}

# Combined lookup table
KEYCODE_MAP: dict[int, Union[Key, KeyCode]] = {
    **_LETTER_MAP,
    **_DIGIT_MAP,
    **_FKEY_MAP,
    **_SPECIAL_MAP,
}


def get_pynput_key(keycode: int) -> Union[Key, KeyCode]:
    """Resolve a protocol keycode to a pynput ``Key`` or ``KeyCode``.

    If the keycode is not in the map a ``KeyCode`` with the raw virtual-key
    value is returned so that the caller can still attempt to use it.
    """
    mapped = KEYCODE_MAP.get(keycode)
    if mapped is not None:
        return mapped
    # Fallback: treat the keycode as a virtual-key code
    return KeyCode.from_vk(keycode)
