package com.touchpc.remotecontrol.transport

import com.touchpc.remotecontrol.protocol.Command
import com.touchpc.remotecontrol.protocol.CommandSerializer
import com.touchpc.remotecontrol.protocol.ProtocolConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketTransport @Inject constructor() : Transport {

    private val _state = MutableStateFlow<TransportState>(TransportState.Disconnected)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var messageListener: ((ByteArray) -> Unit)? = null
    private var stateChangeListener: ((TransportState) -> Unit)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var batchJob: Job? = null

    private val isConnecting = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)
    private val maxReconnectAttempts = 10
    private val initialReconnectDelayMs = 500L
    private val maxReconnectDelayMs = 30_000L

    private var currentHost: String = ""
    private var currentPort: Int = 0

    // Mouse move batching
    private val batchLock = Any()
    private var pendingDx: Int = 0
    private var pendingDy: Int = 0
    private var hasPendingMove = false
    private val batchIntervalMs = 8L // ~125Hz

    override suspend fun connect(host: String, port: Int) {
        if (isConnecting.getAndSet(true)) return

        currentHost = host
        currentPort = port
        reconnectAttempts.set(0)

        updateState(TransportState.Connecting("$host:$port"))

        performConnect(host, port)
    }

    private fun performConnect(host: String, port: Int) {
        client?.dispatcher?.executorService?.shutdown()

        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(0, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("ws://$host:$port")
            .build()

        webSocket = client?.newWebSocket(request, createWebSocketListener())
    }

    override suspend fun disconnect() {
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        batchJob?.cancel()

        webSocket?.close(1000, "User disconnected")
        webSocket = null

        client?.dispatcher?.executorService?.shutdown()
        client = null

        isConnecting.set(false)
        reconnectAttempts.set(0)

        updateState(TransportState.Disconnected)
    }

    override fun send(data: ByteArray) {
        webSocket?.send(data.toByteString(0, data.size))
    }

    fun sendCommand(command: Command) {
        when (command) {
            is Command.MouseMoveRel -> batchMouseMove(command.dx, command.dy)
            else -> {
                val data = CommandSerializer.serialize(command)
                send(data)
            }
        }
    }

    private fun batchMouseMove(dx: Int, dy: Int) {
        synchronized(batchLock) {
            pendingDx += dx
            pendingDy += dy
            hasPendingMove = true
        }

        if (batchJob == null || batchJob?.isActive != true) {
            batchJob = scope.launch {
                while (isActive && _state.value is TransportState.Connected) {
                    delay(batchIntervalMs)
                    flushMouseMoves()
                }
            }
        }
    }

    private fun flushMouseMoves() {
        val dx: Int
        val dy: Int
        synchronized(batchLock) {
            if (!hasPendingMove) return
            dx = pendingDx
            dy = pendingDy
            pendingDx = 0
            pendingDy = 0
            hasPendingMove = false
        }
        if (dx != 0 || dy != 0) {
            val data = CommandSerializer.serialize(Command.MouseMoveRel(dx, dy))
            send(data)
        }
    }

    override fun setOnMessageListener(listener: (ByteArray) -> Unit) {
        messageListener = listener
    }

    override fun setOnStateChangeListener(listener: (TransportState) -> Unit) {
        stateChangeListener = listener
    }

    private fun updateState(newState: TransportState) {
        _state.value = newState
        stateChangeListener?.invoke(newState)
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(ProtocolConstants.HEARTBEAT_INTERVAL_MS)
                val ping = Command.HeartbeatPing(System.currentTimeMillis())
                val data = CommandSerializer.serialize(ping)
                send(data)
            }
        }
    }

    private fun attemptReconnect() {
        val attempts = reconnectAttempts.incrementAndGet()
        if (attempts > maxReconnectAttempts) {
            updateState(TransportState.Error("Max reconnection attempts reached"))
            isConnecting.set(false)
            return
        }

        updateState(TransportState.Reconnecting)

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delay = calculateBackoff(attempts)
            delay(delay)
            performConnect(currentHost, currentPort)
        }
    }

    private fun calculateBackoff(attempt: Int): Long {
        val delay = initialReconnectDelayMs * (1L shl (attempt - 1).coerceAtMost(15))
        return delay.coerceAtMost(maxReconnectDelayMs)
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnecting.set(false)
                reconnectAttempts.set(0)
                updateState(TransportState.Connected)
                startHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val data = bytes.toByteArray()
                // Handle heartbeat pong internally
                val command = CommandSerializer.deserialize(data)
                if (command is Command.HeartbeatPing) {
                    // Server sent a ping, respond with pong
                    val pong = Command.HeartbeatPong(command.timestamp)
                    val pongData = CommandSerializer.serialize(pong)
                    send(pongData)
                    return
                }
                messageListener?.invoke(data)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Protocol uses binary mode, but handle text as fallback
                messageListener?.invoke(text.toByteArray(Charsets.UTF_8))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                heartbeatJob?.cancel()
                batchJob?.cancel()

                if (code == 1000) {
                    updateState(TransportState.Disconnected)
                    isConnecting.set(false)
                } else {
                    attemptReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                heartbeatJob?.cancel()
                batchJob?.cancel()

                val currentState = _state.value
                if (currentState is TransportState.Connected || currentState is TransportState.Reconnecting) {
                    attemptReconnect()
                } else {
                    updateState(TransportState.Error(t.message ?: "Connection failed"))
                    isConnecting.set(false)
                }
            }
        }
    }

    fun destroy() {
        scope.cancel()
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        batchJob?.cancel()
        webSocket?.cancel()
        client?.dispatcher?.executorService?.shutdown()
    }
}
