package com.touchpc.remotecontrol.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MdnsDiscovery @Inject constructor() {

    companion object {
        private const val TAG = "MdnsDiscovery"
        private const val SERVICE_TYPE = "_touchpc._tcp."
    }

    private val _servers = MutableStateFlow<List<ServerInfo>>(emptyList())
    val servers: StateFlow<List<ServerInfo>> = _servers.asStateFlow()

    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isDiscovering = false

    fun startDiscovery(context: Context) {
        if (isDiscovering) return

        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        _servers.value = emptyList()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: $errorCode")
                isDiscovering = false
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(TAG, "Discovery started for $serviceType")
                isDiscovering = true
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(TAG, "Discovery stopped")
                isDiscovering = false
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service found: ${serviceInfo?.serviceName}")
                serviceInfo?.let { resolveService(it) }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service lost: ${serviceInfo?.serviceName}")
                serviceInfo?.let { lostInfo ->
                    val currentList = _servers.value.toMutableList()
                    currentList.removeAll { it.name == lostInfo.serviceName }
                    _servers.value = currentList
                }
            }
        }

        try {
            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery", e)
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${serviceInfo?.serviceName}: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let { resolved ->
                    val host = resolved.host?.hostAddress ?: return
                    val port = resolved.port
                    val name = resolved.serviceName

                    Log.d(TAG, "Resolved: $name at $host:$port")

                    val server = ServerInfo(name, host, port)
                    val currentList = _servers.value.toMutableList()

                    // Replace if same name exists, otherwise add
                    val existingIndex = currentList.indexOfFirst { it.name == name }
                    if (existingIndex >= 0) {
                        currentList[existingIndex] = server
                    } else {
                        currentList.add(server)
                    }

                    _servers.value = currentList
                }
            }
        })
    }

    fun stopDiscovery() {
        if (!isDiscovering) return

        try {
            discoveryListener?.let { listener ->
                nsdManager?.stopServiceDiscovery(listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop discovery", e)
        }

        isDiscovering = false
        discoveryListener = null
    }

    fun discoverServers(context: Context): Flow<List<ServerInfo>> = callbackFlow {
        startDiscovery(context)

        val job = launch {
            _servers.collect { servers ->
                trySend(servers)
            }
        }

        awaitClose {
            job.cancel()
            stopDiscovery()
        }
    }
}
