# TouchPC Android App - Completar Build y Preparar Deploy a Play Store

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Completar la capa UI de la app Android TouchPC (Activities, Fragments, ViewModels, Custom Views, DI) e implementar la configuracion necesaria para publicar en Google Play Store.

**Architecture:** La app usa MVVM con Navigation Component (single Activity + 5 Fragments). El backend (protocol, transport, gesture, discovery, service) ya esta 100% implementado. Solo falta la capa de presentacion (UI) y la configuracion de Hilt DI que conecta todo. Se usa DataStore para persistencia de preferencias.

**Tech Stack:** Kotlin, AndroidX, Material Design 3, Hilt, Navigation Component, ViewBinding, Coroutines/StateFlow, DataStore Preferences, OkHttp WebSocket

---

## Estado Actual

### Completado (NO tocar)
- `protocol/` - Command, CommandSerializer, ProtocolConstants
- `transport/` - Transport, TransportState, WebSocketTransport
- `gesture/` - GestureInterpreter, MultiTouchTracker
- `discovery/` - MdnsDiscovery, ManualDiscovery, ServerInfo
- `service/ConnectionService.kt` - Foreground service
- Todos los XML layouts (10 archivos)
- Navigation graph, menu, resources (strings, colors, dimens, themes)
- AndroidManifest.xml
- build.gradle.kts (app + root + settings)

### Falta Implementar
1. **DI & App Setup** - TouchPCApp, AppModule
2. **Data Layer** - PreferencesManager (DataStore)
3. **MainActivity** - Single Activity con NavHost + BottomNav
4. **ConnectionFragment + ViewModel** - Pantalla de conexion
5. **ServerAdapter** - RecyclerView adapter para lista de servidores
6. **TouchpadFragment + ViewModel** - Pantalla touchpad
7. **TouchpadView** - Custom View para superficie tactil
8. **KeyboardFragment + ViewModel** - Pantalla teclado
9. **KeyboardLayoutView** - Custom View para teclado virtual
10. **ShortcutsFragment + ViewModel + Adapter** - Pantalla atajos
11. **SettingsFragment + ViewModel** - Pantalla ajustes
12. **App Icons** - Mipmap adaptive icons para todas las densidades
13. **Signing Config** - Keystore + release signing
14. **ProGuard refinements** - Reglas para release build

---

## Task 1: Hilt Application Class

**Files:**
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/app/TouchPCApp.kt`

**Step 1: Create TouchPCApp.kt**

```kotlin
package com.touchpc.remotecontrol.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TouchPCApp : Application()
```

**Step 2: Commit**
```bash
git add android-app/app/src/main/java/com/touchpc/remotecontrol/app/TouchPCApp.kt
git commit -m "feat: add Hilt application class"
```

---

## Task 2: Hilt DI Module

**Files:**
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/di/AppModule.kt`

**Step 1: Create AppModule.kt**

```kotlin
package com.touchpc.remotecontrol.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.touchpc.remotecontrol.discovery.ManualDiscovery
import com.touchpc.remotecontrol.discovery.MdnsDiscovery
import com.touchpc.remotecontrol.transport.WebSocketTransport
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "touchpc_settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}
```

> Nota: WebSocketTransport, MdnsDiscovery y ManualDiscovery ya usan `@Inject constructor()` y `@Singleton`, asi que Hilt los provee automaticamente sin necesidad de `@Provides`.

**Step 2: Commit**
```bash
git add android-app/app/src/main/java/com/touchpc/remotecontrol/di/AppModule.kt
git commit -m "feat: add Hilt DI module with DataStore provider"
```

---

## Task 3: PreferencesManager (DataStore)

**Files:**
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/data/PreferencesManager.kt`

**Step 1: Create PreferencesManager.kt**

```kotlin
package com.touchpc.remotecontrol.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class TouchSettings(
    val sensitivity: Float = 1.0f,
    val acceleration: Float = 1.0f,
    val tapToClick: Boolean = true,
    val scrollSpeed: Float = 1.0f,
    val naturalScrolling: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val themeMode: Int = 0 // 0=system, 1=light, 2=dark
)

data class ServerHistoryEntry(
    val name: String,
    val host: String,
    val port: Int,
    val lastConnected: Long
)

