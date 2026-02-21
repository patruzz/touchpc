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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTouchpadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.touchpadView.setOnCommandsListener { viewModel.sendCommands(it) }
        binding.btnLeftClick.setOnClickListener { viewModel.leftClick() }
        binding.btnRightClick.setOnClickListener { viewModel.rightClick() }
        binding.btnMiddleClick.setOnClickListener { viewModel.middleClick() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.transportState.collect { state ->
                    binding.connectionStatus.visibility = if (state is TransportState.Connected) View.GONE else View.VISIBLE
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

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
