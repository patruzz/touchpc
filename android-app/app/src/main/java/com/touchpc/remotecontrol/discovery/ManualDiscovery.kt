package com.touchpc.remotecontrol.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManualDiscovery @Inject constructor() {

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 3000
        private val IP_PATTERN = Regex(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        )
        private val HOSTNAME_PATTERN = Regex(
            "^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*$"
        )
    }

    fun validateHost(host: String): Boolean {
        val trimmed = host.trim()
        return IP_PATTERN.matches(trimmed) || HOSTNAME_PATTERN.matches(trimmed)
    }

    fun validatePort(port: Int): Boolean {
        return port in 1..65535
    }

    fun validateInput(host: String, port: Int): ValidationResult {
        if (host.isBlank()) {
            return ValidationResult.Error("Host address cannot be empty")
        }
        if (!validateHost(host)) {
            return ValidationResult.Error("Invalid IP address or hostname")
        }
        if (!validatePort(port)) {
            return ValidationResult.Error("Port must be between 1 and 65535")
        }
        return ValidationResult.Valid
    }

    suspend fun tryConnect(host: String, port: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS)
                    true
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
