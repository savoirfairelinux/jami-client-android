/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package cx.ring.fragments

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.channels.Channel
import cx.ring.channels.ChannelRepository
import dagger.hilt.android.AndroidEntryPoint
import net.jami.services.AccountService
import javax.inject.Inject

@AndroidEntryPoint
class ChannelFragment : Fragment() {
    @Inject lateinit var accountService: AccountService

    private lateinit var repository: ChannelRepository
    private lateinit var channelBar: LinearLayout
    private var channels = emptyList<Channel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        repository = ChannelRepository(requireContext(), accountService.currentAccount?.accountId)
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            clipToPadding = false
            setPadding(dp(16), dp(8), dp(16), dp(80))
            // Keep the first name field from grabbing focus (and the keyboard) on open.
            isFocusableInTouchMode = true
            descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }
        // The fragment container is not inset by the system bars: apply them here so the
        // title stays clear of the status bar and the list clear of the navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(dp(16), bars.top + dp(8), dp(16), bars.bottom + dp(16))
            insets
        }
        ViewCompat.requestApplyInsets(root)
        channelBar = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        val channelScroll = ScrollView(requireContext()).apply {
            isVerticalScrollBarEnabled = false
            addView(channelBar, LinearLayout.LayoutParams(-1, -2))
        }
        root.addView(channelScroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val title = TextView(requireContext()).apply {
            text = getString(R.string.channels_title)
            textSize = 22f
            gravity = Gravity.CENTER_VERTICAL
        }
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageButton(requireContext()).apply {
                setImageResource(R.drawable.baseline_arrow_back_24)
                imageTintList = ColorStateList.valueOf(onSurfaceColor())
                contentDescription = getString(android.R.string.cancel)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(8) })
            addView(title, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        root.addView(header, 0, LinearLayout.LayoutParams(-1, dp(56)))
        // Manage Channels is intentionally a channel-only screen. Members and bulk actions
        // belong to the selected Channel screen, not to this management list.
        refreshChannels()
        return root
    }

    private fun refreshChannels() {
        channels = repository.load()
        renderChannelButtons()
    }

    private fun saveChannels(newChannels: List<Channel>) {
        channels = newChannels
        repository.save(channels)
        if (repository.activeChannelName !in channels.map { it.name })
            repository.activeChannelName = channels.first().name
        (parentFragment as? HomeFragment)?.updateChannelBar()
        renderChannelButtons()
    }

    private fun renderChannelButtons() {
        channelBar.removeAllViews()
        channels.forEachIndexed { index, channel ->
            channelBar.addView(
                channelRow(index, channel),
                LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
            )
        }
        channelBar.addView(MaterialButton(requireContext()).apply {
            text = getString(R.string.channels_add)
            contentDescription = getString(R.string.channels_add)
            setOnClickListener { editChannel(null) }
        }, LinearLayout.LayoutParams(-1, dp(56)))
    }

    /** A channel is renamed in place; "All" is the only one that can be neither edited nor deleted. */
    private fun channelRow(index: Int, channel: Channel): View {
        val editable = !channel.isAllContacts
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val name = EditText(requireContext()).apply {
            setText(channel.name)
            setSingleLine()
            textSize = 18f
            isEnabled = editable
            contentDescription = getString(R.string.channels_name_hint)
            imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            setOnEditorActionListener { view, actionId, _ ->
                if (actionId != EditorInfo.IME_ACTION_DONE) false
                else {
                    view.clearFocus()
                    hideKeyboard(view)
                    true
                }
            }
            // The name is the field itself: what is typed is the channel's name, as soon as
            // it is a name the other channels do not already carry.
            if (editable) addTextChangedListener { renameChannel(index, it.toString()) }
        }
        row.addView(name, LinearLayout.LayoutParams(0, dp(56), 1f))
        if (editable) {
            row.addView(ImageButton(requireContext()).apply {
                setImageResource(R.drawable.baseline_delete_24)
                imageTintList = ColorStateList.valueOf(onSurfaceColor())
                contentDescription = getString(R.string.channels_delete)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { confirmDelete(index) }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
        }
        return row
    }

    private fun renameChannel(index: Int, newName: String) {
        val current = channels.getOrNull(index) ?: return
        val name = newName.trim()
        if (name == current.name) return
        // An empty or already used name is not a name: keep the previous one until it is.
        if (name.isEmpty() || channels.filterIndexed { i, _ -> i != index }.any { it.name == name })
            return
        val wasActive = repository.activeChannelName == current.name
        channels = channels.mapIndexed { i, c -> if (i == index) c.copy(name = name) else c }
        repository.save(channels)
        if (wasActive) repository.activeChannelName = name
        (parentFragment as? HomeFragment)?.updateChannelBar()
    }

    private fun confirmDelete(index: Int) {
        val channel = channels.getOrNull(index) ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.channels_delete)
            .setMessage(getString(R.string.channels_delete_confirmation, channel.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                saveChannels(channels.filterIndexed { i, _ -> i != index })
            }
            .show()
    }

    private fun hideKeyboard(view: View) {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(view.windowToken, 0)
    }

    /** Icons drawn on the surface must be legible on it, in light as in dark. */
    private fun onSurfaceColor(): Int = TypedValue().let { value ->
        requireContext().theme
            .resolveAttribute(com.google.android.material.R.attr.colorOnSurface, value, true)
        if (value.resourceId != 0) ContextCompat.getColor(requireContext(), value.resourceId)
        else value.data
    }

    private fun editChannel(channel: Channel?) {
        if (channel?.isAllContacts == true) return
        val input = EditText(requireContext()).apply {
            setText(channel?.name.orEmpty())
            hint = getString(R.string.channels_name_hint)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (channel == null) R.string.channels_add else R.string.channels_edit)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty() || channels.any { it.name == name && it != channel }) return@setPositiveButton
                if (channel != null && repository.activeChannelName == channel.name)
                    repository.activeChannelName = name
                saveChannels(
                    if (channel == null) channels + Channel(name, emptySet())
                    else channels.map { if (it == channel) it.copy(name = name) else it }
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .apply {
                if (channel != null && !channel.isAllContacts) {
                    setNeutralButton(R.string.channels_delete) { _, _ ->
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.channels_delete)
                            .setMessage(getString(R.string.channels_delete_confirmation, channel.name))
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                saveChannels(channels.filterNot { it == channel })
                            }
                            .show()
                    }
                }
            }.show()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
