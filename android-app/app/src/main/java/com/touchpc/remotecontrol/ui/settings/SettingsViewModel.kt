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
class SettingsViewModel @Inject constructor(private val preferencesManager: PreferencesManager) : ViewModel() {
    val settings: StateFlow<TouchSettings> = preferencesManager.settings.stateIn(viewModelScope, SharingStarted.Lazily, TouchSettings())
    fun updateSensitivity(value: Float) { viewModelScope.launch { preferencesManager.updateSensitivity(value) } }
    fun updateAcceleration(value: Float) { viewModelScope.launch { preferencesManager.updateAcceleration(value) } }
    fun updateTapToClick(enabled: Boolean) { viewModelScope.launch { preferencesManager.updateTapToClick(enabled) } }
    fun updateScrollSpeed(value: Float) { viewModelScope.launch { preferencesManager.updateScrollSpeed(value) } }
    fun updateNaturalScrolling(enabled: Boolean) { viewModelScope.launch { preferencesManager.updateNaturalScrolling(enabled) } }
    fun updateVibration(enabled: Boolean) { viewModelScope.launch { preferencesManager.updateVibration(enabled) } }
    fun updateTheme(mode: Int) {
        viewModelScope.launch { preferencesManager.updateThemeMode(mode) }
        AppCompatDelegate.setDefaultNightMode(when (mode) { 1 -> AppCompatDelegate.MODE_NIGHT_NO; 2 -> AppCompatDelegate.MODE_NIGHT_YES; else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM })
    }
    fun clearHistory() { viewModelScope.launch { preferencesManager.clearServerHistory() } }
}
