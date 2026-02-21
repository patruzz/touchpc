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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShortcutsBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.shortcutsGrid.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.shortcutsGrid.adapter = ShortcutAdapter(viewModel.defaultShortcuts) { viewModel.executeShortcut(it.id) }
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
