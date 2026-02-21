package com.touchpc.remotecontrol.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

object CommandSerializer {

    fun serialize(command: Command): ByteArray {
        return when (command) {
            is Command.MouseMoveRel -> {
                val payload = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
                    .putShort(command.dx.toShort())
                    .putShort(command.dy.toShort())
                    .array()
                buildPacket(ProtocolConstants.MSG_MOUSE_MOVE_REL, payload)
            }

            is Command.MouseButton -> {
                val payload = byteArrayOf(command.button.toByte(), command.action.toByte())
                buildPacket(ProtocolConstants.MSG_MOUSE_BUTTON, payload)
            }

            is Command.Scroll -> {
                val payload = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
                    .putShort(command.dx.toShort())
                    .putShort(command.dy.toShort())
                    .array()
                buildPacket(ProtocolConstants.MSG_SCROLL, payload)
            }

            is Command.DragStart -> {
                val payload = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
                    .putShort(command.x.toShort())
                    .putShort(command.y.toShort())
                    .array()
                buildPacket(ProtocolConstants.MSG_DRAG_START, payload)
            }

            is Command.DragMove -> {
                val payload = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
                    .putShort(command.dx.toShort())
                    .putShort(command.dy.toShort())
                    .array()
                buildPacket(ProtocolConstants.MSG_DRAG_MOVE, payload)
            }

            is Command.DragEnd -> {
                buildPacket(ProtocolConstants.MSG_DRAG_END, byteArrayOf())
            }

            is Command.KeyEvent -> {
                val payload = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
                    .putShort(command.keycode.toShort())
                    .put(command.action.toByte())
                    .put(command.modifiers.toByte())
                    .array()
                buildPacket(ProtocolConstants.MSG_KEY_EVENT, payload)
            }

            is Command.KeyText -> {
                val payload = command.text.toByteArray(Charsets.UTF_8)
                buildPacket(ProtocolConstants.MSG_KEY_TEXT, payload)
            }

            is Command.GesturePinch -> {
                val payload = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
                    .putFloat(command.scale)
                    .putShort(command.centerX.toShort())
                    .putShort(command.centerY.toShort())
                    .array()
                buildPacket(ProtocolConstants.MSG_GESTURE_PINCH, payload)
            }

            is Command.GestureThreeFingerSwipe -> {
                val payload = byteArrayOf(command.direction.toByte())
                buildPacket(ProtocolConstants.MSG_GESTURE_THREE_FINGER_SWIPE, payload)
            }

            is Command.CustomShortcut -> {
                val payload = command.shortcutId.toByteArray(Charsets.UTF_8)
                buildPacket(ProtocolConstants.MSG_CUSTOM_SHORTCUT, payload)
            }

            is Command.HeartbeatPing -> {
                val payload = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
                    .putLong(command.timestamp)
                    .array()
                buildPacket(ProtocolConstants.MSG_HEARTBEAT_PING, payload)
            }

            is Command.HeartbeatPong -> {
                val payload = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
                    .putLong(command.timestamp)
                    .array()
                buildPacket(ProtocolConstants.MSG_HEARTBEAT_PONG, payload)
            }

            is Command.Handshake -> {
                val payload = ByteBuffer.allocate(1 + ProtocolConstants.PIN_HASH_SIZE + ProtocolConstants.NONCE_SIZE)
                    .order(ByteOrder.BIG_ENDIAN)
                    .put(command.version.toByte())
                    .put(command.pinHash)
                    .put(command.nonce)
                    .array()
                buildPacket(ProtocolConstants.MSG_HANDSHAKE, payload)
            }

            is Command.HandshakeAck -> {
                // HandshakeAck is only received, not sent, but serialize for completeness
                val nameBytes = command.serverName.toByteArray(Charsets.UTF_8)
                val payload = ByteBuffer.allocate(1 + 4 + 4 + nameBytes.size)
                    .order(ByteOrder.BIG_ENDIAN)
                    .put(if (command.success) 1.toByte() else 0.toByte())
                    .putShort(command.screenWidth.toShort())
                    .putShort(command.screenHeight.toShort())
                    .put(nameBytes)
                    .array()
                buildPacket(ProtocolConstants.MSG_HANDSHAKE_ACK, payload)
            }
        }
    }

    fun deserialize(data: ByteArray): Command? {
        if (data.size < 2) return null

        val msgId = data[0]
        val payloadLength = data[1].toInt() and 0xFF
        val payload = if (payloadLength > 0 && data.size >= 2 + payloadLength) {
            data.copyOfRange(2, 2 + payloadLength)
        } else {
            byteArrayOf()
        }

        return when (msgId) {
            ProtocolConstants.MSG_HANDSHAKE_ACK -> deserializeHandshakeAck(payload)
            ProtocolConstants.MSG_HEARTBEAT_PONG -> deserializeHeartbeatPong(payload)
            ProtocolConstants.MSG_HEARTBEAT_PING_SERVER -> deserializeHeartbeatPingServer(payload)
            else -> null
        }
    }

    fun deserializeAck(data: ByteArray): Command? {
        return deserialize(data)
    }

    private fun deserializeHandshakeAck(payload: ByteArray): Command.HandshakeAck? {
        if (payload.size < 5) return null
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN)
        val success = buffer.get().toInt() != 0
        val screenWidth = buffer.short.toInt() and 0xFFFF
        val screenHeight = buffer.short.toInt() and 0xFFFF
        val nameBytes = ByteArray(payload.size - 5)
        if (nameBytes.isNotEmpty()) {
            buffer.get(nameBytes)
        }
        val serverName = String(nameBytes, Charsets.UTF_8)
        return Command.HandshakeAck(success, screenWidth, screenHeight, serverName)
    }

    private fun deserializeHeartbeatPong(payload: ByteArray): Command.HeartbeatPong? {
        if (payload.size < 8) return null
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN)
        val timestamp = buffer.long
        return Command.HeartbeatPong(timestamp)
    }

    private fun deserializeHeartbeatPingServer(payload: ByteArray): Command.HeartbeatPing? {
        if (payload.size < 8) return null
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN)
        val timestamp = buffer.long
        return Command.HeartbeatPing(timestamp)
    }

    private fun buildPacket(msgId: Byte, payload: ByteArray): ByteArray {
        val packet = ByteArray(2 + payload.size)
        packet[0] = msgId
        packet[1] = payload.size.toByte()
        payload.copyInto(packet, 2)
        return packet
    }
}
