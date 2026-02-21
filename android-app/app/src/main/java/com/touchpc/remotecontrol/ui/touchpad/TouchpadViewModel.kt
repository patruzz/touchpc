package com.touchpc.remotecontrol.ui.touchpad

import androidx.lifecycle.ViewModel
import com.touchpc.remotecontrol.protocol.Command
import com.touchpc.remotecontrol.protocol.ProtocolConstants
import com.touchpc.remotecontrol.transport.TransportState
import com.touchpc.remotecontrol.transport.WebSocketTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TouchpadViewModel @Inject constructor(
    private val transport: WebSocketTransport
) : ViewModel() {
    val transportState: StateFlow<TransportState> = transport.state
    fun sendCommands(commands: List<Command>) { commands.forEach { transport.sendCommand(it) } }
    fun leftClick() = transport.sendCommand(Command.MouseButton(ProtocolConstants.MOUSE_BUTTON_LEFT, ProtocolConstants.ACTION_CLICK))
    fun rightClick() = transport.sendCommand(Command.MouseButton(ProtocolConstants.MOUSE_BUTTON_RIGHT, ProtocolConstants.ACTION_CLICK))
    fun middleClick() = transport.sendCommand(Command.MouseButton(ProtocolConstants.MOUSE_BUTTON_MIDDLE, ProtocolConstants.ACTION_CLICK))
}