@Singleton
class PreferencesManager @Inject constructor(
    private val dataStore: DataStore<androidx.datastore.preferences.core.Preferences>
) {
    companion object {
        private val KEY_SENSITIVITY = floatPreferencesKey("sensitivity")
        private val KEY_ACCELERATION = floatPreferencesKey("acceleration")
        private val KEY_TAP_TO_CLICK = booleanPreferencesKey("tap_to_click")
        private val KEY_SCROLL_SPEED = floatPreferencesKey("scroll_speed")
        private val KEY_NATURAL_SCROLL = booleanPreferencesKey("natural_scrolling")
        private val KEY_VIBRATION = booleanPreferencesKey("vibration")
        private val KEY_THEME = intPreferencesKey("theme_mode")
        private val KEY_SERVER_HISTORY = stringPreferencesKey("server_history")
    }

    val settings: Flow<TouchSettings> = dataStore.data.map { prefs ->
        TouchSettings(
            sensitivity = prefs[KEY_SENSITIVITY] ?: 1.0f,
            acceleration = prefs[KEY_ACCELERATION] ?: 1.0f,
            tapToClick = prefs[KEY_TAP_TO_CLICK] ?: true,
            scrollSpeed = prefs[KEY_SCROLL_SPEED] ?: 1.0f,
            naturalScrolling = prefs[KEY_NATURAL_SCROLL] ?: false,
            vibrationEnabled = prefs[KEY_VIBRATION] ?: true,
            themeMode = prefs[KEY_THEME] ?: 0
        )
    }

    val serverHistory: Flow<List<ServerHistoryEntry>> = dataStore.data.map { prefs ->
        val json = prefs[KEY_SERVER_HISTORY] ?: "[]"
        parseServerHistory(json)
    }

    suspend fun updateSensitivity(value: Float) {
        dataStore.edit { it[KEY_SENSITIVITY] = value }
    }

    suspend fun updateAcceleration(value: Float) {
        dataStore.edit { it[KEY_ACCELERATION] = value }
    }

    suspend fun updateTapToClick(enabled: Boolean) {
        dataStore.edit { it[KEY_TAP_TO_CLICK] = enabled }
    }

    suspend fun updateScrollSpeed(value: Float) {
        dataStore.edit { it[KEY_SCROLL_SPEED] = value }
    }

    suspend fun updateNaturalScrolling(enabled: Boolean) {
        dataStore.edit { it[KEY_NATURAL_SCROLL] = enabled }
    }

    suspend fun updateVibration(enabled: Boolean) {
        dataStore.edit { it[KEY_VIBRATION] = enabled }
    }

    suspend fun updateThemeMode(mode: Int) {
        dataStore.edit { it[KEY_THEME] = mode }
    }

    suspend fun addServerToHistory(name: String, host: String, port: Int) {
        dataStore.edit { prefs ->
            val json = prefs[KEY_SERVER_HISTORY] ?: "[]"
            val list = parseServerHistory(json).toMutableList()
            list.removeAll { it.host == host && it.port == port }
            list.add(0, ServerHistoryEntry(name, host, port, System.currentTimeMillis()))
            if (list.size > 10) list.subList(10, list.size).clear()
            prefs[KEY_SERVER_HISTORY] = serializeServerHistory(list)
        }
    }

    suspend fun clearServerHistory() {
        dataStore.edit { it[KEY_SERVER_HISTORY] = "[]" }
    }

    private fun parseServerHistory(json: String): List<ServerHistoryEntry> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ServerHistoryEntry(
                    name = obj.optString("name", ""),
                    host = obj.getString("host"),
                    port = obj.getInt("port"),
                    lastConnected = obj.optLong("lastConnected", 0)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeServerHistory(list: List<ServerHistoryEntry>): String {
        val array = JSONArray()
        list.forEach { entry ->
            array.put(JSONObject().apply {
                put("name", entry.name)
                put("host", entry.host)
                put("port", entry.port)
                put("lastConnected", entry.lastConnected)
            })
        }
        return array.toString()
    }
}
```

**Step 2: Commit**
```bash
git add android-app/app/src/main/java/com/touchpc/remotecontrol/data/PreferencesManager.kt
git commit -m "feat: add PreferencesManager with DataStore for settings and server history"
```

---

## Task 4: MainActivity

**Files:**
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/app/MainActivity.kt`

**Step 1: Create MainActivity.kt**

```kotlin
package com.touchpc.remotecontrol.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.touchpc.remotecontrol.R
import com.touchpc.remotecontrol.databinding.ActivityMainBinding
import com.touchpc.remotecontrol.service.ConnectionService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var serviceBound = false
    private var connectionService: ConnectionService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ConnectionService.ConnectionBinder
            connectionService = binder.getService()
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            connectionService = null
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        // Hide bottom nav on connection screen, show on others
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.connectionFragment -> binding.bottomNav.visibility = View.GONE
                else -> binding.bottomNav.visibility = View.VISIBLE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, ConnectionService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    fun getConnectionService(): ConnectionService? = connectionService
}
```

**Step 2: Commit**
```bash
git add android-app/app/src/main/java/com/touchpc/remotecontrol/app/MainActivity.kt
git commit -m "feat: add MainActivity with NavController and service binding"
```

---

## Task 5: ConnectionFragment + ViewModel + ServerAdapter

**Files:**
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/ui/connection/ConnectionViewModel.kt`
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/ui/connection/ServerAdapter.kt`
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/ui/connection/ConnectionFragment.kt`

**Step 1: Create ConnectionViewModel.kt**

```kotlin
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

    fun startDiscovery() {
        mdnsDiscovery.startDiscovery(getApplication())
    }

    fun stopDiscovery() {
        mdnsDiscovery.stopDiscovery()
    }

    fun connect(host: String, portStr: String) {
        val port = portStr.toIntOrNull() ?: 9876
        val validation = manualDiscovery.validateInput(host, port)

        when (validation) {
            is ManualDiscovery.ValidationResult.Error -> {
                _validationError.value = validation.message
            }
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

    fun connectToHistoryEntry(entry: ServerHistoryEntry) {
        viewModelScope.launch {
            preferencesManager.addServerToHistory(entry.name, entry.host, entry.port)
            transport.connect(entry.host, entry.port)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            transport.disconnect()
        }
    }

    fun sendHandshake(pin: String) {
        viewModelScope.launch {
            val nonce = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
            val pinBytes = pin.toByteArray(Charsets.UTF_8)
            val hashInput = pinBytes + nonce
            val hash = java.security.MessageDigest.getInstance("SHA-256").digest(hashInput)
            val command = com.touchpc.remotecontrol.protocol.Command.Handshake(
                com.touchpc.remotecontrol.protocol.ProtocolConstants.PROTOCOL_VERSION,
                hash,
                nonce
            )
            transport.sendCommand(command)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}
```

**Step 2: Create ServerAdapter.kt**

```kotlin
package com.touchpc.remotecontrol.ui.connection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.touchpc.remotecontrol.databinding.ItemServerBinding

data class ServerItem(
    val name: String,
    val host: String,
    val port: Int
)

class ServerAdapter(
    private val onServerClick: (ServerItem) -> Unit
) : ListAdapter<ServerItem, ServerAdapter.ViewHolder>(ServerDiffCallback()) {

    inner class ViewHolder(
        private val binding: ItemServerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ServerItem) {
            binding.serverName.text = item.name
            binding.serverAddress.text = "${item.host}:${item.port}"
            binding.root.setOnClickListener { onServerClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class ServerDiffCallback : DiffUtil.ItemCallback<ServerItem>() {
        override fun areItemsTheSame(oldItem: ServerItem, newItem: ServerItem): Boolean {
            return oldItem.host == newItem.host && oldItem.port == newItem.port
        }

        override fun areContentsTheSame(oldItem: ServerItem, newItem: ServerItem): Boolean {
            return oldItem == newItem
        }
    }
}
```

**Step 3: Create ConnectionFragment.kt**

```kotlin
package com.touchpc.remotecontrol.ui.connection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.touchpc.remotecontrol.R
import com.touchpc.remotecontrol.databinding.DialogPinEntryBinding
import com.touchpc.remotecontrol.databinding.FragmentConnectionBinding
import com.touchpc.remotecontrol.discovery.ServerInfo
import com.touchpc.remotecontrol.service.ConnectionService
import com.touchpc.remotecontrol.transport.TransportState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConnectionFragment : Fragment() {

    private var _binding: FragmentConnectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ConnectionViewModel by viewModels()

    private lateinit var discoveredAdapter: ServerAdapter
    private lateinit var historyAdapter: ServerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupClickListeners()
        observeState()

        viewModel.startDiscovery()
    }

    private fun setupAdapters() {
        discoveredAdapter = ServerAdapter { server ->
            viewModel.connectToServer(ServerInfo(server.name, server.host, server.port))
        }
        binding.discoveredServersList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = discoveredAdapter
        }

        historyAdapter = ServerAdapter { server ->
            viewModel.connect(server.host, server.port.toString())
        }
        binding.serverHistoryList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun setupClickListeners() {
        binding.connectButton.setOnClickListener {
            val host = binding.ipInput.text?.toString()?.trim() ?: ""
            val port = binding.portInput.text?.toString()?.trim() ?: "9876"
            viewModel.connect(host, port)
        }

        binding.disconnectButton.setOnClickListener {
            viewModel.disconnect()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.transportState.collect { state ->
                        updateStatusUI(state)

                        if (state is TransportState.Connected) {
                            showPinDialog()
                        }
                    }
                }

                launch {
                    viewModel.discoveredServers.collect { servers ->
                        val items = servers.map { ServerItem(it.name, it.host, it.port) }
                        discoveredAdapter.submitList(items)
                        binding.scanProgress.visibility =
                            if (items.isEmpty()) View.VISIBLE else View.GONE
                        binding.noServersText.text =
                            if (items.isEmpty()) getString(R.string.scanning)
                            else ""
                        binding.noServersText.visibility =
                            if (items.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.serverHistory.collect { history ->
                        val items = history.map { ServerItem(it.name, it.host, it.port) }
                        historyAdapter.submitList(items)
                    }
                }

                launch {
                    viewModel.validationError.collect { error ->
                        binding.ipInputLayout.error = error
                    }
                }
            }
        }
    }

    private fun updateStatusUI(state: TransportState) {
        val (text, colorRes) = when (state) {
            is TransportState.Connected -> getString(R.string.connected) to R.color.status_connected
            is TransportState.Connecting -> getString(R.string.connecting) to R.color.status_connecting
            is TransportState.Reconnecting -> getString(R.string.reconnecting) to R.color.status_connecting
            is TransportState.Error -> "${getString(R.string.connection_error)}: ${state.message}" to R.color.status_error
            is TransportState.Disconnected -> getString(R.string.disconnected) to R.color.status_disconnected
        }
        binding.statusText.text = text
        binding.statusIndicator.setBackgroundColor(
            resources.getColor(colorRes, requireContext().theme)
        )
        binding.disconnectButton.visibility =
            if (state is TransportState.Connected) View.VISIBLE else View.GONE
    }

    private fun showPinDialog() {
        val dialogBinding = DialogPinEntryBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_TouchPC_Dialog)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.ok) { _, _ ->
                val pin = dialogBinding.pinInput.text?.toString() ?: ""
                if (pin.isNotEmpty()) {
                    viewModel.sendHandshake(pin)
                    ConnectionService.startService(requireContext())
                    findNavController().navigate(R.id.action_connection_to_touchpad)
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                viewModel.disconnect()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

**Step 4: Commit**
```bash
git add android-app/app/src/main/java/com/touchpc/remotecontrol/ui/connection/
git commit -m "feat: add ConnectionFragment, ViewModel, and ServerAdapter"
```

---

## Task 6: TouchpadView (Custom View)

**Files:**
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/ui/touchpad/TouchpadView.kt`

**Step 1: Create TouchpadView.kt**

```kotlin
package com.touchpc.remotecontrol.ui.touchpad

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.touchpc.remotecontrol.R
import com.touchpc.remotecontrol.gesture.GestureInterpreter
import com.touchpc.remotecontrol.protocol.Command

class TouchpadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gestureInterpreter = GestureInterpreter(context)
    private var onCommandsListener: ((List<Command>) -> Unit)? = null

    private val borderPaint = Paint().apply {
        color = context.getColor(R.color.touchpad_border)
        style = Paint.Style.STROKE
        strokeWidth = 2f * context.resources.displayMetrics.density
    }

    fun setOnCommandsListener(listener: (List<Command>) -> Unit) {
        onCommandsListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val commands = gestureInterpreter.onTouchEvent(event)
        if (commands.isNotEmpty()) {
            onCommandsListener?.invoke(commands)
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = borderPaint.strokeWidth / 2f
        canvas.drawRoundRect(
            inset, inset,
            width.toFloat() - inset, height.toFloat() - inset,
            resources.getDimension(R.dimen.touchpad_corner_radius),
            resources.getDimension(R.dimen.touchpad_corner_radius),
            borderPaint
        )
    }

    fun reset() {
        gestureInterpreter.reset()
    }
}
```

**Step 2: Commit**
```bash
git add android-app/app/src/main/java/com/touchpc/remotecontrol/ui/touchpad/TouchpadView.kt
git commit -m "feat: add TouchpadView custom view with gesture interpretation"
```

---

## Task 7: TouchpadFragment + ViewModel

**Files:**
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/ui/touchpad/TouchpadViewModel.kt`
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/ui/touchpad/TouchpadFragment.kt`

**Step 1: Create TouchpadViewModel.kt**

```kotlin
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

    fun sendCommands(commands: List<Command>) {
        commands.forEach { transport.sendCommand(it) }
    }

    fun sendMouseButton(button: Int, action: Int) {
        transport.sendCommand(Command.MouseButton(button, action))
    }

    fun leftClick() = sendMouseButton(ProtocolConstants.MOUSE_BUTTON_LEFT, ProtocolConstants.ACTION_CLICK)
    fun rightClick() = sendMouseButton(ProtocolConstants.MOUSE_BUTTON_RIGHT, ProtocolConstants.ACTION_CLICK)
    fun middleClick() = sendMouseButton(ProtocolConstants.MOUSE_BUTTON_MIDDLE, ProtocolConstants.ACTION_CLICK)
}
```

**Step 2: Create TouchpadFragment.kt**

```kotlin
package com.touchpc.remotecontrol.ui.touchpad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.touchpc.remotecontrol.R
import com.touchpc.remotecontrol.databinding.FragmentTouchpadBinding
import com.touchpc.remotecontrol.transport.TransportState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TouchpadFragment : Fragment() {

    private var _binding: FragmentTouchpadBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TouchpadViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTouchpadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.touchpadView.setOnCommandsListener { commands ->
            viewModel.sendCommands(commands)
        }

        binding.btnLeftClick.setOnClickListener { viewModel.leftClick() }
        binding.btnRightClick.setOnClickListener { viewModel.rightClick() }
        binding.btnMiddleClick.setOnClickListener { viewModel.middleClick() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.transportState.collect { state ->
                    val isConnected = state is TransportState.Connected
                    binding.connectionStatus.visibility = if (isConnected) View.GONE else View.VISIBLE
                    binding.connectionStatus.text = when (state) {
                        is TransportState.Disconnected -> getString(R.string.not_connected)
                        is TransportState.Error -> "${getString(R.string.connection_error)}: ${state.message}"
                        is TransportState.Reconnecting -> getString(R.string.reconnecting)
                        else -> ""
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

**Step 3: Commit**
```bash
git add android-app/app/src/main/java/com/touchpc/remotecontrol/ui/touchpad/TouchpadViewModel.kt
git add android-app/app/src/main/java/com/touchpc/remotecontrol/ui/touchpad/TouchpadFragment.kt
git commit -m "feat: add TouchpadFragment and ViewModel"
```

---

## Task 8: KeyboardLayoutView (Custom View)

**Files:**
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/ui/keyboard/KeyboardLayoutView.kt`

**Step 1: Create KeyboardLayoutView.kt**

Teclado virtual con 3 layouts (QWERTY, F-Keys, Numpad) renderizado via Canvas con soporte para modificadores.

```kotlin
package com.touchpc.remotecontrol.ui.keyboard

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.touchpc.remotecontrol.R
import com.touchpc.remotecontrol.protocol.Command
import com.touchpc.remotecontrol.protocol.ProtocolConstants

data class KeyDef(
    val label: String,
    val keycode: Int,
    val widthWeight: Float = 1f,
    val isModifier: Boolean = false
)

class KeyboardLayoutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var onKeyCommandListener: ((Command) -> Unit)? = null
    private var currentLayout = 0 // 0=QWERTY, 1=F-Keys, 2=Numpad
    private var activeModifiers = 0

    private val density = resources.displayMetrics.density
    private val keyHeight = resources.getDimension(R.dimen.key_height)
    private val keyMargin = resources.getDimension(R.dimen.key_margin)
    private val cornerRadius = resources.getDimension(R.dimen.key_corner_radius)

    private val keyBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.key_background)
        style = Paint.Style.FILL
    }
    private val keyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.key_pressed)
        style = Paint.Style.FILL
    }
    private val modifierActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.key_modifier_active)
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.key_text)
        textSize = resources.getDimension(R.dimen.key_text_size)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val modifierTextPaint = Paint(textPaint).apply {
        color = context.getColor(R.color.key_modifier_text)
    }

    private var pressedKeyIndex: Int = -1
    private var pressedRowIndex: Int = -1

    // Keycodes: using Android KeyEvent codes for transmission
    private val qwertyLayout = listOf(
        listOf(KeyDef("Q", 0x51), KeyDef("W", 0x57), KeyDef("E", 0x45), KeyDef("R", 0x52), KeyDef("T", 0x54), KeyDef("Y", 0x59), KeyDef("U", 0x55), KeyDef("I", 0x49), KeyDef("O", 0x4F), KeyDef("P", 0x50)),
        listOf(KeyDef("A", 0x41), KeyDef("S", 0x53), KeyDef("D", 0x44), KeyDef("F", 0x46), KeyDef("G", 0x47), KeyDef("H", 0x48), KeyDef("J", 0x4A), KeyDef("K", 0x4B), KeyDef("L", 0x4C)),
        listOf(KeyDef("Shift", 0x10, 1.5f, true), KeyDef("Z", 0x5A), KeyDef("X", 0x58), KeyDef("C", 0x43), KeyDef("V", 0x56), KeyDef("B", 0x42), KeyDef("N", 0x4E), KeyDef("M", 0x4D), KeyDef("Del", 0x08, 1.5f)),
        listOf(KeyDef("Ctrl", 0x11, 1.5f, true), KeyDef("Alt", 0x12, 1f, true), KeyDef("Space", 0x20, 4f), KeyDef("Enter", 0x0D, 1.5f), KeyDef("Esc", 0x1B, 1f))
    )

    private val fKeysLayout = listOf(
        listOf(KeyDef("F1", 0x70), KeyDef("F2", 0x71), KeyDef("F3", 0x72), KeyDef("F4", 0x73)),
        listOf(KeyDef("F5", 0x74), KeyDef("F6", 0x75), KeyDef("F7", 0x76), KeyDef("F8", 0x77)),
        listOf(KeyDef("F9", 0x78), KeyDef("F10", 0x79), KeyDef("F11", 0x7A), KeyDef("F12", 0x7B)),
        listOf(KeyDef("PrtSc", 0x2C), KeyDef("ScrLk", 0x91), KeyDef("Pause", 0x13), KeyDef("Ins", 0x2D)),
        listOf(KeyDef("Home", 0x24), KeyDef("End", 0x23), KeyDef("PgUp", 0x21), KeyDef("PgDn", 0x22)),
        listOf(KeyDef("Up", 0x26, 1f), KeyDef("Down", 0x28, 1f), KeyDef("Left", 0x25, 1f), KeyDef("Right", 0x27, 1f), KeyDef("Del", 0x2E, 1f))
    )

    private val numpadLayout = listOf(
        listOf(KeyDef("7", 0x67), KeyDef("8", 0x68), KeyDef("9", 0x69), KeyDef("/", 0x6F)),
        listOf(KeyDef("4", 0x64), KeyDef("5", 0x65), KeyDef("6", 0x66), KeyDef("*", 0x6A)),
        listOf(KeyDef("1", 0x61), KeyDef("2", 0x62), KeyDef("3", 0x63), KeyDef("-", 0x6D)),
        listOf(KeyDef("0", 0x60, 2f), KeyDef(".", 0x6E), KeyDef("+", 0x6B)),
        listOf(KeyDef("Tab", 0x09), KeyDef("Enter", 0x0D, 2f), KeyDef("Bksp", 0x08))
    )

    private fun getCurrentLayout(): List<List<KeyDef>> = when (currentLayout) {
        1 -> fKeysLayout
        2 -> numpadLayout
        else -> qwertyLayout
    }

    fun setLayout(index: Int) {
        currentLayout = index
        invalidate()
    }

    fun setOnKeyCommandListener(listener: (Command) -> Unit) {
        onKeyCommandListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layout = getCurrentLayout()
        val totalRows = layout.size
        val rowH = (height.toFloat() - keyMargin * (totalRows + 1)) / totalRows

        for ((rowIndex, row) in layout.withIndex()) {
            val totalWeight = row.sumOf { it.widthWeight.toDouble() }.toFloat()
            val keyW = (width.toFloat() - keyMargin * (row.size + 1)) / totalWeight
            var x = keyMargin

            for ((keyIndex, key) in row.withIndex()) {
                val w = keyW * key.widthWeight
                val y = keyMargin + rowIndex * (rowH + keyMargin)
                val rect = RectF(x, y, x + w, y + rowH)

                val isPressed = rowIndex == pressedRowIndex && keyIndex == pressedKeyIndex
                val isModActive = key.isModifier && isModifierActive(key.keycode)
                val paint = when {
                    isModActive -> modifierActivePaint
                    isPressed -> keyPressedPaint
                    else -> keyBgPaint
                }
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

                val tp = if (isModActive) modifierTextPaint else textPaint
                val textY = rect.centerY() - (tp.descent() + tp.ascent()) / 2
                canvas.drawText(key.label, rect.centerX(), textY, tp)

                x += w + keyMargin
            }
        }
    }

    private fun isModifierActive(keycode: Int): Boolean = when (keycode) {
        0x11 -> (activeModifiers and ProtocolConstants.MOD_CTRL) != 0
        0x10 -> (activeModifiers and ProtocolConstants.MOD_SHIFT) != 0
        0x12 -> (activeModifiers and ProtocolConstants.MOD_ALT) != 0
        0x5B -> (activeModifiers and ProtocolConstants.MOD_META) != 0
        else -> false
    }

    private fun getModifierBit(keycode: Int): Int = when (keycode) {
        0x11 -> ProtocolConstants.MOD_CTRL
        0x10 -> ProtocolConstants.MOD_SHIFT
        0x12 -> ProtocolConstants.MOD_ALT
        0x5B -> ProtocolConstants.MOD_META
        else -> 0
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val hit = hitTest(event.x, event.y)
                if (hit != null) {
                    pressedRowIndex = hit.first
                    pressedKeyIndex = hit.second
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (pressedRowIndex >= 0 && pressedKeyIndex >= 0) {
                    val layout = getCurrentLayout()
                    val key = layout.getOrNull(pressedRowIndex)?.getOrNull(pressedKeyIndex)
                    if (key != null) {
                        handleKeyPress(key)
                    }
                }
                pressedRowIndex = -1
                pressedKeyIndex = -1
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedRowIndex = -1
                pressedKeyIndex = -1
                invalidate()
            }
        }
        return true
    }

    private fun handleKeyPress(key: KeyDef) {
        if (key.isModifier) {
            val bit = getModifierBit(key.keycode)
            activeModifiers = activeModifiers xor bit
            invalidate()
        } else {
            onKeyCommandListener?.invoke(
                Command.KeyEvent(key.keycode, ProtocolConstants.ACTION_CLICK, activeModifiers)
            )
            // Clear non-sticky modifiers after key press
            activeModifiers = 0
            invalidate()
        }
    }

    private fun hitTest(x: Float, y: Float): Pair<Int, Int>? {
        val layout = getCurrentLayout()
        val totalRows = layout.size
        val rowH = (height.toFloat() - keyMargin * (totalRows + 1)) / totalRows

        for ((rowIndex, row) in layout.withIndex()) {
            val totalWeight = row.sumOf { it.widthWeight.toDouble() }.toFloat()
            val keyW = (width.toFloat() - keyMargin * (row.size + 1)) / totalWeight
            var kx = keyMargin
            val ky = keyMargin + rowIndex * (rowH + keyMargin)

            for ((keyIndex, key) in row.withIndex()) {
                val w = keyW * key.widthWeight
                if (x in kx..(kx + w) && y in ky..(ky + rowH)) {
                    return Pair(rowIndex, keyIndex)
                }
                kx += w + keyMargin
            }
        }
        return null
    }
}
```

**Step 2: Commit**
```bash
git add android-app/app/src/main/java/com/touchpc/remotecontrol/ui/keyboard/KeyboardLayoutView.kt
git commit -m "feat: add KeyboardLayoutView custom view with QWERTY, F-Keys, Numpad"
```

---

## Task 9: KeyboardFragment + ViewModel

**Files:**
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/ui/keyboard/KeyboardViewModel.kt`
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/ui/keyboard/KeyboardFragment.kt`

**Step 1: Create KeyboardViewModel.kt**

```kotlin
package com.touchpc.remotecontrol.ui.keyboard

import androidx.lifecycle.ViewModel
import com.touchpc.remotecontrol.protocol.Command
import com.touchpc.remotecontrol.transport.WebSocketTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class KeyboardViewModel @Inject constructor(
    private val transport: WebSocketTransport
) : ViewModel() {

    fun sendKeyCommand(command: Command) {
        transport.sendCommand(command)
    }

    fun sendText(text: String) {
        if (text.isNotEmpty()) {
            transport.sendCommand(Command.KeyText(text))
        }
    }
}
```

**Step 2: Create KeyboardFragment.kt**

```kotlin
package com.touchpc.remotecontrol.ui.keyboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayout
import com.touchpc.remotecontrol.R
import com.touchpc.remotecontrol.databinding.FragmentKeyboardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class KeyboardFragment : Fragment() {

    private var _binding: FragmentKeyboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: KeyboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKeyboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup tabs
        binding.keyboardTabs.addTab(binding.keyboardTabs.newTab().setText(R.string.tab_qwerty))
        binding.keyboardTabs.addTab(binding.keyboardTabs.newTab().setText(R.string.tab_fkeys))
        binding.keyboardTabs.addTab(binding.keyboardTabs.newTab().setText(R.string.tab_numpad))

        binding.keyboardTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.keyboardLayoutView.setLayout(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.keyboardLayoutView.setOnKeyCommandListener { command ->
            viewModel.sendKeyCommand(command)
        }

        binding.sendTextButton.setOnClickListener {
            val text = binding.textInput.text?.toString() ?: ""
            viewModel.sendText(text)
            binding.textInput.text?.clear()
        }

        binding.textInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = binding.textInput.text?.toString() ?: ""
                viewModel.sendText(text)
                binding.textInput.text?.clear()
                true
            } else false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

**Step 3: Commit**
```bash
git add android-app/app/src/main/java/com/touchpc/remotecontrol/ui/keyboard/
git commit -m "feat: add KeyboardFragment and ViewModel with text input"
```

---

## Task 10: ShortcutsFragment + ViewModel + Adapter

**Files:**
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/ui/shortcuts/ShortcutsViewModel.kt`
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/ui/shortcuts/ShortcutAdapter.kt`
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/ui/shortcuts/ShortcutsFragment.kt`

**Step 1: Create ShortcutsViewModel.kt**

```kotlin
package com.touchpc.remotecontrol.ui.shortcuts

import androidx.lifecycle.ViewModel
import com.touchpc.remotecontrol.protocol.Command
import com.touchpc.remotecontrol.transport.WebSocketTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ShortcutItem(
    val id: String,
    val name: String,
    val iconResId: Int = android.R.drawable.ic_menu_send
)

@HiltViewModel
class ShortcutsViewModel @Inject constructor(
    private val transport: WebSocketTransport
) : ViewModel() {

    val defaultShortcuts = listOf(
        ShortcutItem("copy", "Copy", android.R.drawable.ic_menu_edit),
        ShortcutItem("paste", "Paste", android.R.drawable.ic_menu_edit),
        ShortcutItem("cut", "Cut", android.R.drawable.ic_menu_edit),
        ShortcutItem("undo", "Undo", android.R.drawable.ic_menu_revert),
        ShortcutItem("redo", "Redo", android.R.drawable.ic_menu_revert),
        ShortcutItem("select_all", "Select All", android.R.drawable.ic_menu_agenda),
        ShortcutItem("save", "Save", android.R.drawable.ic_menu_save),
        ShortcutItem("find", "Find", android.R.drawable.ic_menu_search),
        ShortcutItem("tab_close", "Close Tab", android.R.drawable.ic_menu_close_clear_cancel),
        ShortcutItem("tab_new", "New Tab", android.R.drawable.ic_menu_add),
        ShortcutItem("alt_tab", "Alt+Tab", android.R.drawable.ic_menu_sort_by_size),
        ShortcutItem("alt_f4", "Alt+F4", android.R.drawable.ic_menu_close_clear_cancel),
        ShortcutItem("win_d", "Desktop", android.R.drawable.ic_menu_myplaces)
    )

    fun executeShortcut(shortcutId: String) {
        transport.sendCommand(Command.CustomShortcut(shortcutId))
    }
}
```

**Step 2: Create ShortcutAdapter.kt**

```kotlin
package com.touchpc.remotecontrol.ui.shortcuts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.touchpc.remotecontrol.databinding.ItemShortcutBinding

class ShortcutAdapter(
    private val shortcuts: List<ShortcutItem>,
    private val onClick: (ShortcutItem) -> Unit
) : RecyclerView.Adapter<ShortcutAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: ItemShortcutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ShortcutItem) {
            binding.shortcutName.text = item.name
            binding.shortcutIcon.setImageResource(item.iconResId)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShortcutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(shortcuts[position])
    }

    override fun getItemCount() = shortcuts.size
}
```

**Step 3: Create ShortcutsFragment.kt**

```kotlin
package com.touchpc.remotecontrol.ui.shortcuts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.touchpc.remotecontrol.databinding.FragmentShortcutsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShortcutsFragment : Fragment() {

    private var _binding: FragmentShortcutsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ShortcutsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShortcutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ShortcutAdapter(viewModel.defaultShortcuts) { shortcut ->
            viewModel.executeShortcut(shortcut.id)
        }

        binding.shortcutsGrid.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.shortcutsGrid.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

**Step 4: Commit**
```bash
git add android-app/app/src/main/java/com/touchpc/remotecontrol/ui/shortcuts/
git commit -m "feat: add ShortcutsFragment, ViewModel, and adapter with 13 predefined shortcuts"
```

---

## Task 11: SettingsFragment + ViewModel

**Files:**
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/ui/settings/SettingsViewModel.kt`
- Create: `android-app/app/src/main/java/com/touchpc/remotecontrol/ui/settings/SettingsFragment.kt`

**Step 1: Create SettingsViewModel.kt**

```kotlin
package com.touchpc.remotecontrol.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchpc.remotecontrol.data.PreferencesManager
import com.touchpc.remotecontrol.data.TouchSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val settings: StateFlow<TouchSettings> = preferencesManager.settings
        .stateIn(viewModelScope, SharingStarted.Lazily, TouchSettings())

    fun updateSensitivity(value: Float) {
        viewModelScope.launch { preferencesManager.updateSensitivity(value) }
    }

    fun updateAcceleration(value: Float) {
        viewModelScope.launch { preferencesManager.updateAcceleration(value) }
    }

    fun updateTapToClick(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.updateTapToClick(enabled) }
    }

    fun updateScrollSpeed(value: Float) {
        viewModelScope.launch { preferencesManager.updateScrollSpeed(value) }
    }

    fun updateNaturalScrolling(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.updateNaturalScrolling(enabled) }
    }

    fun updateVibration(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.updateVibration(enabled) }
    }

    fun updateTheme(mode: Int) {
        viewModelScope.launch { preferencesManager.updateThemeMode(mode) }
        val nightMode = when (mode) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun clearHistory() {
        viewModelScope.launch { preferencesManager.clearServerHistory() }
    }
}
```

**Step 2: Create SettingsFragment.kt**

```kotlin
package com.touchpc.remotecontrol.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.touchpc.remotecontrol.BuildConfig
import com.touchpc.remotecontrol.R
import com.touchpc.remotecontrol.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    private var isInitializing = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.versionText.text = getString(R.string.version) + " " + BuildConfig.VERSION_NAME

        setupListeners()
        observeSettings()
    }

    private fun setupListeners() {
        binding.sensitivitySlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.updateSensitivity(value)
        }
        binding.accelerationSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.updateAcceleration(value)
        }
        binding.tapToClickSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isInitializing) viewModel.updateTapToClick(isChecked)
        }
        binding.scrollSpeedSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.updateScrollSpeed(value)
        }
        binding.naturalScrollSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isInitializing) viewModel.updateNaturalScrolling(isChecked)
        }
        binding.vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isInitializing) viewModel.updateVibration(isChecked)
        }
        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (!isInitializing) {
                val mode = when (checkedId) {
                    R.id.theme_light -> 1
                    R.id.theme_dark -> 2
                    else -> 0
                }
                viewModel.updateTheme(mode)
            }
        }
        binding.clearHistoryButton.setOnClickListener {
            viewModel.clearHistory()
            Toast.makeText(requireContext(), R.string.history_cleared, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { settings ->
                    isInitializing = true
                    binding.sensitivitySlider.value = settings.sensitivity
                    binding.accelerationSlider.value = settings.acceleration
                    binding.tapToClickSwitch.isChecked = settings.tapToClick
                    binding.scrollSpeedSlider.value = settings.scrollSpeed
                    binding.naturalScrollSwitch.isChecked = settings.naturalScrolling
                    binding.vibrationSwitch.isChecked = settings.vibrationEnabled
                    when (settings.themeMode) {
                        1 -> binding.themeLight.isChecked = true
                        2 -> binding.themeDark.isChecked = true
                        else -> binding.themeSystem.isChecked = true
                    }
                    isInitializing = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

**Step 3: Commit**
```bash
git add android-app/app/src/main/java/com/touchpc/remotecontrol/ui/settings/
git commit -m "feat: add SettingsFragment and ViewModel with DataStore persistence"
```

---

## Task 12: Generar App Icons (Adaptive Icons)

**Files:**
- Create: `android-app/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `android-app/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Create: `android-app/app/src/main/res/drawable/ic_launcher_foreground.xml` (vector icon)
- Create: `android-app/app/src/main/res/values/ic_launcher_background.xml`
- Create: placeholder PNGs en cada densidad de mipmap

**Step 1: Crear ic_launcher_foreground.xml (vector drawable)**

Un icono de cursor/touchpad con Material Design style.

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group android:translateX="22" android:translateY="22">
        <path
            android:fillColor="#FFFFFF"
            android:pathData="M32,0 L2,0 C0.9,0 0,0.9 0,2 L0,42 C0,43.1 0.9,44 2,44 L14,44 L14,62 L24,48 L62,48 C63.1,48 64,47.1 64,46 L64,2 C64,0.9 63.1,0 62,0 L32,0 Z M10,38 L10,6 L58,6 L58,38 L10,38 Z" />
        <path
            android:fillColor="#FFFFFF"
            android:pathData="M20,14 L20,30 L28,22 L36,30 L36,14" />
    </group>
</vector>
```

**Step 2: Crear adaptive icon XML files**

`mipmap-anydpi-v26/ic_launcher.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```

`mipmap-anydpi-v26/ic_launcher_round.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```

`values/ic_launcher_background.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#1565C0</color>
</resources>
```

**Step 3: Crear mipmap PNG placeholders**

Para pre-API 26, crear PNGs basicos en cada densidad:
- `mipmap-mdpi/ic_launcher.png` (48x48)
- `mipmap-hdpi/ic_launcher.png` (72x72)
- `mipmap-xhdpi/ic_launcher.png` (96x96)
- `mipmap-xxhdpi/ic_launcher.png` (144x144)
- `mipmap-xxxhdpi/ic_launcher.png` (192x192)

> Nota: Generar PNGs reales con Android Studio Image Asset Studio antes del release final. Para compilar, usaremos el adaptive icon con vector que no requiere PNGs en API 26+.

**Step 4: Commit**
```bash
git add android-app/app/src/main/res/mipmap-anydpi-v26/
git add android-app/app/src/main/res/drawable/ic_launcher_foreground.xml
git add android-app/app/src/main/res/values/ic_launcher_background.xml
git commit -m "feat: add adaptive icon with vector foreground"
```

---

## Task 13: Release Signing Configuration

**Files:**
- Modify: `android-app/app/build.gradle.kts`
- Create: `android-app/keystore.properties` (template, NO incluir en git)

**Step 1: Create keystore.properties template**

```properties
storePassword=CHANGE_ME
keyPassword=CHANGE_ME
keyAlias=touchpc-release
storeFile=../touchpc-release.keystore
```

**Step 2: Modify build.gradle.kts para release signing**

Agregar al bloque `android {}`:

```kotlin
import java.util.Properties

// Al inicio del archivo, antes del bloque android
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    // ... existing config ...

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true  // AGREGAR: shrink resources
            signingConfig = signingConfigs.getByName("release")  // AGREGAR
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

**Step 3: Agregar keystore.properties a .gitignore**

```
keystore.properties
*.keystore
*.jks
```

**Step 4: Generar keystore (comando para el desarrollador)**

```bash
keytool -genkey -v -keystore touchpc-release.keystore -alias touchpc-release -keyalg RSA -keysize 2048 -validity 10000
```

**Step 5: Commit**
```bash
git add android-app/app/build.gradle.kts
git add .gitignore
git commit -m "feat: add release signing configuration and resource shrinking"
```

---

## Task 14: ProGuard Refinements

**Files:**
- Modify: `android-app/app/proguard-rules.pro`

**Step 1: Verificar y agregar reglas necesarias**

Agregar reglas para Navigation Component, DataStore, y Kotlin coroutines:

```proguard
# Existing rules should already cover OkHttp, Hilt

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }

# Navigation Component
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

# Keep sealed classes for TransportState
-keep class com.touchpc.remotecontrol.transport.TransportState$* { *; }
-keep class com.touchpc.remotecontrol.protocol.Command$* { *; }
```

**Step 2: Commit**
```bash
git add android-app/app/proguard-rules.pro
git commit -m "fix: add ProGuard rules for coroutines, DataStore, and Navigation"
```

---

## Task 15: Verificacion y Build

**Step 1: Verificar estructura de archivos**

Confirmar que todos los archivos existen en las rutas correctas:
- `app/TouchPCApp.kt`
- `app/MainActivity.kt`
- `di/AppModule.kt`
- `data/PreferencesManager.kt`
- `ui/connection/{ConnectionFragment,ConnectionViewModel,ServerAdapter}.kt`
- `ui/touchpad/{TouchpadFragment,TouchpadViewModel,TouchpadView}.kt`
- `ui/keyboard/{KeyboardFragment,KeyboardViewModel,KeyboardLayoutView}.kt`
- `ui/shortcuts/{ShortcutsFragment,ShortcutsViewModel,ShortcutAdapter}.kt`
- `ui/settings/{SettingsFragment,SettingsViewModel}.kt`

**Step 2: Intentar compilar (debug build)**

```bash
cd android-app && ./gradlew assembleDebug
```

**Step 3: Corregir errores de compilacion si los hay**

Iterar hasta que el build pase.

**Step 4: Build release (sin signing si no hay keystore)**

```bash
cd android-app && ./gradlew assembleRelease
```

**Step 5: Commit final**
```bash
git add -A
git commit -m "build: verify full project compilation"
```

---

## Resumen de Archivos a Crear

| # | Archivo | Descripcion |
|---|---------|-------------|
| 1 | `app/TouchPCApp.kt` | Hilt Application |
| 2 | `di/AppModule.kt` | Hilt DI module |
| 3 | `data/PreferencesManager.kt` | DataStore preferences |
| 4 | `app/MainActivity.kt` | Single Activity |
| 5 | `ui/connection/ConnectionViewModel.kt` | Connection state |
| 6 | `ui/connection/ServerAdapter.kt` | Server list adapter |
| 7 | `ui/connection/ConnectionFragment.kt` | Connection screen |
| 8 | `ui/touchpad/TouchpadView.kt` | Custom touch surface |
| 9 | `ui/touchpad/TouchpadViewModel.kt` | Touchpad state |
| 10 | `ui/touchpad/TouchpadFragment.kt` | Touchpad screen |
| 11 | `ui/keyboard/KeyboardLayoutView.kt` | Custom keyboard view |
| 12 | `ui/keyboard/KeyboardViewModel.kt` | Keyboard state |
| 13 | `ui/keyboard/KeyboardFragment.kt` | Keyboard screen |
| 14 | `ui/shortcuts/ShortcutsViewModel.kt` | Shortcuts state |
| 15 | `ui/shortcuts/ShortcutAdapter.kt` | Shortcuts grid adapter |
| 16 | `ui/shortcuts/ShortcutsFragment.kt` | Shortcuts screen |
| 17 | `ui/settings/SettingsViewModel.kt` | Settings state |
| 18 | `ui/settings/SettingsFragment.kt` | Settings screen |
| 19 | `res/mipmap-anydpi-v26/ic_launcher.xml` | Adaptive icon |
| 20 | `res/mipmap-anydpi-v26/ic_launcher_round.xml` | Round adaptive icon |
| 21 | `res/drawable/ic_launcher_foreground.xml` | Icon foreground vector |
| 22 | `res/values/ic_launcher_background.xml` | Icon background color |

Todos los paths son relativos a `android-app/app/src/main/java/com/touchpc/remotecontrol/` (Kotlin) o `android-app/app/src/main/` (resources).

## Verificacion

1. **Build debug:** `./gradlew assembleDebug` debe completar sin errores
2. **Build release:** `./gradlew assembleRelease` (requiere keystore configurado)
3. **Instalar en dispositivo:** `adb install app/build/outputs/apk/debug/app-debug.apk`
4. **Test manual:**
   - La app abre en ConnectionFragment
   - Se puede ingresar IP manualmente y conectar
   - La discovery mDNS muestra servidores disponibles
   - El touchpad responde a gestos
   - El teclado virtual envia teclas
   - Los shortcuts se ejecutan correctamente
   - Los settings se persisten al reiniciar la app

## Requisitos Play Store (post-build)

- [ ] Generar keystore de produccion con `keytool`
- [ ] Generar PNGs del icono con Android Studio Image Asset Studio
- [ ] Crear Privacy Policy y hospedarla (GitHub Pages funciona)
- [ ] Preparar screenshots (telefono + tablet)
- [ ] Crear cuenta de Google Play Developer ($25 USD one-time)
- [ ] Llenar ficha de la app en Play Console (descripcion, categorias, etc.)
- [ ] Subir AAB: `./gradlew bundleRelease`
