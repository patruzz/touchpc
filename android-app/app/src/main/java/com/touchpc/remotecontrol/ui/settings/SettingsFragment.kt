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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.versionText.text = "${getString(R.string.version)} ${BuildConfig.VERSION_NAME}"
        setupListeners()
        observeSettings()
    }

    private fun setupListeners() {
        binding.sensitivitySlider.addOnChangeListener { _, value, fromUser -> if (fromUser) viewModel.updateSensitivity(value) }
        binding.accelerationSlider.addOnChangeListener { _, value, fromUser -> if (fromUser) viewModel.updateAcceleration(value) }
        binding.tapToClickSwitch.setOnCheckedChangeListener { _, isChecked -> if (!isInitializing) viewModel.updateTapToClick(isChecked) }
        binding.scrollSpeedSlider.addOnChangeListener { _, value, fromUser -> if (fromUser) viewModel.updateScrollSpeed(value) }
        binding.naturalScrollSwitch.setOnCheckedChangeListener { _, isChecked -> if (!isInitializing) viewModel.updateNaturalScrolling(isChecked) }
        binding.vibrationSwitch.setOnCheckedChangeListener { _, isChecked -> if (!isInitializing) viewModel.updateVibration(isChecked) }
        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (!isInitializing) viewModel.updateTheme(when (checkedId) { R.id.theme_light -> 1; R.id.theme_dark -> 2; else -> 0 })
        }
        binding.clearHistoryButton.setOnClickListener {
            viewModel.clearHistory()
            Toast.makeText(requireContext(), R.string.history_cleared, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { s ->
                    isInitializing = true
                    binding.sensitivitySlider.value = s.sensitivity
                    binding.accelerationSlider.value = s.acceleration
                    binding.tapToClickSwitch.isChecked = s.tapToClick
                    binding.scrollSpeedSlider.value = s.scrollSpeed
                    binding.naturalScrollSwitch.isChecked = s.naturalScrolling
                    binding.vibrationSwitch.isChecked = s.vibrationEnabled
                    when (s.themeMode) { 1 -> binding.themeLight.isChecked = true; 2 -> binding.themeDark.isChecked = true; else -> binding.themeSystem.isChecked = true }
                    isInitializing = false
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
