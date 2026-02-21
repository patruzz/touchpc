package com.touchpc.remotecontrol.protocol

object ProtocolConstants {

    // Protocol version
    const val PROTOCOL_VERSION = 1

    // Message IDs
    const val MSG_HEARTBEAT_PING: Byte = 0x01
    const val MSG_MOUSE_MOVE_REL: Byte = 0x10
    const val MSG_MOUSE_BUTTON: Byte = 0x20
    const val MSG_SCROLL: Byte = 0x30
    const val MSG_DRAG_START: Byte = 0x31
    const val MSG_DRAG_MOVE: Byte = 0x32
    const val MSG_DRAG_END: Byte = 0x33.toByte()
    const val MSG_KEY_EVENT: Byte = 0x40
    const val MSG_KEY_TEXT: Byte = 0x41
    const val MSG_GESTURE_PINCH: Byte = 0x50
    const val MSG_GESTURE_THREE_FINGER_SWIPE: Byte = 0x51
    const val MSG_CUSTOM_SHORTCUT: Byte = 0x60
    const val MSG_HANDSHAKE: Byte = 0xE0.toByte()
    const val MSG_HANDSHAKE_ACK: Byte = 0xE1.toByte()
    const val MSG_HEARTBEAT_PONG: Byte = 0xF0.toByte()
    const val MSG_HEARTBEAT_PING_SERVER: Byte = 0xF1.toByte()

    // Mouse buttons
    const val MOUSE_BUTTON_LEFT = 0
    const val MOUSE_BUTTON_RIGHT = 1
    const val MOUSE_BUTTON_MIDDLE = 2

    // Mouse/Key actions
    const val ACTION_PRESS = 0
    const val ACTION_RELEASE = 1
    const val ACTION_CLICK = 2

    // Key actions
    const val KEY_ACTION_PRESS = 0
    const val KEY_ACTION_RELEASE = 1

    // Modifier bitmasks
    const val MOD_CTRL = 0x01
    const val MOD_SHIFT = 0x02
    const val MOD_ALT = 0x04
    const val MOD_META = 0x08

    // Swipe directions
    const val SWIPE_UP = 0
    const val SWIPE_DOWN = 1
    const val SWIPE_LEFT = 2
    const val SWIPE_RIGHT = 3

    // Heartbeat interval in milliseconds
    const val HEARTBEAT_INTERVAL_MS = 5000L

    // Default port
    const val DEFAULT_PORT = 9876

    // PIN hash size
    const val PIN_HASH_SIZE = 32

    // Nonce size
    const val NONCE_SIZE = 16
}
