/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package cx.ring.navigation

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.client.BroadcastActivity
import cx.ring.repositories.CircleRepository
import cx.ring.utils.PresenceBadge
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Circle
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import javax.inject.Inject

@AndroidEntryPoint
class CirclesBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var circleRepository: CircleRepository

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var contactService: ContactService

    @Inject
    lateinit var conversationFacade: ConversationFacade

    private val disposables = CompositeDisposable()
    private var listContainer: LinearLayout? = null
    private var currentCircles: List<Circle> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
        }

        // Handle bar
        val handle = View(ctx).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(2).toFloat()
                setColor(Color.parseColor("#44888888"))
            }
        }
        root.addView(handle, LinearLayout.LayoutParams(dp(40), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(14)
        })

        // Header row
        val headerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val badgeIcon = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_circles_24)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#6366F1"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#266366F1"))
            }
        }
        headerRow.addView(badgeIcon, LinearLayout.LayoutParams(dp(40), dp(40)).apply {
            marginEnd = dp(12)
        })

        val titleCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        titleCol.addView(TextView(ctx).apply {
            text = getString(R.string.function_circles)
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        })
        titleCol.addView(TextView(ctx).apply {
            text = getString(R.string.circle_sheet_subtitle)
            textSize = 12f
            alpha = 0.7f
        })
        headerRow.addView(titleCol, LinearLayout.LayoutParams(0, -2, 1f))

        val addBtn = MaterialButton(ctx).apply {
            text = getString(R.string.circle_create_button)
            isAllCaps = false
            textSize = 13f
            insetTop = 0
            insetBottom = 0
            minHeight = dp(38)
            setPadding(dp(16), dp(6), dp(16), dp(6))
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#6366F1"))
            setTextColor(Color.WHITE)
            cornerRadius = dp(19)
            setOnClickListener { showCreateCircleDialog() }
        }
        headerRow.addView(addBtn, LinearLayout.LayoutParams(-2, -2))
        root.addView(headerRow, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(16)
        })

        val scroll = ScrollView(ctx).apply {
            isVerticalScrollBarEnabled = false
        }
        val list = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        listContainer = list
        scroll.addView(list, LinearLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, -2))

        return root
    }

    override fun onStart() {
        super.onStart()
        disposables.add(
            circleRepository.observeForCurrentAccount()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ circles ->
                    currentCircles = circles.filterNot { it.isAllContacts }
                    renderCircles()
                }, { err ->
                    view?.let { Snackbar.make(it, err.message ?: getString(R.string.generic_error), Snackbar.LENGTH_LONG).show() }
                })
        )
    }

    override fun onStop() {
        disposables.clear()
        super.onStop()
    }

    private fun renderCircles() {
        val container = listContainer ?: return
        val ctx = requireContext()
        container.removeAllViews()

        for (circle in currentCircles) {
            val card = MaterialCardView(ctx).apply {
                radius = dp(14).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                strokeColor = Color.parseColor("#336366F1")
                setCardBackgroundColor(Color.parseColor("#0D6366F1"))
                isClickable = true
                isFocusable = true
                setOnClickListener { onCircleClicked(circle) }
            }

            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(10), dp(12))
            }

            val icon = ImageView(ctx).apply {
                setImageResource(R.drawable.ic_circles_24)
                imageTintList = ColorStateList.valueOf(Color.parseColor("#6366F1"))
                setPadding(dp(7), dp(7), dp(7), dp(7))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#266366F1"))
                }
            }
            row.addView(icon, LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                marginEnd = dp(12)
            })

            val textCol = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
            }
            textCol.addView(TextView(ctx).apply {
                text = circleRepository.displayName(circle)
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            val countText = when {
                circle.totalCount == 0 -> getString(R.string.circle_no_members)
                circle.groups.isEmpty() -> resources.getQuantityString(
                    R.plurals.circle_count_contacts, circle.members.size, circle.members.size
                )
                circle.members.isEmpty() -> resources.getQuantityString(
                    R.plurals.circle_count_groups, circle.groups.size, circle.groups.size
                )
                else -> getString(
                    R.string.circle_count_contacts_groups,
                    resources.getQuantityString(R.plurals.circle_count_contacts, circle.members.size, circle.members.size),
                    resources.getQuantityString(R.plurals.circle_count_groups, circle.groups.size, circle.groups.size)
                )
            }
            textCol.addView(TextView(ctx).apply {
                text = countText
                textSize = 12f
                alpha = 0.75f
            })
            row.addView(textCol, LinearLayout.LayoutParams(0, -2, 1f))

            // Members button
            val membersBtn = MaterialButton(
                ctx,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = getString(R.string.conversation_members)
                isAllCaps = false
                textSize = 12f
                insetTop = 0
                insetBottom = 0
                minimumHeight = 0
                minHeight = dp(32)
                setPadding(dp(12), dp(4), dp(12), dp(4))
                strokeColor = ColorStateList.valueOf(Color.parseColor("#666366F1"))
                setTextColor(Color.parseColor("#6366F1"))
                cornerRadius = dp(16)
                setOnClickListener { showManageMembersDialog(circle) }
            }
            row.addView(membersBtn, LinearLayout.LayoutParams(-2, -2).apply {
                marginEnd = dp(6)
            })

            // Broadcast button
            val broadcastBtn = MaterialButton(ctx).apply {
                text = getString(R.string.circle_broadcast)
                isAllCaps = false
                textSize = 12f
                insetTop = 0
                insetBottom = 0
                minimumHeight = 0
                minHeight = dp(32)
                setPadding(dp(12), dp(4), dp(12), dp(4))
                backgroundTintList = ColorStateList.valueOf(Color.parseColor("#6366F1"))
                setTextColor(Color.WHITE)
                cornerRadius = dp(16)
                setOnClickListener { onCircleClicked(circle) }
            }
            row.addView(broadcastBtn, LinearLayout.LayoutParams(-2, -2))

            // Menu button (Rename / Delete)
            val moreBtn = ImageButton(ctx).apply {
                setImageResource(R.drawable.baseline_more_vert_24)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { anchor -> showCircleOptionsMenu(anchor, circle) }
            }
            row.addView(moreBtn, LinearLayout.LayoutParams(dp(34), dp(34)))

            card.addView(row)
            container.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(8)
            })
        }
    }

    private fun onCircleClicked(circle: Circle) {
        if (circle.totalCount == 0) {
            showManageMembersDialog(circle)
            return
        }
        val accountId = accountService.currentAccount?.accountId ?: return
        dismiss()
        startActivity(BroadcastActivity.intent(requireContext(), accountId, circle.id))
    }

    private fun showCircleOptionsMenu(anchor: View, circle: Circle) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add(0, 1, 0, R.string.circle_manage_members)
            menu.add(0, 2, 1, R.string.circle_rename)
            menu.add(0, 3, 2, R.string.circle_delete)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> showManageMembersDialog(circle)
                    2 -> showRenameCircleDialog(circle)
                    3 -> showDeleteCircleDialog(circle)
                }
                true
            }
            show()
        }
    }

    private fun showCreateCircleDialog() {
        val ctx = requireContext()
        val input = EditText(ctx).apply {
            hint = getString(R.string.circle_name_hint)
            setSingleLine()
        }
        val wrapper = LinearLayout(ctx).apply {
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(input, LinearLayout.LayoutParams(-1, -2))
        }
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.circle_new_title)
            .setView(wrapper)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.collab_create) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    disposables.add(
                        circleRepository.create(name)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({}, { err ->
                                view?.let {
                                    Snackbar.make(it, err.message ?: getString(R.string.generic_error), Snackbar.LENGTH_LONG).show()
                                }
                            })
                    )
                }
            }
            .show()
    }

    private fun showRenameCircleDialog(circle: Circle) {
        val ctx = requireContext()
        val input = EditText(ctx).apply {
            setText(circleRepository.displayName(circle))
            setSingleLine()
        }
        val wrapper = LinearLayout(ctx).apply {
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(input, LinearLayout.LayoutParams(-1, -2))
        }
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.circle_rename)
            .setView(wrapper)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.shared_services_save) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    disposables.add(
                        circleRepository.rename(circle.id, name)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({}, { err ->
                                view?.let {
                                    Snackbar.make(it, err.message ?: getString(R.string.generic_error), Snackbar.LENGTH_LONG).show()
                                }
                            })
                    )
                }
            }
            .show()
    }

    private fun showDeleteCircleDialog(circle: Circle) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.circle_delete_title)
            .setMessage(getString(R.string.circle_delete_message, circleRepository.displayName(circle)))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.menu_delete) { _, _ ->
                disposables.add(
                    circleRepository.delete(circle.id)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({}, { err ->
                            view?.let {
                                Snackbar.make(it, err.message ?: getString(R.string.generic_error), Snackbar.LENGTH_LONG).show()
                            }
                        })
                )
            }
            .show()
    }

    private data class CandidateEntry(
        val label: String,
        val uri: String,
        val isGroup: Boolean
    )

    private fun showManageMembersDialog(circle: Circle) {
        val conversations = accountService.currentAccount?.getConversations().orEmpty()
        val candidates = ArrayList<CandidateEntry>()
        val seen = HashSet<String>()
        for (conv in conversations) {
            // Boards are not groups: a Circle can only gather contacts and groups
            if (conv.isFeed())
                continue
            val peer = conv.contacts.firstOrNull { !it.isUser } ?: conv.contacts.firstOrNull()
            val title = conv.profile.blockingFirst().displayName?.takeIf { it.isNotBlank() }
                ?: peer?.profile?.blockingFirst()?.displayName?.takeIf { it.isNotBlank() }
                ?: peer?.username?.blockingGet()?.takeIf { it.isNotBlank() }
                ?: conv.uri.rawUriString
            if (conv.isSwarmGroup()) {
                val rawUri = conv.uri.rawUriString
                if (seen.add(rawUri)) {
                    candidates.add(CandidateEntry(getString(R.string.circle_group_entry, title), rawUri, true))
                }
            } else {
                val contactUri = (peer?.uri ?: conv.uri).rawUriString
                if (seen.add(contactUri)) {
                    candidates.add(CandidateEntry(title, contactUri, false))
                }
            }
        }

        if (candidates.isEmpty()) {
            view?.let {
                Snackbar.make(
                    it,
                    getString(R.string.circle_no_candidates, circleRepository.displayName(circle)),
                    Snackbar.LENGTH_LONG
                ).show()
            }
            return
        }

        val ctx = requireContext()
        val accountId = accountService.currentAccount?.accountId ?: return
        val labels: Array<CharSequence> = candidates.map { it.label as CharSequence }.toTypedArray()
        val checked = BooleanArray(candidates.size) { idx ->
            val c = candidates[idx]
            if (c.isGroup) c.uri in circle.groups else c.uri in circle.members
        }
        val presenceDisposables = CompositeDisposable()

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setOnDismissListener { presenceDisposables.dispose() }
            .setTitle(getString(R.string.circle_members_title, circleRepository.displayName(circle)))
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.shared_services_save) { _, _ ->
                val newMembers = mutableSetOf<String>()
                val newGroups = mutableSetOf<String>()
                for (i in candidates.indices) {
                    if (checked[i]) {
                        if (candidates[i].isGroup) newGroups.add(candidates[i].uri)
                        else newMembers.add(candidates[i].uri)
                    }
                }
                disposables.add(
                    circleRepository.setCircleMembers(circle.id, newMembers, newGroups)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({}, { err ->
                            view?.let {
                                Snackbar.make(it, err.message ?: getString(R.string.generic_error), Snackbar.LENGTH_LONG).show()
                            }
                        })
                )
            }
            .show()

        // Contacts show their presence after their name; groups have none
        candidates.forEachIndexed { index, candidate ->
            if (candidate.isGroup)
                return@forEachIndexed
            presenceDisposables.add(contactService.observeContact(accountId, Uri.fromString(candidate.uri), true)
                .map { it.presence }
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ presence ->
                    labels[index] = PresenceBadge.label(ctx, candidate.label, presence)
                    (dialog.listView?.adapter as? BaseAdapter)?.notifyDataSetChanged()
                }, {}))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val TAG = "CirclesBottomSheet"
        fun newInstance(): CirclesBottomSheet = CirclesBottomSheet()
    }
}
