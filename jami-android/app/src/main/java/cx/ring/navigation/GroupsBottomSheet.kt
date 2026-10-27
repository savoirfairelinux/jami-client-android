/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.navigation

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.fragments.ContactPickerFragment
import cx.ring.utils.ActionHelper
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.model.interaction.TextMessage
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import net.jami.smartlist.ConversationItemViewModel
import javax.inject.Inject

@AndroidEntryPoint
class GroupsBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var conversationFacade: ConversationFacade

    private val disposables = CompositeDisposable()
    private var listContainer: LinearLayout? = null

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
            setImageResource(R.drawable.baseline_group_24)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#EC4899"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#26EC4899"))
            }
        }
        headerRow.addView(badgeIcon, LinearLayout.LayoutParams(dp(40), dp(40)).apply {
            marginEnd = dp(12)
        })

        val titleCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        titleCol.addView(TextView(ctx).apply {
            text = getString(R.string.function_groups)
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        })
        titleCol.addView(TextView(ctx).apply {
            text = getString(R.string.group_sheet_subtitle)
            textSize = 12f
            alpha = 0.7f
        })
        headerRow.addView(titleCol, LinearLayout.LayoutParams(0, -2, 1f))

        val createGroupBtn = MaterialButton(ctx).apply {
            text = getString(R.string.circle_create_button)
            isAllCaps = false
            textSize = 12f
            insetTop = 0
            insetBottom = 0
            minWidth = 0
            minHeight = 0
            setPadding(dp(14), 0, dp(14), 0)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EC4899"))
            setTextColor(Color.WHITE)
            cornerRadius = dp(18)
            setOnClickListener {
                val fm = parentFragmentManager
                dismiss()
                ContactPickerFragment().show(fm, ContactPickerFragment.TAG)
            }
        }
        headerRow.addView(createGroupBtn, LinearLayout.LayoutParams(-2, dp(36)))

        root.addView(headerRow, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(16)
        })

        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
        }
        val itemsLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        listContainer = itemsLayout
        scroll.addView(itemsLayout, ViewGroup.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, -2))

        observeGroups()
        return root
    }

    private fun observeGroups() {
        disposables.add(
            accountService.currentAccountSubject
                .switchMap { account ->
                    account.getConversationsSubject().switchMap { conversations ->
                        val groups = conversations.filter { conv ->
                            conv.isSwarmGroup() &&
                                conv.mode.blockingFirst(Conversation.Mode.OneToOne) != Conversation.Mode.Public &&
                                !conv.isFeed()
                        }
                        if (groups.isEmpty()) {
                            Observable.just(emptyList())
                        } else {
                            Observable.combineLatest(
                                groups.map { conv ->
                                    conversationFacade.observeConversation(account, conv, false)
                                        .map { vm -> conv to vm }
                                }
                            ) { array ->
                                @Suppress("UNCHECKED_CAST")
                                array.map { it as Pair<Conversation, ConversationItemViewModel> }
                            }
                        }
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ groupPairs ->
                    renderGroupList(groupPairs)
                }, {})
        )
    }

    private fun renderGroupList(groups: List<Pair<Conversation, ConversationItemViewModel>>) {
        val container = listContainer ?: return
        val ctx = context ?: return
        container.removeAllViews()

        if (groups.isEmpty()) {
            val emptyView = TextView(ctx).apply {
                text = getString(R.string.group_empty, getString(R.string.new_group))
                gravity = Gravity.CENTER
                alpha = 0.65f
                textSize = 14f
                setPadding(dp(16), dp(32), dp(16), dp(32))
            }
            container.addView(emptyView, LinearLayout.LayoutParams(-1, -2))
            return
        }

        val onSurfaceColor = ColorStateList.valueOf(
            MaterialColors.getColor(container, com.google.android.material.R.attr.colorOnSurfaceVariant)
        )
        val selectableBgRes = TypedValue().also {
            ctx.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, it, true)
        }.resourceId

        for ((conv, vm) in groups) {
            val card = MaterialCardView(ctx).apply {
                radius = dp(14).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                strokeColor = Color.parseColor("#33EC4899")
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val homeActivity = activity as? HomeActivity
                    dismiss()
                    homeActivity?.startConversation(conv.accountId, conv.uri)
                }
            }

            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(6), dp(10))
            }

            // Group Avatar
            val avatarView = ImageView(ctx).apply {
                setImageDrawable(
                    AvatarDrawable.Builder()
                        .withViewModel(vm)
                        .withCircleCrop(true)
                        .build(ctx)
                )
            }
            row.addView(avatarView, LinearLayout.LayoutParams(dp(42), dp(42)).apply {
                marginEnd = dp(12)
            })

            // Group title & subtitle (member count + last message)
            val textCol = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
            }
            val groupTitle = vm.title.ifBlank { vm.uriTitle }.ifBlank { conv.uri.rawRingId.take(10) }
            textCol.addView(TextView(ctx).apply {
                text = groupTitle
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })

            val memberCount = conv.contacts.size
            val lastPreview = (conv.lastEvent as? TextMessage)?.body?.takeIf { it.isNotBlank() }
            val members = resources.getQuantityString(R.plurals.group_member_count, memberCount, memberCount)
            val subtitleText = if (lastPreview != null) {
                getString(R.string.group_subtitle_with_preview, members, lastPreview)
            } else {
                members
            }
            textCol.addView(TextView(ctx).apply {
                text = subtitleText
                textSize = 12f
                alpha = 0.75f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            row.addView(textCol, LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginEnd = dp(4)
            })

            // Three vertical dots menu button on the right
            val moreBtn = ImageButton(ctx).apply {
                setImageResource(R.drawable.baseline_more_vert_24)
                imageTintList = onSurfaceColor
                if (selectableBgRes != 0) {
                    setBackgroundResource(selectableBgRes)
                } else {
                    background = null
                }
                contentDescription = getString(R.string.group_options)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                setOnClickListener { anchor ->
                    val popup = PopupMenu(ctx, anchor, Gravity.END)
                    popup.menu.add(0, 1, 0, R.string.swarm_group_action_leave)
                    popup.setOnMenuItemClickListener { item ->
                        if (item.itemId == 1) {
                            ActionHelper.launchDeleteSwarmGroupAction(
                                context = ctx,
                                accountId = conv.accountId,
                                uri = conv.uri,
                                callback = { accountId: String, conversationUri: Uri ->
                                    disposables.add(
                                        conversationFacade.removeConversation(accountId, conversationUri)
                                            .subscribe({}, {})
                                    )
                                }
                            )
                            true
                        } else {
                            false
                        }
                    }
                    popup.show()
                }
            }
            row.addView(moreBtn, LinearLayout.LayoutParams(dp(38), dp(38)))

            card.addView(row)
            container.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(8)
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listContainer = null
        disposables.clear()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        const val TAG = "GroupsBottomSheet"
        fun newInstance() = GroupsBottomSheet()
    }
}
