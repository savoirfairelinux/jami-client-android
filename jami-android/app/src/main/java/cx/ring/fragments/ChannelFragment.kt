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
import android.util.Log
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
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.channels.Channel
import cx.ring.channels.ChannelRepository
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.services.AccountService
import javax.inject.Inject

@AndroidEntryPoint
class ChannelFragment : Fragment() {
    @Inject lateinit var accountService: AccountService

    private lateinit var repository: ChannelRepository
    private lateinit var channelBar: LinearLayout
    private lateinit var addButton: MaterialButton
    private var accountId: String? = null
    private var accountInvalid = false
    private var channelsLoaded = false
    private var updatingNames = false
    private var channels = emptyList<Channel>()
    private val rows = linkedMapOf<String, LinearLayout>()
    private val pendingNames = mutableMapOf<String, String>()
    private val disposables = CompositeDisposable()
    private val writes = PublishSubject.create<Completable>()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        accountId = if (state != null) state.getString(KEY_ACCOUNT)
            else accountService.currentAccount?.accountId
        repository = ChannelRepository(requireContext(), accountService, accountId)
        // Keep accepted edits ordered and let them finish even after this screen closes.
        writes.concatMapCompletable { it.onErrorComplete() }
            .subscribe({}, { error -> Log.e(TAG, "Channel update queue stopped", error) })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_ACCOUNT, accountId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
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
        addButton = MaterialButton(requireContext()).apply {
            text = getString(R.string.channels_add)
            contentDescription = getString(R.string.channels_add)
            isEnabled = false
            setOnClickListener { addChannel() }
        }
        channelBar.addView(addButton, LinearLayout.LayoutParams(-1, dp(56)))
        return root
    }

    override fun onStart() {
        super.onStart()
        disposables.add(accountService.currentAccountSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ account ->
                if (account.accountId != accountId) {
                    accountInvalid = true
                    updateActions()
                    showError(IllegalStateException(getString(R.string.channels_account_changed)))
                }
            }, { error -> showError(error) }))
        disposables.add(repository.observe()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ snapshot ->
                channelsLoaded = true
                reconcileChannels(snapshot)
            }, { error ->
                channelsLoaded = false
                updateActions()
                showError(error)
            }))
    }

    override fun onStop() {
        disposables.clear()
        super.onStop()
    }

    override fun onDestroyView() {
        disposables.clear()
        rows.clear()
        channelsLoaded = false
        super.onDestroyView()
    }

    override fun onDestroy() {
        writes.onComplete()
        super.onDestroy()
    }

    private fun reconcileChannels(snapshot: List<Channel>) {
        channels = snapshot
        val ids = channels.map { it.id }.toSet()
        rows.keys.filterNot { it in ids }.forEach { id ->
            rows.remove(id)?.let { row ->
                if (row.hasFocus()) hideKeyboard(row)
                channelBar.removeView(row)
            }
            pendingNames.remove(id)
        }
        channels.forEach { channel ->
            val row = rows.getOrPut(channel.id) {
                channelRow(channel).also {
                    channelBar.addView(it, channelBar.childCount - 1,
                        LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
                }
            }
            val name = row.getChildAt(0) as EditText
            if (!name.hasFocus() && channel.id !in pendingNames)
                updateName(name, channel.name)
        }
        updateActions()
    }

    /** A channel is renamed in place; "All" is the only one that can be neither edited nor deleted. */
    private fun channelRow(channel: Channel): LinearLayout {
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
            if (editable) {
                addTextChangedListener {
                    if (!updatingNames) renameChannel(channel.id, this, it.toString())
                }
                setOnFocusChangeListener { _, focused ->
                    if (!focused && channel.id !in pendingNames) {
                        channels.firstOrNull { it.id == channel.id }?.let { updateName(this, it.name) }
                    }
                }
            }
        }
        row.addView(name, LinearLayout.LayoutParams(0, dp(56), 1f))
        if (editable) {
            row.addView(ImageButton(requireContext()).apply {
                setImageResource(R.drawable.baseline_delete_24)
                imageTintList = ColorStateList.valueOf(onSurfaceColor())
                contentDescription = getString(R.string.channels_delete)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { confirmDelete(channel.id) }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
        }
        return row
    }

    private fun renameChannel(id: String, input: EditText, newName: String) {
        if (!canEdit()) return
        val current = channels.firstOrNull { it.id == id } ?: run {
            showError(IllegalStateException(getString(R.string.channels_deleted)))
            return
        }
        val name = newName.trim()
        input.error = nameError(id, name)
        if (input.error != null || name == (pendingNames[id] ?: current.name)) return
        pendingNames[id] = name
        submit(repository.rename(id, name), onFinished = {
            if (pendingNames[id] == name) pendingNames.remove(id)
        })
    }

    private fun confirmDelete(id: String) {
        if (!canEdit()) return
        val channel = channels.firstOrNull { it.id == id } ?: return
        val root = view
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.channels_delete)
            .setMessage(getString(R.string.channels_delete_confirmation, channel.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (view === root && canEdit()) submit(repository.delete(id))
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

    private fun addChannel() {
        if (!canEdit()) return
        val root = view
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.channels_name_hint)
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.channels_add)
            .setView(input)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            if (view !== root || !canEdit()) {
                dialog.dismiss()
                return@setOnClickListener
            }
            val name = input.text.toString().trim()
            input.error = nameError(null, name)
            if (input.error == null) {
                submit(repository.create(name))
                dialog.dismiss()
            }
        }
    }

    private fun nameError(id: String?, name: String): String? = when {
        name.isEmpty() -> getString(R.string.channels_name_required)
        channels.any { it.id != id && (pendingNames[it.id] ?: it.name) == name } ->
            getString(R.string.channels_name_exists)
        else -> null
    }

    private fun updateName(input: EditText, name: String) {
        if (input.text.toString() == name) return
        updatingNames = true
        try {
            input.setText(name)
            input.error = null
        } finally {
            updatingNames = false
        }
    }

    private fun updateActions() {
        val enabled = channelsLoaded && !accountInvalid &&
            accountService.currentAccount?.let { it.accountId == accountId } == true
        addButton.isEnabled = enabled
        rows.forEach { (id, row) ->
            val editable = enabled && channels.any { it.id == id && !it.isAllContacts }
            for (index in 0 until row.childCount) row.getChildAt(index).isEnabled = editable
        }
    }

    private fun canEdit(): Boolean {
        val account = accountService.currentAccount
        if (!accountInvalid && channelsLoaded && account != null && account.accountId == accountId)
            return true
        showError(IllegalStateException(getString(
            if (!channelsLoaded) R.string.channels_loading else R.string.channels_account_changed)))
        return false
    }

    private fun submit(operation: Completable, onFinished: () -> Unit = {}) {
        val root = view ?: return
        writes.onNext(operation.observeOn(AndroidSchedulers.mainThread())
            .doOnComplete {
                onFinished()
                if (view === root && !accountInvalid) reconcileChannels(repository.load())
            }
            .doOnError { error ->
                onFinished()
                Log.e(TAG, "Unable to update Channels", error)
                if (view === root && !accountInvalid) showError(error)
            })
    }

    private fun showError(error: Throwable) {
        Log.e(TAG, "Unable to update Channels", error)
        view?.let {
            Snackbar.make(it, error.message ?: getString(R.string.channels_update_error),
                Snackbar.LENGTH_LONG).show()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val KEY_ACCOUNT = "channel_account"
        private const val TAG = "ChannelFragment"
    }
}
