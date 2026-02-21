package com.touchpc.remotecontrol.ui.connection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.touchpc.remotecontrol.databinding.ItemServerBinding

data class ServerItem(val name: String, val host: String, val port: Int)

class ServerAdapter(
    private val onServerClick: (ServerItem) -> Unit
) : ListAdapter<ServerItem, ServerAdapter.ViewHolder>(ServerDiffCallback()) {

    inner class ViewHolder(private val binding: ItemServerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ServerItem) {
            binding.serverName.text = item.name
            binding.serverAddress.text = "${item.host}:${item.port}"
            binding.root.setOnClickListener { onServerClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) { holder.bind(getItem(position)) }

    private class ServerDiffCallback : DiffUtil.ItemCallback<ServerItem>() {
        override fun areItemsTheSame(oldItem: ServerItem, newItem: ServerItem) = oldItem.host == newItem.host && oldItem.port == newItem.port
        override fun areContentsTheSame(oldItem: ServerItem, newItem: ServerItem) = oldItem == newItem
    }
}
