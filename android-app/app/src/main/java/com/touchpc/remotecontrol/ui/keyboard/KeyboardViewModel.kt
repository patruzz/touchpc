package com.touchpc.remotecontrol.ui.keyboard

import androidx.lifecycle.ViewModel
import com.touchpc.remotecontrol.protocol.Command
import com.touchpc.remotecontrol.transport.WebSocketTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class KeyboardViewModel @Inject constructor(private val transport: WebSocketTransport) : ViewModel() {
    fun sendKeyCommand(command: Command) { transport.sendCommand(command) }
    fun sendText(text: String) { if (text.isNotEmpty()) transport.sendCommand(Command.KeyText(text)) }
}
