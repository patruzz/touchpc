package com.touchpc.remotecontrol.transport

import kotlinx.coroutines.flow.StateFlow

interface Transport {
    val state: StateFlow<TransportState>
    suspend fun connect(host: String, port: Int)
    suspend fun disconnect()
    fun send(data: ByteArray)
    fun setOnMessageListener(listener: (ByteArray) -> Unit)
    fun setOnStateChangeListener(listener: (TransportState) -> Unit)
}
