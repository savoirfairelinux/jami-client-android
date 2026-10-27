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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.repositories.BoardRepository
import cx.ring.repositories.CircleRepository
import cx.ring.utils.PresenceBadge
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Board
import net.jami.model.Contact
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ContactService
import javax.inject.Inject

/**
 * Space dedicated to Boards (daemon Feeds) and their publications.
 */
@AndroidEntryPoint
class BoardBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var boardRepository: BoardRepository

    @Inject
    lateinit var circleRepository: CircleRepository

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var contactService: ContactService

    private val disposables = CompositeDisposable()
    private var listContainer: LinearLayout? = null
    private var currentBoards: List<Board> = emptyList()

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
            setImageResource(R.drawable.baseline_campaign_24)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#F97316"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#26F97316"))
            }
        }
        headerRow.addView(badgeIcon, LinearLayout.LayoutParams(dp(40), dp(40)).apply {
            marginEnd = dp(12)
        })

        val titleCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        titleCol.addView(TextView(ctx).apply {
            text = getString(R.string.board_sheet_title)
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
        })
        titleCol.addView(TextView(ctx).apply {
            text = getString(R.string.board_sheet_subtitle)
            textSize = 12f
            alpha = 0.7f
        })
        headerRow.addView(titleCol, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(headerRow, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(14)
        })

        // Action buttons row: "+ New board" & "Publish"
        val actionsRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val createBoardBtn = MaterialButton(
            ctx,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = getString(R.string.board_new)
            isAllCaps = false
            textSize = 13f
            insetTop = 0
            insetBottom = 0
            minHeight = dp(40)
            strokeColor = ColorStateList.valueOf(Color.parseColor("#80F97316"))
            setTextColor(Color.parseColor("#F97316"))
            cornerRadius = dp(20)
            setOnClickListener { showCreateOrEditBoardDialog(null) }
        }
        actionsRow.addView(createBoardBtn, LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginEnd = dp(8)
        })

        val publishBtn = MaterialButton(ctx).apply {
            text = getString(R.string.board_publish)
            isAllCaps = false
            textSize = 13f
            insetTop = 0
            insetBottom = 0
            minHeight = dp(40)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F97316"))
            setTextColor(Color.WHITE)
            cornerRadius = dp(20)
            setOnClickListener { onQuickPublishClicked() }
        }
        actionsRow.addView(publishBtn, LinearLayout.LayoutParams(0, -2, 1f))

        root.addView(actionsRow, LinearLayout.LayoutParams(-1, -2).apply {
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
        boardRepository.refresh()
        disposables.add(
            boardRepository.observeBoards()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ boards ->
                    currentBoards = boards
                    renderBoards()
                }, { err ->
                    showMessage(err.message ?: getString(R.string.generic_error))
                })
        )
        disposables.add(
            accountService.conversationErrors
                .filter { it.message.contains("Feed") }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { showMessage(feedErrorText(it.message)) }
        )
    }

    override fun onStop() {
        disposables.clear()
        super.onStop()
    }

    private fun showMessage(text: String) {
        view?.let { Snackbar.make(it, text, Snackbar.LENGTH_LONG).show() }
    }

    private fun feedErrorText(message: String): String = when {
        message.contains("not available for subscription") -> getString(R.string.board_error_unavailable)
        message.contains("no longer allows") -> getString(R.string.board_error_no_longer_allowed)
        message.contains("confirmed contact") -> getString(R.string.board_error_confirmed_contacts_only)
        message.contains("not permitted") -> getString(R.string.board_error_not_permitted)
        else -> getString(R.string.board_error_generic, message)
    }

    private fun renderBoards() {
        val container = listContainer ?: return
        val ctx = context ?: return
        container.removeAllViews()

        val ownedBoards = currentBoards.filter { it.owned && !it.closed }
        val followedBoards = currentBoards.filter { !it.owned && it.isFollowed }
        val offeredBoards = currentBoards.filter { it.isOffered }

        if (ownedBoards.isEmpty() && followedBoards.isEmpty() && offeredBoards.isEmpty()) {
            val emptyCard = MaterialCardView(ctx).apply {
                radius = dp(14).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                strokeColor = Color.parseColor("#33F97316")
                setCardBackgroundColor(Color.parseColor("#0DF97316"))
            }
            val emptyCol = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(20), dp(24), dp(20), dp(24))
            }
            emptyCol.addView(TextView(ctx).apply {
                text = getString(R.string.board_empty_title)
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            })
            emptyCol.addView(TextView(ctx).apply {
                text = getString(R.string.board_empty_message)
                textSize = 12f
                alpha = 0.75f
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, dp(14))
            })
            emptyCol.addView(MaterialButton(ctx).apply {
                text = getString(R.string.board_create_first)
                isAllCaps = false
                insetTop = 0
                insetBottom = 0
                minHeight = dp(40)
                setPadding(dp(20), dp(8), dp(20), dp(8))
                backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F97316"))
                setTextColor(Color.WHITE)
                cornerRadius = dp(20)
                setOnClickListener { showCreateOrEditBoardDialog(null) }
            }, LinearLayout.LayoutParams(-2, -2))
            emptyCard.addView(emptyCol)
            container.addView(emptyCard, LinearLayout.LayoutParams(-1, -2))
            return
        }

        addSection(container, getString(R.string.board_section_mine), ownedBoards)
        addSection(container, getString(R.string.board_section_followed), followedBoards)
        addSection(container, getString(R.string.board_section_available), offeredBoards)
    }

    private fun addSection(container: LinearLayout, title: String, boards: List<Board>) {
        if (boards.isEmpty())
            return
        container.addView(sectionHeader("$title (${boards.size})"))
        for (board in boards) {
            container.addView(boardCard(board), LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(8)
            })
        }
    }

    private fun sectionHeader(title: String): TextView = TextView(requireContext()).apply {
        text = title
        setAllCaps(true)
        textSize = 11f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.parseColor("#F97316"))
        setPadding(dp(4), dp(8), dp(4), dp(6))
    }

    private fun smallButton(label: String, filled: Boolean, onClick: () -> Unit): MaterialButton {
        val ctx = requireContext()
        val button = if (filled) MaterialButton(ctx) else MaterialButton(
            ctx,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        )
        return button.apply {
            text = label
            isAllCaps = false
            textSize = 12f
            minimumHeight = 0
            minHeight = dp(32)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            cornerRadius = dp(16)
            if (filled) {
                backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F97316"))
                setTextColor(Color.WHITE)
            } else {
                strokeColor = ColorStateList.valueOf(Color.parseColor("#80F97316"))
                setTextColor(Color.parseColor("#F97316"))
            }
            setOnClickListener { onClick() }
        }
    }

    private fun boardCard(board: Board): View {
        val ctx = requireContext()
        val card = MaterialCardView(ctx).apply {
            radius = dp(14).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = Color.parseColor("#33F97316")
            setCardBackgroundColor(Color.parseColor("#0DF97316"))
            if (board.isFollowed) {
                isClickable = true
                isFocusable = true
                setOnClickListener { openBoardConversation(board) }
            }
        }

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(10), dp(12))
        }

        val icon = ImageView(ctx).apply {
            setImageResource(R.drawable.baseline_campaign_24)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#F97316"))
            setPadding(dp(7), dp(7), dp(7), dp(7))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#26F97316"))
            }
        }
        row.addView(icon, LinearLayout.LayoutParams(dp(36), dp(36)).apply {
            marginEnd = dp(12)
        })

        val textCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        textCol.addView(TextView(ctx).apply {
            text = if (board.unreadCount > 0) "${board.title} (${board.unreadCount})" else board.title
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
        textCol.addView(TextView(ctx).apply {
            text = boardRepository.statusSummary(board)
            textSize = 12f
            alpha = 0.75f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
        row.addView(textCol, LinearLayout.LayoutParams(0, -2, 1f))

        when {
            board.owned -> {
                row.addView(smallButton(getString(R.string.board_publish), true) { openBoardConversation(board) },
                    LinearLayout.LayoutParams(-2, dp(34)).apply { marginEnd = dp(4) })
                val settingsBtn = ImageButton(ctx).apply {
                    setImageResource(R.drawable.baseline_settings_24)
                    setBackgroundColor(Color.TRANSPARENT)
                    contentDescription = getString(R.string.board_manage)
                    setOnClickListener { showCreateOrEditBoardDialog(board) }
                }
                row.addView(settingsBtn, LinearLayout.LayoutParams(dp(34), dp(34)))
            }
            board.subscribed -> {
                row.addView(smallButton(getString(R.string.board_action_view), false) { openBoardConversation(board) },
                    LinearLayout.LayoutParams(-2, dp(34)).apply { marginEnd = dp(2) })
                val moreBtn = ImageButton(ctx).apply {
                    setImageResource(R.drawable.baseline_more_vert_24)
                    setBackgroundColor(Color.TRANSPARENT)
                    contentDescription = getString(R.string.board_options)
                    setOnClickListener { anchor ->
                        PopupMenu(ctx, anchor, Gravity.END).apply {
                            menu.add(0, 1, 0, R.string.board_unsubscribe)
                            setOnMenuItemClickListener { item ->
                                if (item.itemId == 1) confirmUnsubscribe(board)
                                true
                            }
                        }.show()
                    }
                }
                row.addView(moreBtn, LinearLayout.LayoutParams(dp(34), dp(34)))
            }
            board.requested -> {
                row.addView(smallButton(getString(R.string.board_pending), false) {}.apply { isEnabled = false },
                    LinearLayout.LayoutParams(-2, dp(34)))
            }
            else -> {
                row.addView(smallButton(getString(R.string.board_subscribe), true) { subscribe(board) },
                    LinearLayout.LayoutParams(-2, dp(34)))
            }
        }

        card.addView(row)
        return card
    }

    private fun subscribe(board: Board) {
        disposables.add(
            boardRepository.subscribe(board, true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showMessage(getString(R.string.board_subscription_requested, board.title))
                }, { err -> showMessage(err.message ?: getString(R.string.generic_error)) })
        )
    }

    private fun confirmUnsubscribe(board: Board) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.board_unsubscribe_title, board.title))
            .setMessage(R.string.board_unsubscribe_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.board_unsubscribe) { _, _ ->
                disposables.add(
                    boardRepository.subscribe(board, false)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({}, { err -> showMessage(err.message ?: getString(R.string.generic_error)) })
                )
            }
            .show()
    }

    private fun openBoardConversation(board: Board) {
        val homeActivity = activity as? HomeActivity
        dismiss()
        homeActivity?.startConversation(board.accountId, board.uri)
    }

    private fun onQuickPublishClicked() {
        val owned = currentBoards.filter { it.owned && !it.closed }
        when {
            owned.isEmpty() -> showCreateOrEditBoardDialog(null, openOnCreated = true)
            owned.size == 1 -> openBoardConversation(owned.first())
            else -> {
                val titles = owned.map { it.title }.toTypedArray()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.board_choose_for_publish)
                    .setItems(titles) { _, which ->
                        openBoardConversation(owned[which])
                    }
                    .show()
            }
        }
    }

    private fun showCreateOrEditBoardDialog(existing: Board?, openOnCreated: Boolean = false) {
        val ctx = requireContext()
        val account = accountService.currentAccount ?: return
        val circles = circleRepository.load(account.accountId).filterNot { it.isAllContacts }

        // Only confirmed contacts can be authorized by the daemon
        val contactCandidates = ArrayList<Pair<String, String>>()
        val seenUris = HashSet<String>()
        for (conv in account.getConversations()) {
            if (conv.isSwarmGroup())
                continue
            val peer = conv.contacts.firstOrNull { !it.isUser } ?: continue
            val rawId = peer.uri.rawRingId
            if (account.getContact(rawId)?.status != Contact.Status.CONFIRMED || !seenUris.add(rawId))
                continue
            val name = conv.profile.blockingFirst().displayName?.takeIf { it.isNotBlank() }
                ?: peer.profile.blockingFirst().displayName?.takeIf { it.isNotBlank() }
                ?: peer.username?.blockingGet()?.takeIf { it.isNotBlank() }
                ?: rawId
            contactCandidates.add(name to rawId)
        }
        contactCandidates.sortBy { it.first.lowercase() }

        val formScroll = ScrollView(ctx)
        val form = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(8))
        }

        val titleInput = EditText(ctx).apply {
            hint = getString(R.string.board_name_hint)
            setText(existing?.title.orEmpty())
            setSingleLine()
        }
        form.addView(titleInput, LinearLayout.LayoutParams(-1, -2))

        val repliesCheck = CheckBox(ctx).apply {
            text = getString(R.string.board_allow_replies)
            isChecked = existing?.allowReplies ?: false
        }
        form.addView(repliesCheck, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
        })

        val selectedContactUris = existing?.authorizedUris?.toMutableSet() ?: mutableSetOf()
        val contactChecks = HashMap<String, CheckBox>()
        val presenceDisposables = CompositeDisposable()
        val circleMembers = circles.associate { circle ->
            circle.id to circle.members.map { Uri.fromString(it).rawRingId }.filter { it in seenUris }.toSet()
        }
        // A Circle is shown as authorized when all of its reachable members are
        val selectedCircleIds = circles
            .filter { circle -> circleMembers[circle.id].orEmpty().let { it.isNotEmpty() && selectedContactUris.containsAll(it) } }
            .map { it.id }
            .toMutableSet()

        if (circles.isNotEmpty()) {
            form.addView(TextView(ctx).apply {
                text = getString(R.string.board_authorized_circles)
                setTypeface(typeface, Typeface.BOLD)
                textSize = 13f
                setPadding(0, dp(10), 0, dp(4))
            })
            for (circle in circles) {
                val members = circleMembers[circle.id].orEmpty()
                form.addView(CheckBox(ctx).apply {
                    text = resources.getQuantityString(R.plurals.board_circle_option, members.size, circleRepository.displayName(circle), members.size)
                    isChecked = circle.id in selectedCircleIds
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) {
                            selectedCircleIds.add(circle.id)
                            selectedContactUris.addAll(members)
                        } else {
                            selectedCircleIds.remove(circle.id)
                            val stillCovered = selectedCircleIds.flatMap { circleMembers[it].orEmpty() }.toSet()
                            selectedContactUris.removeAll(members - stillCovered)
                        }
                        for ((uri, check) in contactChecks)
                            check.isChecked = uri in selectedContactUris
                    }
                })
            }
        }

        if (contactCandidates.isNotEmpty()) {
            form.addView(TextView(ctx).apply {
                text = getString(R.string.board_authorized_contacts)
                setTypeface(typeface, Typeface.BOLD)
                textSize = 13f
                setPadding(0, dp(10), 0, dp(4))
            })
            for ((name, rawId) in contactCandidates) {
                form.addView(CheckBox(ctx).apply {
                    text = name
                    isChecked = rawId in selectedContactUris
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) selectedContactUris.add(rawId)
                        else selectedContactUris.remove(rawId)
                    }
                    contactChecks[rawId] = this
                    presenceDisposables.add(contactService.observeContact(account.accountId, Uri.fromId(rawId), true)
                        .map { it.presence }
                        .distinctUntilChanged()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ presence -> text = PresenceBadge.label(ctx, name, presence) }, {}))
                })
            }
        } else {
            form.addView(TextView(ctx).apply {
                text = getString(R.string.board_no_confirmed_contacts)
                textSize = 12f
                alpha = 0.7f
                setPadding(0, dp(10), 0, dp(4))
            })
        }

        formScroll.addView(form)

        val builder = MaterialAlertDialogBuilder(ctx)
            .setTitle(if (existing == null) getString(R.string.board_create_title) else getString(R.string.board_manage_title, existing.title))
            .setView(formScroll)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(if (existing == null) R.string.collab_create else R.string.shared_services_save) { _, _ ->
                val title = titleInput.text.toString().trim()
                val replies = repliesCheck.isChecked
                if (title.isEmpty()) {
                    showMessage(getString(R.string.board_name_required))
                    return@setPositiveButton
                }
                if (existing == null) {
                    disposables.add(
                        boardRepository.createBoard(
                            title = title,
                            allowReplies = replies,
                            authorizedContactUris = selectedContactUris,
                            authorizedCircleIds = selectedCircleIds
                        ).observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ created ->
                                if (openOnCreated) openBoardConversation(created)
                                else showMessage(getString(R.string.board_created, created.title))
                            }, { err -> showMessage(err.message ?: getString(R.string.generic_error)) })
                    )
                } else {
                    disposables.add(
                        boardRepository.updateBoard(
                            board = existing,
                            title = title,
                            allowReplies = replies,
                            authorizedContactUris = selectedContactUris,
                            authorizedCircleIds = selectedCircleIds
                        ).observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                showMessage(getString(R.string.board_updated, title))
                            }, { err -> showMessage(err.message ?: getString(R.string.generic_error)) })
                    )
                }
            }

        if (existing != null) {
            builder.setNeutralButton(R.string.board_close) { _, _ -> confirmClose(existing) }
        }

        builder.setOnDismissListener { presenceDisposables.dispose() }
        builder.show()
    }

    private fun confirmClose(board: Board) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.board_close_title, board.title))
            .setMessage(R.string.board_close_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.board_close_confirm) { _, _ ->
                disposables.add(
                    boardRepository.closeBoard(board)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ showMessage(getString(R.string.board_closed, board.title)) },
                            { err -> showMessage(err.message ?: getString(R.string.generic_error)) })
                )
            }
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val TAG = "BoardBottomSheet"
        fun newInstance(): BoardBottomSheet = BoardBottomSheet()
    }
}
