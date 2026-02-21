package com.touchpc.remotecontrol.transport

sealed class TransportState {
    data object Disconnected : TransportState()
    data class Connecting(val address: String) : TransportState()
    data object Connected : TransportState()
    data class Error(val message: String) : TransportState()
    data object Reconnecting : TransportState()
}
