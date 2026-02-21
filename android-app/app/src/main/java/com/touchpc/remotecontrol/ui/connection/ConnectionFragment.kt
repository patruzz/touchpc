package com.touchpc.remotecontrol.ui.connection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        binding.disconnectButton.setOnClickListener { viewModel.disconnect() }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.transportState.collect { state ->
                        updateStatusUI(state)
                        if (state is TransportState.Connected) { showPinDialog() }
                    }
                }
                launch {
                    viewModel.discoveredServers.collect { servers ->
                        val items = servers.map { ServerItem(it.name, it.host, it.port) }
                        discoveredAdapter.submitList(items)
                        binding.scanProgress.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                        binding.noServersText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                        binding.noServersText.text = if (items.isEmpty()) getString(R.string.scanning) else ""
                    }
                }
                launch {
                    viewModel.serverHistory.collect { history ->
                        historyAdapter.submitList(history.map { ServerItem(it.name, it.host, it.port) })
                    }
                }
                launch {
                    viewModel.validationError.collect { error -> binding.ipInputLayout.error = error }
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
        binding.statusIndicator.setBackgroundColor(resources.getColor(colorRes, requireContext().theme))
        binding.disconnectButton.visibility = if (state is TransportState.Connected) View.VISIBLE else View.GONE
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
            .setNegativeButton(R.string.cancel) { _, _ -> viewModel.disconnect() }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
