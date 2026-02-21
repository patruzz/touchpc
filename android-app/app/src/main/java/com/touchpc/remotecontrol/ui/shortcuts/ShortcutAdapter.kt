package com.touchpc.remotecontrol.ui.shortcuts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.touchpc.remotecontrol.databinding.ItemShortcutBinding

class ShortcutAdapter(
    private val shortcuts: List<ShortcutItem>,
    private val onClick: (ShortcutItem) -> Unit
) : RecyclerView.Adapter<ShortcutAdapter.ViewHolder>() {
    inner class ViewHolder(private val binding: ItemShortcutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ShortcutItem) {
            binding.shortcutName.text = item.name
            binding.shortcutIcon.setImageResource(item.iconResId)
            binding.root.setOnClickListener { onClick(item) }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemShortcutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: ViewHolder, position: Int) { holder.bind(shortcuts[position]) }
    override fun getItemCount() = shortcuts.size
}
