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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentKeyboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.keyboardTabs.addTab(binding.keyboardTabs.newTab().setText(R.string.tab_qwerty))
        binding.keyboardTabs.addTab(binding.keyboardTabs.newTab().setText(R.string.tab_fkeys))
        binding.keyboardTabs.addTab(binding.keyboardTabs.newTab().setText(R.string.tab_numpad))
        binding.keyboardTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { binding.keyboardLayoutView.setLayout(tab.position) }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        binding.keyboardLayoutView.setOnKeyCommandListener { viewModel.sendKeyCommand(it) }
        binding.sendTextButton.setOnClickListener {
            viewModel.sendText(binding.textInput.text?.toString() ?: ""); binding.textInput.text?.clear()
        }
        binding.textInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { viewModel.sendText(binding.textInput.text?.toString() ?: ""); binding.textInput.text?.clear(); true } else false
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
