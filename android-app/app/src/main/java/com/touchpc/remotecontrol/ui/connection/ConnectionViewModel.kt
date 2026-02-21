package com.touchpc.remotecontrol.ui.connection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.touchpc.remotecontrol.data.PreferencesManager
import com.touchpc.remotecontrol.data.ServerHistoryEntry
import com.touchpc.remotecontrol.discovery.ManualDiscovery
import com.touchpc.remotecontrol.discovery.MdnsDiscovery
import com.touchpc.remotecontrol.discovery.ServerInfo
import com.touchpc.remotecontrol.transport.TransportState
import com.touchpc.remotecontrol.transport.WebSocketTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    application: Application,
    private val transport: WebSocketTransport,
    private val mdnsDiscovery: MdnsDiscovery,
    private val manualDiscovery: ManualDiscovery,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

    val transportState: StateFlow<TransportState> = transport.state
    val discoveredServers: StateFlow<List<ServerInfo>> = mdnsDiscovery.servers
    val serverHistory: StateFlow<List<ServerHistoryEntry>> = preferencesManager.serverHistory
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    fun startDiscovery() { mdnsDiscovery.startDiscovery(getApplication()) }
    fun stopDiscovery() { mdnsDiscovery.stopDiscovery() }

    fun connect(host: String, portStr: String) {
        val port = portStr.toIntOrNull() ?: 9876
        val validation = manualDiscovery.validateInput(host, port)
        when (validation) {
            is ManualDiscovery.ValidationResult.Error -> { _validationError.value = validation.message }
            is ManualDiscovery.ValidationResult.Valid -> {
                _validationError.value = null
                viewModelScope.launch {
                    preferencesManager.addServerToHistory(host, host, port)
                    transport.connect(host, port)
                }
            }
        }
    }

    fun connectToServer(server: ServerInfo) {
        viewModelScope.launch {
            preferencesManager.addServerToHistory(server.name, server.host, server.port)
            transport.connect(server.host, server.port)
        }
    }

    fun disconnect() { viewModelScope.launch { transport.disconnect() } }

    fun sendHandshake(pin: String) {
        viewModelScope.launch {
            val nonce = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
            val pinBytes = pin.toByteArray(Charsets.UTF_8)
            val hashInput = pinBytes + nonce
            val hash = java.security.MessageDigest.getInstance("SHA-256").digest(hashInput)
            val command = com.touchpc.remotecontrol.protocol.Command.Handshake(
                com.touchpc.remotecontrol.protocol.ProtocolConstants.PROTOCOL_VERSION, hash, nonce
            )
            transport.sendCommand(command)
        }
    }

    override fun onCleared() { super.onCleared(); stopDiscovery() }
}
