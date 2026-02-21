package com.touchpc.remotecontrol.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.touchpc.remotecontrol.R
import com.touchpc.remotecontrol.app.MainActivity
import com.touchpc.remotecontrol.transport.TransportState
import com.touchpc.remotecontrol.transport.WebSocketTransport
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class ConnectionService : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "touchpc_connection"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.touchpc.remotecontrol.STOP_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, ConnectionService::class.java)
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ConnectionService::class.java)
            context.stopService(intent)
        }
    }

    @Inject
    lateinit var transport: WebSocketTransport

    private val binder = ConnectionBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateObserverJob: Job? = null
    private var currentAddress: String = ""

    inner class ConnectionBinder : Binder() {
        fun getTransport(): WebSocketTransport = transport
        fun getService(): ConnectionService = this@ConnectionService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(TransportState.Disconnected))
        observeTransportState()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch {
                transport.disconnect()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stateObserverJob?.cancel()
        scope.cancel()
        transport.destroy()
        super.onDestroy()
    }

    fun connect(host: String, port: Int) {
        currentAddress = "$host:$port"
        scope.launch {
            transport.connect(host, port)
        }
    }

    fun disconnect() {
        scope.launch {
            transport.disconnect()
            currentAddress = ""
        }
    }

    private fun observeTransportState() {
        stateObserverJob = scope.launch {
            transport.state.collectLatest { state ->
                updateNotification(state)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(state: TransportState): Notification {
        val contentText = when (state) {
            is TransportState.Connected -> getString(R.string.notification_connected, currentAddress)
            is TransportState.Connecting -> getString(R.string.notification_connecting, state.address)
            is TransportState.Reconnecting -> getString(R.string.reconnecting)
            is TransportState.Error -> getString(R.string.connection_error) + ": ${state.message}"
            is TransportState.Disconnected -> getString(R.string.notification_disconnected)
        }

        val contentIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val stopIntent = Intent(this, ConnectionService::class.java).apply {
            action = ACTION_STOP
        }.let {
            PendingIntent.getService(
                this, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (state is TransportState.Connected) {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.disconnect),
                stopIntent
            )
        }

        return builder.build()
    }

    private fun updateNotification(state: TransportState) {
        val notification = createNotification(state)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
