package com.touchpc.remotecontrol.ui.shortcuts

import androidx.lifecycle.ViewModel
import com.touchpc.remotecontrol.protocol.Command
import com.touchpc.remotecontrol.transport.WebSocketTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ShortcutItem(val id: String, val name: String, val iconResId: Int = android.R.drawable.ic_menu_send)

@HiltViewModel
class ShortcutsViewModel @Inject constructor(private val transport: WebSocketTransport) : ViewModel() {
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
    fun executeShortcut(shortcutId: String) { transport.sendCommand(Command.CustomShortcut(shortcutId)) }
}
