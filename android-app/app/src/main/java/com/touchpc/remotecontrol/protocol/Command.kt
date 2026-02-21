package com.touchpc.remotecontrol.protocol

sealed class Command {
    data class MouseMoveRel(val dx: Int, val dy: Int) : Command()
    data class MouseButton(val button: Int, val action: Int) : Command()
    data class Scroll(val dx: Int, val dy: Int) : Command()
    data class DragStart(val x: Int, val y: Int) : Command()
    data class DragMove(val dx: Int, val dy: Int) : Command()
    data object DragEnd : Command()
    data class KeyEvent(val keycode: Int, val action: Int, val modifiers: Int) : Command()
    data class KeyText(val text: String) : Command()
    data class GesturePinch(val scale: Float, val centerX: Int, val centerY: Int) : Command()
    data class GestureThreeFingerSwipe(val direction: Int) : Command()
    data class CustomShortcut(val shortcutId: String) : Command()
    data class HeartbeatPing(val timestamp: Long) : Command()
    data class HeartbeatPong(val timestamp: Long) : Command()
    data class Handshake(val version: Int, val pinHash: ByteArray, val nonce: ByteArray) : Command() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Handshake) return false
            return version == other.version &&
                    pinHash.contentEquals(other.pinHash) &&
                    nonce.contentEquals(other.nonce)
        }

        override fun hashCode(): Int {
            var result = version
            result = 31 * result + pinHash.contentHashCode()
            result = 31 * result + nonce.contentHashCode()
            return result
        }
    }

    data class HandshakeAck(
        val success: Boolean,
        val screenWidth: Int,
        val screenHeight: Int,
        val serverName: String
    ) : Command()
}
