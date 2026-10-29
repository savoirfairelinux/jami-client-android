/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
package cx.ring.fragments

import android.Manifest
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorInt
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.*
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.adapters.ConversationAdapter
import cx.ring.client.CallActivity
import cx.ring.client.CollabDocuments
import cx.ring.client.ConversationDetailsActivity
import cx.ring.client.ConversationActivity
import cx.ring.client.ConversationDetailsActivity.Companion.EXIT_REASON
import cx.ring.client.ConversationDetailsActivity.Companion.ExitReason
import cx.ring.databinding.FragConversationBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.service.DRingService
import cx.ring.service.LocationSharingService
import cx.ring.services.NotificationServiceImpl
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationColor
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationPreferences
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationSymbol
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getAccountPreferences
import cx.ring.services.SharedPreferencesServiceImpl.Companion.PREF_THREADED_VIEW
import cx.ring.utils.*
import cx.ring.utils.ContentUri.getShareItems
import cx.ring.views.AvatarDrawable
import cx.ring.views.AvatarFactory
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.conversation.ConversationPresenter
import net.jami.conversation.ConversationView
import net.jami.daemon.JamiService
import net.jami.model.*
import net.jami.model.Account.ComposingStatus
import net.jami.model.Call.CallStatus
import net.jami.model.interaction.DataTransfer
import net.jami.model.interaction.Interaction
import net.jami.model.interaction.TextMessage
import net.jami.services.NotificationService
import net.jami.services.PeerServicesService
import net.jami.services.PeerServicesStatus
import net.jami.smartlist.ConversationItemViewModel
import java.io.File
import java.util.*
import androidx.core.content.edit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cx.ring.client.HomeActivity
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class ConversationFragment : BaseSupportFragment<ConversationPresenter, ConversationView>(),
    ConversationView, SearchView.OnQueryTextListener {
    private var locationServiceConnection: ServiceConnection? = null
    private var binding: FragConversationBinding? = null
    private var currentBottomView: View? = null
    private var mAdapter: ConversationAdapter? = null
    private var mSearchAdapter: ConversationAdapter? = null
    private var threadAdapter: ConversationAdapter? = null
    private var threadState = ConversationThreadState()
    private var accountPreferences: SharedPreferences? = null
    private val threadPreferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_THREADED_VIEW) applyThreadPreference()
        }
    private var threads: ConversationThreads? = null
    private var restoredThreadRoot: String? = null
    private var threadRenderJob: Disposable? = null
    private var threadScreenAnimation: Animation? = null
    private var pendingThreadSlide: Boolean? = null
    private var threadScreenOnShown: (() -> Unit)? = null
    private var threadScreenGeneration = 0
    private val changedThreadMessages = HashSet<String>()
    private val newThreadMessages = HashSet<String>()
    private var forceThreadRefresh = false
    private var pendingThreadScroll: String? = null
    private var pendingThreadOffset = 0
    private var pendingThreadHighlight = false
    private var mainScrollMessage: String? = null
    private var mainScrollOffset = 0
    private var requestedThreadRoot: String? = null
    private val threadNavigation = ConversationThreadNavigation()
    private var threadNavigationSnackbar: Snackbar? = null
    private var historyRequestInFlight = false
    private var historyExhausted = false
    private var viewGeneration = 0
    private var attachmentReplyTarget: String? = null
    private var audioReplyTarget: String? = null
    private var composingStatus = ComposingStatus.Idle
    private val renderThread = Runnable { renderThreadHistory() }
    private val threadBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (threadScreenAnimation != null) {
                cancelThreadScreenTransition()
                updateThreadHeader()
            } else if (threadNavigation.target != null)
                cancelThreadNavigation()
            else if (binding?.toolbar?.menu?.findItem(R.id.conv_search)?.isActionViewExpanded == true)
                binding?.toolbar?.menu?.findItem(R.id.conv_search)?.collapseActionView()
            else closeThread()
        }
    }
    private val visibleAdapter: ConversationAdapter?
        get() = if (threadState.enabled) threadAdapter else mAdapter
    private val animation = ValueAnimator()
    private var mPreferences: SharedPreferences? = null
    private var mCurrentPhoto: File? = null
    private var mCurrentFileAbsolutePath: String? = null
    private val mCompositeDisposable = CompositeDisposable()
    private var replyingTo: Interaction? = null
    private var mIsBubble = false
    private val mParticipantAvatars: MutableMap<String, AvatarDrawable> = HashMap()
    private val mSmallParticipantAvatars: MutableMap<String, AvatarDrawable> = HashMap()
    private var mapWidth = 0
    private var mapHeight = 0
    private var marginPxTotal = 0
    private var loading = true
    private var accountOffline = false
    private var animating = 0
    private var lastInsets: WindowInsetsCompat? = null
    private var mConversationItem: ConversationItemViewModel? = null
    private var mPeerServicesCheckDisposable: Disposable? = null

    private var inlineAudioRecorder: InlineAudioRecorder? = null

    @Inject lateinit var peerServicesService: PeerServicesService
    @Named("UiScheduler") @Inject lateinit var uiScheduler: Scheduler

    private fun isImeAnimation(a: WindowInsetsAnimationCompat): Boolean {
        return (a.typeMask and WindowInsetsCompat.Type.ime()) != 0
    }

    private fun updatePaddings(windowInsets: WindowInsetsCompat? = null) {
        val root = view ?: return
        val actualInsets = windowInsets ?: ViewCompat.getRootWindowInsets(root) ?: lastInsets ?: return
        val insets = actualInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
        binding?.apply {
            if (errorMsgPane.isVisible) {
                errorMsgPane.updatePadding(top = insets.top)
                appbar.updatePadding(top = 0)
            } else {
                errorMsgPane.updatePadding(top = 0)
                appbar.updatePadding(top = insets.top)
            }

            mainContainer.updatePadding(bottom = insets.bottom)
        }
    }

    private var swipeHelper: ItemTouchHelper? = null

    private val pickMultipleMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)) { uris ->
            val replyTo = attachmentReplyTarget
            for (uri in uris) {
                startFileSend(AndroidFileUtils.getCacheFile(requireContext(), uri)
                    .observeOn(DeviceUtils.uiScheduler)
                    .flatMapCompletable { file: File -> sendFile(file, replyTo) })
            }
        }

    fun getConversationAvatar(uri: String): AvatarDrawable? = mParticipantAvatars[uri]

    private fun outgoingReplyTarget(): String? =
        threadState.replyTarget(replyingTo?.messageId)

    private fun currentThreadRoot(): Interaction? =
        threadState.rootId?.let { root ->
            mAdapter?.history?.firstOrNull {
                it.messageId == root && it.type != Interaction.InteractionType.INVALID
            }
        }

    private fun canSendThreadReply(): Boolean {
        if (threadState.rootId == null || currentThreadRoot() != null) return true
        Toast.makeText(context, R.string.conversation_thread_missing, Toast.LENGTH_LONG).show()
        return false
    }

    private fun restoreComposerDraft() {
        replyingTo = null
        binding?.replyGroup?.isVisible = false
        binding?.msgInputTxt?.setText(mPreferences?.getString(threadState.draftKey, null))
        restoreComposerReply()
    }

    private fun restoreComposerReply() {
        if (replyingTo != null) return
        val id = mPreferences?.getString(threadState.replyDraftKey, null) ?: return
        mAdapter?.history?.firstOrNull { it.messageId == id }?.let {
            showReplyPreview(it, focus = false)
        }
    }

    private fun rememberMainScroll() {
        if (threadState.rootId != null) return
        val list = binding?.histList ?: return
        val manager = list.layoutManager as? LinearLayoutManager ?: return
        val position = manager.findFirstVisibleItemPosition()
        mainScrollMessage = visibleAdapter?.getMessageFromPosition(position)?.messageId
        mainScrollOffset = manager.findViewByPosition(position)?.top?.minus(list.paddingTop) ?: 0
    }

    private fun restoreScrollAnchor() {
        val id = pendingThreadScroll ?: return
        val list = binding?.histList ?: return
        val adapter = visibleAdapter ?: return
        var position = adapter.getMessagePosition(id)
        if (position == -1 && threadState.enabled && threadState.rootId == null)
            threads?.rootId(id)?.let { position = adapter.getMessagePosition(it) }
        if (position == -1) return
        (list.layoutManager as? LinearLayoutManager)
            ?.scrollToPositionWithOffset(position, pendingThreadOffset)
        if (pendingThreadHighlight) {
            val highlightedPosition = position
            list.doOnNextLayout {
                list.layoutManager?.findViewByPosition(highlightedPosition)
                    ?.setBackgroundColor(requireContext().getColor(R.color.surface))
            }
        }
        pendingThreadScroll = null
        pendingThreadOffset = 0
        pendingThreadHighlight = false
    }

    private fun createThreadSlide(page: View, forward: Boolean, enter: Boolean): Animation {
        val movingRight = forward == (page.layoutDirection == View.LAYOUT_DIRECTION_RTL)
        val slide = if (enter) AnimationUtils.makeInAnimation(page.context, movingRight)
            else AnimationUtils.makeOutAnimation(page.context, movingRight)
        slide.duration = page.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        return slide
    }

    private fun transitionThreadScreen(forward: Boolean, change: () -> Unit) {
        val page = binding?.root ?: return
        cancelThreadScreenTransition(showPage = false)
        if (page.isInvisible) pendingThreadSlide = forward
        if (!page.isLaidOut || !page.isShown || !ValueAnimator.areAnimatorsEnabled()) {
            change()
            return
        }
        val slide = createThreadSlide(page, forward, enter = false)
        threadScreenAnimation = slide
        slide.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                if (threadScreenAnimation !== animation || binding?.root !== page) return
                threadScreenAnimation = null
                page.isInvisible = true
                pendingThreadSlide = forward
                change()
            }
        })
        threadBack.isEnabled = true
        page.startAnimation(slide)
    }

    private fun showThreadScreen() {
        if (threadScreenAnimation != null) return
        val page = binding?.root ?: return
        val forward = pendingThreadSlide
        val onShown = threadScreenOnShown
        if (forward == null && onShown == null) return
        val generation = threadScreenGeneration
        pendingThreadSlide = null
        threadScreenOnShown = null
        // Wait for RecyclerView to lay out the destination, not just update its adapter.
        page.doOnPreDraw {
            if (binding?.root !== page || generation != threadScreenGeneration) return@doOnPreDraw
            binding?.histList?.itemAnimator?.endAnimations()
            page.isVisible = true
            if (forward != null && ValueAnimator.areAnimatorsEnabled())
                page.startAnimation(createThreadSlide(page, forward, enter = true))
            onShown?.invoke()
        }
    }

    private fun cancelThreadScreenTransition(showPage: Boolean = true) {
        threadScreenGeneration++
        threadScreenAnimation = null
        pendingThreadSlide = null
        threadScreenOnShown = null
        binding?.root?.apply {
            clearAnimation()
            if (showPage) isVisible = true
        }
    }

    fun openThread(rootId: String, onOpened: (() -> Unit)? = null) {
        if (!threadState.enabled || threadState.rootId == rootId) return
        rememberMainScroll()
        transitionThreadScreen(forward = true) {
            threadState.openThread(rootId)
            restoreComposerDraft()
            pendingThreadScroll = null
            pendingThreadOffset = 0
            pendingThreadHighlight = false
            forceThreadRefresh = true
            updateThreadHeader()
            onOpened?.invoke()
            scheduleThreadRender()
        }
    }

    private fun closeThread(onClosed: (() -> Unit)? = null) {
        cancelThreadNavigation()
        transitionThreadScreen(forward = false) {
            threadState.closeThread()
            restoreComposerDraft()
            pendingThreadScroll = mainScrollMessage
            pendingThreadOffset = mainScrollOffset
            pendingThreadHighlight = false
            forceThreadRefresh = true
            updateThreadHeader()
            onClosed?.invoke()
            scheduleThreadRender()
        }
    }

    private fun setThreadingEnabled(enabled: Boolean) {
        cancelThreadScreenTransition()
        rememberMainScroll()
        cancelThreadNavigation()
        threadRenderJob?.dispose()
        threadState.enable(enabled)
        restoreComposerDraft()
        pendingThreadScroll = mainScrollMessage
        pendingThreadOffset = mainScrollOffset
        pendingThreadHighlight = false
        binding?.histList?.adapter = visibleAdapter
        updateThreadHeader()
        if (enabled) {
            forceThreadRefresh = true
            scheduleThreadRender()
        } else {
            changedThreadMessages.clear()
            newThreadMessages.clear()
            binding?.histList?.removeCallbacks(renderThread)
            restoreScrollAnchor()
        }
    }

    private fun updateThreadHeader() {
        val binding = binding ?: return
        val root = threadState.rootId
        if (binding.threadBar.isVisible != (root != null)) {
            val transition = binding.appbar.layoutTransition
            binding.appbar.layoutTransition = null
            binding.threadBar.isVisible = root != null
            binding.toolbar.isVisible = root == null
            binding.appbar.layoutTransition = transition
        }
        binding.threadConversationName.text = binding.contactTitle.text
        updateThreadNavigationFeedback()
        val canCompose = !accountOffline && (root == null || currentThreadRoot() != null)
        binding.msgInputTxt.isEnabled = canCompose
        binding.msgInputTxt.setHint(
            if (root == null) R.string.write_a_message else R.string.conversation_thread_reply_hint
        )
        binding.msgSend.isEnabled = canCompose
        binding.emojiSend.isEnabled = canCompose
        binding.btnMenu.isEnabled = canCompose
        binding.btnTakePicture.isEnabled = canCompose
        binding.btnAudioRecord.isEnabled = canCompose
        threadBack.isEnabled = root != null || threadNavigation.target != null ||
            threadScreenAnimation != null
    }

    private fun updateThreadNavigationFeedback() {
        val binding = binding ?: return
        if (threadNavigation.target != null) {
            if (threadNavigationSnackbar != null) return
            threadNavigationSnackbar = Snackbar.make(
                binding.root, R.string.conversation_thread_finding, Snackbar.LENGTH_INDEFINITE
            ).setAnchorView(binding.cvMessageInput)
                .setAction(android.R.string.cancel) { cancelThreadNavigation() }
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(snackbar: Snackbar, event: Int) {
                        if (threadNavigationSnackbar === snackbar) {
                            threadNavigationSnackbar = null
                            cancelThreadNavigation()
                        }
                    }
                })
                .also { it.show() }
        } else {
            val snackbar = threadNavigationSnackbar
            threadNavigationSnackbar = null
            snackbar?.dismiss()
            if (threadNavigation.failed) {
                threadNavigation.cancel()
                Toast.makeText(context, R.string.conversation_thread_unavailable, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadEarlierMessages() {
        if (loading || historyRequestInFlight || historyExhausted ||
            threadState.rootId != null || threadNavigation.target != null
        ) return
        val knownIds = mAdapter?.history.orEmpty().map { it.messageId }.toSet()
        historyRequestInFlight = true
        loading = true
        val generation = viewGeneration
        presenter.loadMore {
            if (generation != viewGeneration || binding == null) return@loadMore
            historyRequestInFlight = false
            historyExhausted = mAdapter?.history.orEmpty().all { it.messageId in knownIds }
            loading = false
            if (threadNavigation.target != null) continueThreadNavigation()
            else if (threadState.enabled) scheduleThreadRender()
        }
    }

    private fun applyThreadPreference() {
        if (binding == null) return
        val enabled = ConversationPath.fromBundle(arguments)?.conversationUri?.isSwarm == true &&
            accountPreferences?.getBoolean(PREF_THREADED_VIEW, false) == true
        if (threadState.enabled != enabled) setThreadingEnabled(enabled)
    }

    fun revealThreadMessage(messageId: String) {
        threadNavigation.start(messageId)
        continueThreadNavigation()
    }

    private fun cancelThreadNavigation() {
        threadNavigation.cancel()
        updateThreadHeader()
    }

    private fun continueThreadNavigation() {
        val target = threadNavigation.target ?: return
        if (binding == null || !threadState.enabled) return
        val history = mAdapter?.history.orEmpty()
        if (ConversationThreads(history).rootId(target) != null) {
            threadNavigation.cancel()
            scrollToMessage(target, true)
            updateThreadHeader()
            return
        }
        updateThreadHeader()
        if (historyRequestInFlight) return
        val request = threadNavigation.begin(history.map { it.messageId }.toSet()) ?: return
        historyRequestInFlight = true
        val generation = viewGeneration
        presenter.loadMore {
            if (generation != viewGeneration || binding == null) return@loadMore
            historyRequestInFlight = false
            loading = false
            val loaded = mAdapter?.history.orEmpty()
            historyExhausted = loaded.all { it.messageId in request.loadedIds }
            val resolved = threadNavigation.target?.let {
                ConversationThreads(loaded).rootId(it) != null
            } ?: false
            if (threadNavigation.complete(request, loaded.map { it.messageId }.toSet(), resolved))
                binding?.histList?.post { continueThreadNavigation() }
            else updateThreadHeader()
        }
    }

    private fun scheduleThreadRender(changedId: String? = null) {
        if (!threadState.enabled) return
        changedId?.let { changedThreadMessages.add(it) }
        threadRenderJob?.dispose()
        binding?.histList?.apply {
            removeCallbacks(renderThread)
            postOnAnimation(renderThread)
        }
    }

    private fun renderThreadHistory() {
        if (!threadState.enabled) return
        val history = mAdapter?.history ?: return
        val root = threadState.rootId
        val changed = changedThreadMessages.toSet()
        val added = newThreadMessages.toSet()
        val force = forceThreadRefresh
        threadRenderJob = Single.fromCallable { ConversationThreads(history) }
            .subscribeOn(Schedulers.computation())
            .observeOn(uiScheduler)
            .subscribe({ projection ->
                val list = binding?.histList ?: return@subscribe
                val adapter = threadAdapter ?: return@subscribe
                if (!threadState.enabled || threadState.rootId != root) return@subscribe
                val messages = if (root == null) projection.timeline
                    else projection.messages(root)
                val oldIds = adapter.history.map { it.messageId }.toSet()
                val incoming = messages.filter {
                    it.messageId in added && it.messageId !in oldIds
                }
                val manager = list.layoutManager as? LinearLayoutManager
                val nearEnd = (manager?.findLastVisibleItemPosition() ?: 0) >=
                    adapter.itemCount - 3
                val counts = if (root == null) projection.timeline.mapNotNull { message ->
                    message.messageId?.let { it to projection.replyCount(it) }
                }.toMap() else emptyMap()
                adapter.threadRootId = root
                adapter.updateProjection(
                    messages, counts, changed, force,
                    if (root == null) composingStatus else ComposingStatus.Idle
                )
                threads = projection
                changedThreadMessages.removeAll(changed)
                newThreadMessages.removeAll(added)
                forceThreadRefresh = false
                restoreComposerReply()
                updateThreadHeader()
                val showLatest = threadState.consumeScrollToLatest(pendingThreadScroll)
                if (pendingThreadScroll != null) restoreScrollAnchor()
                else if (showLatest) scrollToEnd()
                else if (incoming.isNotEmpty() &&
                    (nearEnd || incoming.any { !it.isIncoming })
                ) {
                    scrollToEnd()
                    if (incoming.any { it.isIncoming })
                        announceIncomingMessageForAccessibility()
                }
                showThreadScreen()
            }, { error ->
                cancelThreadScreenTransition()
                Log.e(TAG, "Unable to display conversation threads", error)
                displayErrorToast(Error.GENERIC_ERROR)
            })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val list = binding?.histList
        val manager = list?.layoutManager as? LinearLayoutManager
        val position = manager?.findFirstVisibleItemPosition() ?: -1
        outState.putString(STATE_THREAD_ROOT, threadState.rootId)
        outState.putString(
            STATE_SCROLL_MESSAGE,
            visibleAdapter?.getMessageFromPosition(position)?.messageId
        )
        outState.putInt(
            STATE_SCROLL_OFFSET,
            manager?.findViewByPosition(position)?.top?.minus(list?.paddingTop ?: 0) ?: 0
        )
        outState.putString(STATE_MAIN_SCROLL_MESSAGE, mainScrollMessage)
        outState.putInt(STATE_MAIN_SCROLL_OFFSET, mainScrollOffset)
        outState.putString(STATE_ATTACHMENT_REPLY, attachmentReplyTarget)
        outState.putString(STATE_AUDIO_REPLY, audioReplyTarget)
        outState.putString(STATE_PHOTO, mCurrentPhoto?.absolutePath)
        super.onSaveInstanceState(outState)
    }

    override fun refreshView(conversation: List<Interaction>) {
        if (binding != null) binding!!.pbLoading.visibility = View.GONE
        mAdapter?.let { adapter ->
            adapter.updateDataset(conversation.toMutableList())
            historyExhausted = false
            loading = false
            if (threadState.enabled) {
                forceThreadRefresh = true
                scheduleThreadRender()
                val root = threadState.rootId
                if (root != null && conversation.none { it.messageId == root } &&
                    requestedThreadRoot != root
                ) {
                    requestedThreadRoot = root
                    revealThreadMessage(root)
                }
            }
            binding?.histList?.doOnNextLayout {
                it.animate()
                    .alpha(1f)
                    .setDuration(220)
                    .start()
            }
        }
    }

    override fun scrollToEnd() {
        visibleAdapter?.let { adapter ->
            if (adapter.itemCount > 0)
                binding?.histList?.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun announceIncomingMessageForAccessibility() {
        val recyclerView = binding?.histList ?: return
        val am = requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        if (am?.isTouchExplorationEnabled != true) return
        recyclerView.doOnNextLayout {
            val lastPosition = (visibleAdapter?.itemCount ?: 0) - 1
            if (lastPosition < 0) return@doOnNextLayout
            recyclerView.layoutManager?.findViewByPosition(lastPosition)
                ?.performAccessibilityAction(
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null
                )
        }
    }

    override fun scrollToMessage(messageId: String, highlight: Boolean) {
        val histList = binding?.histList ?: return
        if (threadState.enabled) {
            val projection = ConversationThreads(mAdapter?.history.orEmpty())
            val root = projection.rootId(messageId)
            if (root != null && root != threadState.rootId &&
                (root != messageId || threadState.rootId != null)
            ) {
                openThread(root) { scrollToMessage(messageId, highlight) }
                return
            }
            if (root == null && threadState.rootId != null) {
                closeThread { scrollToMessage(messageId, highlight) }
                return
            }
            if (messageId != requestedThreadRoot || pendingThreadScroll == null)
                pendingThreadScroll = messageId
            pendingThreadHighlight = highlight
            scheduleThreadRender()
            return
        }
        visibleAdapter?.let { adapter ->
            val position = adapter.getMessagePosition(messageId)
            if (position == -1)
                return

            histList.scrollToPosition(position)

            if (highlight) {
                histList.doOnNextLayout {
                    histList.layoutManager?.findViewByPosition(position)
                        ?.setBackgroundColor(requireContext().getColor(R.color.surface))
                }
            }
        }
    }

    override fun displayErrorToast(error: Error) {
        val errorString: String = when (error) {
            Error.NO_INPUT -> getString(R.string.call_error_no_camera_no_microphone)
            Error.INVALID_FILE -> getString(R.string.invalid_file)
            Error.NOT_ABLE_TO_WRITE_FILE -> getString(R.string.not_able_to_write_file)
            Error.NO_SPACE_LEFT -> getString(R.string.no_space_left_on_device)
            else -> getString(R.string.generic_error)
        }
        Toast.makeText(requireContext(), errorString, Toast.LENGTH_LONG).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        restoredThreadRoot = savedInstanceState?.getString(STATE_THREAD_ROOT)
            ?: threadState.rootId
        pendingThreadScroll = savedInstanceState?.getString(STATE_SCROLL_MESSAGE)
        pendingThreadOffset = savedInstanceState?.getInt(STATE_SCROLL_OFFSET) ?: 0
        mainScrollMessage = savedInstanceState?.getString(STATE_MAIN_SCROLL_MESSAGE)
        mainScrollOffset = savedInstanceState?.getInt(STATE_MAIN_SCROLL_OFFSET) ?: 0
        attachmentReplyTarget = savedInstanceState?.getString(STATE_ATTACHMENT_REPLY)
        audioReplyTarget = savedInstanceState?.getString(STATE_AUDIO_REPLY)
        mCurrentPhoto = savedInstanceState?.getString(STATE_PHOTO)?.let { File(it) }
        val res = resources
        mapWidth = res.getDimensionPixelSize(R.dimen.location_sharing_minmap_width)
        mapHeight = res.getDimensionPixelSize(R.dimen.location_sharing_minmap_height)
        marginPxTotal = res.getDimensionPixelSize(R.dimen.conversation_message_input_margin)

        return FragConversationBinding.inflate(inflater, container, false).apply {
            currentBottomView = cvMessageInput
            animation.duration = 150
            animation.addUpdateListener { valueAnimator: ValueAnimator ->
                histList.updatePadding(bottom = valueAnimator.animatedValue as Int)
            }

            ViewCompat.setWindowInsetsAnimationCallback(
                mainContainer,
                object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                        if (isImeAnimation(animation)) {
                            animating++
                        }
                    }

                    override fun onProgress(
                        insets: WindowInsetsCompat,
                        runningAnimations: List<WindowInsetsAnimationCompat>
                    ): WindowInsetsCompat {
                        val windowInsets = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
                        )
                        mainContainer.updatePadding(bottom = windowInsets.bottom)
                        return insets
                    }

                    override fun onEnd(animation: WindowInsetsAnimationCompat) {
                        if (isImeAnimation(animation)) {
                            animating = (animating - 1).coerceAtLeast(0)
                        }
                    }
                })

            ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
                lastInsets = windowInsets
                if (animating == 0) {
                    updatePaddings(windowInsets)
                }
                WindowInsetsCompat.CONSUMED
            }

            // Content may be both text and non-text (HTML, images, videos, audio files, etc).
            ViewCompat.setOnReceiveContentListener(msgInputTxt, SUPPORTED_MIME_TYPES) { _, payload ->
                // Split the incoming content into two groups: content URIs and everything else.
                // This way we can implement custom handling for URIs and delegate the rest.
                val split = payload.partition { item -> item.uri != null }
                val uriContent = split.first
                val remaining = split.second

                // Handles content URIs.
                if (uriContent != null) {
                    val replyTo = outgoingReplyTarget()
                    val clip = uriContent.clip
                    for (i in 0 until clip.itemCount) {
                        val uri = clip.getItemAt(i).uri
                        startFileSend(AndroidFileUtils.getCacheFile(requireContext(), uri)
                            .flatMapCompletable { sendFile(it, replyTo) })
                    }
                }
                // Delegates the processing for text and everything else to the platform.
                remaining
            }

            msgInputTxt.setOnEditorActionListener { _, actionId: Int, _ -> actionSendMsgText(actionId) }
            msgInputTxt.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus: Boolean ->
                if (hasFocus) {
                    (childFragmentManager.findFragmentById(R.id.mapLayout) as? LocationSharingFragment)?.hideControls()
                }
            }

            msgInputTxt.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    val message = s.toString()
                    val hasMessage = !TextUtils.isEmpty(message)
                    presenter.onComposingChanged(hasMessage)
                    if (hasMessage) {
                        msgSend.visibility = View.VISIBLE
                        emojiSend.visibility = View.GONE
                    } else {
                        msgSend.visibility = View.GONE
                        emojiSend.visibility = View.VISIBLE
                    }
                    mPreferences?.let { preferences ->
                        if (hasMessage)
                            preferences.edit { putString(threadState.draftKey, message) }
                        else preferences.edit { remove(threadState.draftKey) }
                    }
                }
            })

            msgInputTxt.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                if (oldBottom == 0 && oldTop == 0) {
                    // set initial list padding
                    histList.updatePadding(bottom = (currentBottomView?.height ?: 0) + marginPxTotal)
                } else {
                    if (animation.isStarted) animation.cancel()
                    animation.setIntValues(
                        histList.paddingBottom,
                        (currentBottomView?.height ?: 0) + marginPxTotal
                    )
                    animation.start()
                }
            }

            replyCloseBtn.setOnClickListener { clearReply() }
            fabLatest.setOnClickListener { scrollToEnd() }

            toolbar.setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            toolbar.setOnClickListener { presenter.openContact() }
            toolbar.addMenuProvider(menuProvider)

            ongoingCallPane.setOnClickListener { presenter.clickOnGoingPane() }
            msgSend.setOnClickListener { sendMessageText() }
            setupInlineAudioRecorder(this)
            emojiSend.setOnClickListener { sendEmoji() }
            btnMenu.setOnClickListener { expandMenu(it) }
            btnTakePicture.setOnClickListener {
                attachmentReplyTarget = outgoingReplyTarget()
                takePicture()
            }
            threadClose.setOnClickListener { closeThread() }
            unknownContactButton.setOnClickListener { presenter.onAddContact() }
            btnBlock.setOnClickListener { presenter.onBlockIncomingContactRequest() }
            btnRefuse.setOnClickListener { presenter.onRefuseIncomingContactRequest() }
            btnAccept.setOnClickListener { presenter.onAcceptIncomingContactRequest() }

            binding = this
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, threadBack)
        // Listen for the audio recorder's result here so it survives the dialog being recreated
        // (e.g. on a theme or orientation change).
        childFragmentManager.setFragmentResultListener(
            AudioMessageRecorderFragment.REQUEST_KEY, viewLifecycleOwner
        ) { _, result ->
            val path = result.getString(AudioMessageRecorderFragment.RESULT_FILE_PATH) ?: return@setFragmentResultListener
            val file = File(path)
            if (file.exists() && file.length() > 0L) {
                val replyTo = audioReplyTarget
                startFileSend(Single.just(file).flatMapCompletable { f ->
                    sendFile(f, replyTo)
                })
            }
        }
        binding?.apply {
            ViewCompat.setAccessibilityHeading(threadTitle, true)
            restoreComposerDraft()
            updateThreadHeader()

            histList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                // The minimum amount of items to have below current scroll position before loading more.
                val visibleLoadThreshold = 3
                // The amount of items to have below the current scroll position to display
                // the scroll to latest button.
                val visibleLatestThreshold = 8
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {}
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    if (threadState.rootId == null && histList.adapter != mSearchAdapter
                        && layoutManager.findFirstVisibleItemPosition() < visibleLoadThreshold
                    ) {
                        if (dy < 0) historyExhausted = false
                        loadEarlierMessages()
                    }

                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    if (layoutManager.itemCount - lastVisibleItemPosition > visibleLatestThreshold)
                        fabLatest.show()
                    else fabLatest.hide()
                }
            })

            (histList.itemAnimator as? DefaultItemAnimator)?.supportsChangeAnimations = false
            histList.adapter = visibleAdapter
            searchList.adapter = mSearchAdapter

            val messageSwipeController =
                ActionHelper.MessageSwipeController(
                    context = requireContext(),
                    onSwipe = { position: Int ->
                        visibleAdapter?.getMessageFromPosition(position)?.let {
                            presenter.startReplyTo(it)
                        }
                    }
                )
            swipeHelper = ItemTouchHelper(messageSwipeController).apply {
                attachToRecyclerView(histList)
            }
        }
    }

    override fun setConversationColor(@ColorInt color: Int) {
        mAdapter?.convColor = getConversationColor(requireContext(), color)
        mSearchAdapter?.convColor = getConversationColor(requireContext(), color)
        threadAdapter?.convColor = getConversationColor(requireContext(), color)
    }

    override fun setConversationSymbol(symbol: CharSequence) {
        binding?.emojiSend?.text = getConversationSymbol(requireContext(), symbol)
    }

    override fun onDestroyView() {
        cancelThreadScreenTransition()
        viewGeneration++
        threadNavigation.reset()
        threadState.consumeScrollToLatest(null)
        val snackbar = threadNavigationSnackbar
        threadNavigationSnackbar = null
        snackbar?.dismiss()
        historyRequestInFlight = false
        historyExhausted = false
        requestedThreadRoot = null
        binding?.histList?.removeCallbacks(renderThread)
        threadRenderJob?.dispose()
        threadRenderJob = null
        threads = null
        changedThreadMessages.clear()
        newThreadMessages.clear()
        animation.removeAllUpdateListeners()
        binding?.histList?.adapter = null
        mCompositeDisposable.clear()
        inlineAudioRecorder?.release()
        inlineAudioRecorder = null
        locationServiceConnection?.let {
            try {
                requireContext().unbindService(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error unbinding service: " + e.message)
            }
        }
        mAdapter = null
        threadAdapter = null
        super.onDestroyView()
        binding = null
    }

    private fun clearReply() {
        mPreferences?.edit { remove(threadState.replyDraftKey) }
        if (replyingTo != null) {
            replyingTo = null
            binding?.apply {
                replyGroup.isVisible = false
            }
        }
    }

    fun sendMessageText() {
        if (!canSendThreadReply()) return
        val message = binding!!.msgInputTxt.text.toString()
        presenter.sendTextMessage(message, replyingTo ?: currentThreadRoot())
        clearMsgEdit()
    }

    fun sendEmoji() {
        if (!canSendThreadReply()) return
        presenter.sendTextMessage(
            binding!!.emojiSend.text.toString(), replyingTo ?: currentThreadRoot()
        )
        clearReply()
    }

    private fun expandMenu(v: View) {
        if (!canSendThreadReply()) return
        val popup = PopupMenu(requireContext(), v)
        popup.inflate(R.menu.conversation_share_actions)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            attachmentReplyTarget = outgoingReplyTarget()
            when (item.itemId) {
                //R.id.conv_send_audio -> sendAudioMessage()
                R.id.conv_send_video -> sendVideoMessage()
                R.id.conv_send_file -> openFilePicker()
                R.id.conv_select_media -> openGallery()
                R.id.conv_share_location -> shareLocation()
                R.id.conv_new_document -> newCollabDocument()
                R.id.chat_extensions -> presenter.showExtensionListHandlers()
            }
            false
        }
        popup.menu.findItem(R.id.chat_extensions).isVisible = JamiService.getPluginsEnabled() && !JamiService.getChatHandlers().isEmpty()
        if (threadState.rootId != null) {
            popup.menu.findItem(R.id.conv_share_location).isVisible = false
            popup.menu.findItem(R.id.conv_new_document).isVisible = false
            popup.menu.findItem(R.id.chat_extensions).isVisible = false
        }
        popup.setForceShowIcon(true)
        popup.show()
    }

    private fun newCollabDocument() {
        val path = ConversationPath.fromBundle(arguments) ?: return
        CollabDocuments.promptNew(
            requireContext(), path, presenter.collaborationService, mCompositeDisposable
        )
    }

    override fun showExtensionListHandlers(accountId: String, contactId: String) {
        Log.w(TAG, "show Extension Chat Handlers List")
        val fragment = ExtensionHandlersListFragment.newInstance(accountId, contactId)
        childFragmentManager.beginTransaction()
            .add(R.id.extensionListHandlers, fragment, ExtensionHandlersListFragment.TAG)
            .commit()
        binding?.let { binding ->
            val params = binding.mapCard.layoutParams as RelativeLayout.LayoutParams
            if (params.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                binding.mapCard.layoutParams = params
            }
            binding.mapCard.visibility = View.VISIBLE
        }
    }

    fun hideExtensionListHandlers() {
        if (binding!!.mapCard.visibility != View.GONE) {
            binding!!.mapCard.visibility = View.GONE
            val fragmentManager = childFragmentManager
            val fragment = fragmentManager.findFragmentById(R.id.extensionListHandlers)
            if (fragment != null) {
                fragmentManager.beginTransaction()
                    .remove(fragment)
                    .commit()
            }
        }
        val params = binding!!.mapCard.layoutParams as RelativeLayout.LayoutParams
        if (params.width != mapWidth) {
            params.width = mapWidth
            params.height = mapHeight
            binding!!.mapCard.layoutParams = params
        }
    }

    private fun shareLocation() {
        presenter.shareLocation()
    }

    fun closeLocationSharing(isSharing: Boolean) {
        val params = binding!!.mapCard.layoutParams as RelativeLayout.LayoutParams
        if (params.width != mapWidth) {
            params.width = mapWidth
            params.height = mapHeight
            params.updateMargins(top = resources.getDimensionPixelSize(R.dimen.location_sharing_minmap_margin))
            binding!!.mapCard.layoutParams = params
        }
        if (!isSharing) hideMap()
    }

    fun expandMapView() {
        // The binding.root view must have android:animateLayoutChanges="true"
        binding!!.root.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        val margin = resources.getDimensionPixelSize(R.dimen.location_sharing_minmap_margin)
        val params = binding!!.mapCard.layoutParams as RelativeLayout.LayoutParams
        params.setMargins(0, 0, 0, binding!!.cvMessageInput.height + margin)
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        binding!!.mapCard.layoutParams = params
    }

    override fun startShareLocation(accountId: String, conversationId: String) {
        showMap(accountId, conversationId, true)
    }

    override fun showMap(accountId: String, contactId: String, open: Boolean) {
        if (binding!!.mapCard.visibility == View.GONE) {
            expandMapView()

            val fragment = LocationSharingFragment.newInstance(accountId, contactId, open)
            childFragmentManager.beginTransaction()
                .add(R.id.mapLayout, fragment, "map")
                .commit()
            binding!!.mapCard.visibility = View.VISIBLE
        }
        if (open) {
            val fragment = childFragmentManager.findFragmentById(R.id.mapLayout)
            (fragment as? LocationSharingFragment)?.showControls()
        }
    }

    override fun hideMap() {
        if (binding!!.mapCard.visibility != View.GONE) {
            binding!!.mapCard.visibility = View.GONE
            val fragmentManager = childFragmentManager
            val fragment = fragmentManager.findFragmentById(R.id.mapLayout)
            if (fragment != null) {
                fragmentManager.beginTransaction()
                    .remove(fragment)
                    .commit()
            }
        }
    }

    private fun sendAudioMessage() {
        if (!canSendThreadReply()) return
        if (!presenter.deviceRuntimeService.hasAudioPermission()) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE_CAPTURE_AUDIO)
        } else {
            showAudioRecorder()
        }
    }

    private fun setupInlineAudioRecorder(binding: FragConversationBinding) {
        val recorder = InlineAudioRecorder(
            context = requireContext(),
            overlay = binding.audioRecordOverlay,
            maxDurationMs = MAX_AUDIO_DURATION_MS.toLong(),
            maxFileSize = DEFAULT_AUDIO_MAX_SIZE,
            callbacks = object : InlineAudioRecorder.Callbacks {
                override fun hasAudioPermission() = presenter.deviceRuntimeService.hasAudioPermission()

                override fun onStart() {
                    audioReplyTarget = outgoingReplyTarget()
                }

                override fun onSimpleTap() = sendAudioMessage()

                override fun onSend(file: File) {
                    val replyTo = audioReplyTarget
                    startFileSend(Single.just(file).flatMapCompletable { f ->
                        sendFile(f, replyTo)
                    })
                }

                override fun onReview(file: File, amplitudes: FloatArray, continueRecording: Boolean) {
                    showAudioRecorder(file, amplitudes, continueRecording)
                }
            }
        )
        recorder.attach(binding.btnAudioRecord)
        inlineAudioRecorder = recorder
    }

    private fun showAudioRecorder(
        initialSegment: File? = null,
        initialAmplitudes: FloatArray? = null,
        continueRecording: Boolean = false,
    ) {
        if (childFragmentManager.findFragmentByTag(AUDIO_RECORDER_TAG) != null) return
        AudioMessageRecorderFragment.newInstance(
            maxDurationMs = MAX_AUDIO_DURATION_MS.toLong(),
            maxFileSize = DEFAULT_AUDIO_MAX_SIZE,
            initialSegment = initialSegment,
            initialAmplitudes = initialAmplitudes,
            continueRecording = continueRecording,
        ).show(childFragmentManager, AUDIO_RECORDER_TAG)
    }

    private fun sendVideoMessage() {
        if (!presenter.deviceRuntimeService.hasVideoPermission()) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_CAPTURE_VIDEO)
        } else {
            try {
                val context = requireContext()
                val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                    putExtra("android.intent.extras.CAMERA_FACING", 1)
                    putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                    putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0)
                }
                val videoFile = AndroidFileUtils.createVideoFile(context)
                mCurrentPhoto = videoFile
                intent.putExtra(MediaStore.EXTRA_OUTPUT, ContentUri.getUriForFile(context, videoFile))
                startActivityForResult(intent, REQUEST_CODE_CAPTURE_VIDEO)
            } catch (ex: Exception) {
                Log.e(TAG, "sendVideoMessage: error", ex)
                Toast.makeText(activity, getString(R.string.video_recorder_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun takePicture() {
        if (!canSendThreadReply()) return
        if (!presenter.deviceRuntimeService.hasVideoPermission()) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_TAKE_PICTURE)
            return
        }
        val c = context ?: return
        try {
            val photoFile = AndroidFileUtils.createImageFile(c)
            Log.i(TAG, "takePicture: attempting to save to $photoFile")
            val photoURI = ContentUri.getUriForFile(c, photoFile)
            val takePictureIntent =
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    .putExtra("android.intent.extras.CAMERA_FACING", 1)
                    .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    .putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            mCurrentPhoto = photoFile
            startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PICTURE)
        } catch (e: Exception) {
            Toast.makeText(c, getString(R.string.taking_picture_error), Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun openFilePicker() {
        attachmentReplyTarget = outgoingReplyTarget()
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "*/*"
        startActivityForResult(intent, REQUEST_CODE_FILE_PICKER)
    }

    private fun openGallery() {
        attachmentReplyTarget = outgoingReplyTarget()
        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    private fun sendFile(file: File, replyTo: String?): Completable = Completable.fromAction {
        presenter.sendFile(file, replyTo)
    }

    private fun startFileSend(op: Completable): Disposable {
        setLoading(true)
        return op.observeOn(DeviceUtils.uiScheduler)
            .doFinally { setLoading(false) }
            .subscribe({}) { e ->
                Log.e(TAG, "startFileSend: unable to create cache file", e)
                displayErrorToast(Error.INVALID_FILE)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        Log.w(TAG, "onActivityResult: $requestCode $resultCode $resultData")
        val replyTo = attachmentReplyTarget
        if (requestCode == REQUEST_CODE_FILE_PICKER) {
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                val clipData = resultData.clipData
                if (clipData != null) { // checking multiple selection or not
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        startFileSend(AndroidFileUtils.getCacheFile(requireContext(), uri)
                            .observeOn(DeviceUtils.uiScheduler)
                            .flatMapCompletable { file: File -> sendFile(file, replyTo) })
                    }
                } else {
                    resultData.data?.let { uri ->
                        startFileSend(AndroidFileUtils.getCacheFile(requireContext(), uri)
                            .observeOn(DeviceUtils.uiScheduler)
                            .flatMapCompletable { file: File -> sendFile(file, replyTo) })
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_TAKE_PICTURE || requestCode == REQUEST_CODE_CAPTURE_VIDEO) {
            if (resultCode != Activity.RESULT_OK) {
                mCurrentPhoto = null
                return
            }
            val currentPhoto = mCurrentPhoto
            var file: Single<File>? = null
            if (currentPhoto == null || !currentPhoto.exists() || currentPhoto.length() == 0L) {
                resultData?.data?.let { uri ->
                    file = AndroidFileUtils.getCacheFile(requireContext(), uri)
                }
            } else {
                file = Single.just(currentPhoto)
            }
            mCurrentPhoto = null
            val sendingFile = file
            if (sendingFile != null)
                startFileSend(sendingFile.flatMapCompletable { f -> sendFile(f, replyTo) })
            else
                Toast.makeText(activity, getString(R.string.find_picture_error), Toast.LENGTH_SHORT).show()
        } else if (requestCode == REQUEST_CODE_SAVE_FILE) {
            val uri = resultData?.data
            if (resultCode == Activity.RESULT_OK && uri != null) {
                writeToFile(uri)
            }
        } else if (requestCode == REQUEST_CODE_EDIT_MESSAGE) {
            val uri = resultData?.data ?: return
            if (resultCode == Activity.RESULT_OK) {
                val path = InteractionPath.fromUri(uri) ?: return
                val message = resultData.getStringExtra(Intent.EXTRA_TEXT) ?: return
                presenter.editMessage(path.conversation.accountId, path.conversation.conversationUri, path.messageId, message)
            }
        }
    }

    private fun writeToFile(data: Uri) {
        val path = mCurrentFileAbsolutePath ?: return
        val cr = context?.contentResolver ?: return
        mCompositeDisposable.add(AndroidFileUtils.copyFileToUri(cr, File(path), data)
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ Toast.makeText(context, R.string.file_saved_successfully, Toast.LENGTH_SHORT).show() })
            { Toast.makeText(context, R.string.generic_error, Toast.LENGTH_SHORT).show() })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        for (i in permissions.indices) {
            val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED
            when (permissions[i]) {
                Manifest.permission.CAMERA -> {
                    presenter.cameraPermissionChanged(granted)
                    if (granted) {
                        if (requestCode == REQUEST_CODE_CAPTURE_VIDEO)
                            sendVideoMessage()
                        else if (requestCode == REQUEST_CODE_TAKE_PICTURE)
                            takePicture()
                    }
                    return
                }
                Manifest.permission.RECORD_AUDIO -> {
                    if (granted && requestCode == REQUEST_CODE_CAPTURE_AUDIO)
                        sendAudioMessage()
                    return
                }
                else -> {}
            }
        }
    }

    override fun addElement(element: Interaction) {
        val appended = mAdapter?.add(element) == true
        if (!historyRequestInFlight) historyExhausted = false
        loading = false
        if (threadState.enabled) {
            if (mAdapter?.history?.lastOrNull()?.messageId == element.messageId &&
                element.type != Interaction.InteractionType.INVALID
            ) element.messageId?.let { newThreadMessages.add(it) }
            scheduleThreadRender(element.messageId)
            return
        }
        if (appended && element.type != Interaction.InteractionType.INVALID) {
            scrollToEnd()
            if (element.isIncoming && !element.body.isNullOrEmpty()) {
                announceIncomingMessageForAccessibility()
            }
        }
    }

    override fun updateElement(element: Interaction) {
        mAdapter?.update(element)
        scheduleThreadRender(element.messageId)
    }

    override fun removeElement(element: Interaction) {
        mAdapter?.remove(element)
        scheduleThreadRender(element.messageId)
    }

    override fun setComposingStatus(composingStatus: ComposingStatus) {
        this.composingStatus = composingStatus
        mAdapter?.setComposingStatus(composingStatus)
        if (threadState.enabled) {
            if (threadState.rootId == null)
                threadAdapter?.setComposingStatus(composingStatus)
            return
        }
        if (composingStatus == ComposingStatus.Active) scrollToEnd()
    }

    override fun acceptFile(accountId: String, conversationUri: net.jami.model.Uri, transfer: DataTransfer) {
        if (transfer.messageId == null && transfer.fileId == null)
            return
        val cacheDir = requireContext().cacheDir
        val spaceLeft = AndroidFileUtils.getSpaceLeft(cacheDir.toString())
        if (spaceLeft == -1L || transfer.totalSize > spaceLeft) {
            presenter.noSpaceLeft()
            return
        }
        requireActivity().startService(Intent(DRingService.ACTION_FILE_ACCEPT, ConversationPath.toUri(accountId, conversationUri),
            requireContext(), DRingService::class.java)
            .putExtra(DRingService.KEY_MESSAGE_ID, transfer.messageId)
            .putExtra(DRingService.KEY_TRANSFER_ID, transfer.fileId)
        )
    }

    override fun shareFile(path: File, displayName: String) {
        val c = context ?: return
        AndroidFileUtils.shareFile(c, path, displayName)
    }

    override fun openFile(path: File, displayName: String) {
        val c = context ?: return
        AndroidFileUtils.openFile(c, path, displayName)
    }

    private fun actionSendMsgText(actionId: Int): Boolean = when (actionId) {
        EditorInfo.IME_ACTION_SEND -> {
            sendMessageText()
            true
        }
        else -> false
    }

    override fun onStart() {
        super.onStart()
        accountPreferences?.registerOnSharedPreferenceChangeListener(threadPreferenceListener)
        applyThreadPreference()
        presenter.resume(mIsBubble)
        mAdapter?.setTypingAnimationsActive(true)
        threadAdapter?.setTypingAnimationsActive(true)
    }

    override fun onStop() {
        super.onStop()
        accountPreferences?.unregisterOnSharedPreferenceChangeListener(threadPreferenceListener)
        mAdapter?.setTypingAnimationsActive(false)
        threadAdapter?.setTypingAnimationsActive(false)
        inlineAudioRecorder?.release()
        presenter.pause()
    }

    override fun onDestroy() {
        mPeerServicesCheckDisposable?.dispose()
        mCompositeDisposable.dispose()
        super.onDestroy()
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
            val searchMenuItem = menu.findItem(R.id.conv_search)
            searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    presenter.stopSearch()
                    binding?.searchList?.isVisible = false
                    currentBottomView?.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in))
                    return true
                }

                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    presenter.startSearch()
                    binding?.searchList?.isVisible = true
                    return true
                }
            })

            (searchMenuItem.actionView as? SearchView)?.let {
                it.setOnQueryTextListener(this@ConversationFragment)
                it.queryHint = getString(R.string.conversation_search_hint)
            }
        }

        override fun onMenuItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.conv_action_audiocall -> presenter.goToCall(false)
                R.id.conv_action_videocall -> presenter.goToCall(true)
                R.id.conv_conversation_details -> presenter.openContact()
                R.id.conv_action_peer_services -> showPeerServicesBottomSheet()
                else -> return false
            }
            return true
        }
    }

    // ==================== OnQueryTextListener methods =======================
    override fun onQueryTextSubmit(query: String): Boolean {
        return true
    }

    override fun onQueryTextChange(query: String): Boolean {
        if (query.isNotBlank())
            presenter.setSearchQuery(query.trim())
        mSearchAdapter?.clearSearchResults()
        return true
    }
    // ================== OnQueryTextListener methods end =======================

    override fun addSearchResults(results: List<Interaction>) {
        mSearchAdapter?.addSearchResults(results)
    }

    override fun shareText(body: String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, body)
            type = "text/plain"
        }, null))
    }

    override fun initPresenter(presenter: ConversationPresenter) {
        val path = ConversationPath.fromBundle(arguments)
        mIsBubble = requireArguments().getBoolean(NotificationServiceImpl.EXTRA_BUBBLE)
        Log.w(TAG, "initPresenter $path")
        if (path == null)
            return

        mAdapter = ConversationAdapter(this, presenter)
        mSearchAdapter = ConversationAdapter(this, presenter, isSearch = true)
        threadAdapter = ConversationAdapter(this, presenter, isThreadProjection = true).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        accountPreferences = getAccountPreferences(requireContext(), path.accountId)
        threadState = ConversationThreadState(
            path.conversationUri.isSwarm &&
                accountPreferences?.getBoolean(PREF_THREADED_VIEW, false) == true,
            restoredThreadRoot
        )

        // Load shared preferences. Usually useful for non-swarm conversations.
        try {
            mPreferences = getConversationPreferences(
                    requireContext(), path.accountId, path.conversationUri
            ).also { sharedPreferences ->
                sharedPreferences.edit { remove(KEY_PREFERENCE_CONVERSATION_LAST_READ) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to load conversation preferences")
        }
        presenter.init(path.conversationUri, path.accountId)

        var connection = locationServiceConnection
        if (connection == null) {
            connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    Log.w(TAG, "onServiceConnected")
                    val binder = service as LocationSharingService.LocalBinder
                    val locationService = binder.service
                    if (locationService.isSharing(path)) {
                        showMap(path.accountId, path.conversationUri.uri, false)
                    }
                    /*try {
                        requireContext().unbindService(locationServiceConnection!!)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error unbinding service", e)
                    }*/
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    Log.w(TAG, "onServiceDisconnected")
                    locationServiceConnection = null
                }
            }
            locationServiceConnection = connection
            Log.w(TAG, "bindService")
            requireContext().bindService(Intent(requireContext(), LocationSharingService::class.java), connection, 0)
        }
    }

    override fun updateContact(contact: ContactViewModel) {
        val contactKey = contact.contact.primaryNumber
        val a = mSmallParticipantAvatars[contactKey]
        if (a != null) {
            a.update(contact)
            mParticipantAvatars[contactKey]!!.update(contact)
            mAdapter?.setPhoto()
            threadAdapter?.setPhoto()
        } else {
            val builder = AvatarDrawable.Builder()
                .withContact(contact)
                .withCircleCrop(true)
            mParticipantAvatars[contactKey] = builder
                .withPresence(true)
                .build(requireContext())
            mSmallParticipantAvatars[contactKey] = builder
                .withPresence(false)
                .build(requireContext())
        }
    }

    override fun displayContact(conversation: ConversationItemViewModel) {
        mConversationItem = conversation
        val avatar = AvatarFactory.getAvatar(requireContext(), conversation).blockingGet()
        mParticipantAvatars[conversation.uri.rawRingId] = AvatarDrawable(avatar)
        setupActionbar(conversation, avatar)

        mPeerServicesCheckDisposable?.dispose()
        mPeerServicesCheckDisposable = null

        val peerServicesItem = binding?.toolbar?.menu?.findItem(R.id.conv_action_peer_services)
            ?: return
        peerServicesItem.isVisible = false

        if (conversation.isGroup()) return

        val peerContact = conversation.contacts.firstOrNull { !it.contact.isUser } ?: return
        val peerUri = peerContact.contact.uri.uri
        val path = ConversationPath.fromBundle(arguments) ?: return

        mPeerServicesCheckDisposable = peerServicesService.queryPeerServices(path.accountId, peerUri)
            .firstElement()
            .observeOn(uiScheduler)
            .subscribe { result ->
                if (result.status == PeerServicesStatus.OK && result.services.isNotEmpty()) {
                    binding?.toolbar?.menu?.findItem(R.id.conv_action_peer_services)?.isVisible = true
                }
            }
    }

    override fun displayOngoingCallPane(display: Boolean, hasVideo: Boolean) {
        val binding = binding ?: return
        if (display) {
            (binding.returnActiveCallIcon.background as? AnimatedVectorDrawable)?.start()
            val icon = if (hasVideo) R.drawable.outline_videocam_24 else R.drawable.outline_call_24
            binding.returnActiveCallIcon.setImageResource(icon)
            binding.toolbar.menu.findItem(R.id.conv_action_videocall).setEnabled(false)
            binding.toolbar.menu.findItem(R.id.conv_action_audiocall).setEnabled(false)
            binding.ongoingCallPane.visibility = View.VISIBLE
        } else {
            (binding.returnActiveCallIcon.background as? AnimatedVectorDrawable)?.stop()
            binding.toolbar.menu.findItem(R.id.conv_action_videocall).setEnabled(true)
            binding.toolbar.menu.findItem(R.id.conv_action_audiocall).setEnabled(true)
            binding.ongoingCallPane.visibility = View.GONE
        }
    }

    override fun displayNumberSpinner(conversation: Conversation, number: net.jami.model.Uri) {
        binding!!.numberSelector.visibility = View.VISIBLE
        binding!!.numberSelector.setSelection(getIndex(binding!!.numberSelector, number))
    }

    override fun hideNumberSpinner() {
        binding!!.numberSelector.visibility = View.GONE
    }

    override fun clearMsgEdit() {
        clearReply()
        binding!!.msgInputTxt.setText("")
    }

    override fun goToHome() {
        val hostActivity = activity
        if (hostActivity is ConversationActivity) {
            hostActivity.finish()
        } else {
            // Post because we might be currently executing a fragment transaction
            view?.post { hostActivity?.onBackPressedDispatcher?.onBackPressed() }
        }
    }

    private val conversationDetailsActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.extras?.getString(EXIT_REASON)?.let {
                    when (it) {
                        // Todo: Go to conversation for CONTACT_UNBLOCKED and INVITATION_ACCEPTED
                        ExitReason.CONTACT_ADDED.toString(),
                        ExitReason.CONTACT_DELETED.toString(),
                        ExitReason.CONTACT_BLOCKED.toString(),
                        ExitReason.CONTACT_UNBLOCKED.toString(),
                        ExitReason.CONVERSATION_LEFT.toString(),
                        ExitReason.INVITATION_ACCEPTED.toString() -> goToHome()
                        else -> {}
                    }
                }
            }
        }

    override fun goToDetailsActivity(accountId: String, uri: net.jami.model.Uri) {
        val logo = binding!!.conversationAvatar
        val options = ActivityOptionsCompat
            .makeSceneTransitionAnimation(requireActivity(), logo, "conversationIcon")
        val intent = Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, uri))
            .setClass(requireContext().applicationContext, ConversationDetailsActivity::class.java)
        conversationDetailsActivityLauncher.launch(intent, options)
    }

    private fun showPeerServicesBottomSheet() {
        val path = ConversationPath.fromBundle(arguments) ?: return
        val conversation = mConversationItem ?: return
        if (conversation.isGroup()) return
        val peerContact = conversation.contacts.firstOrNull { !it.contact.isUser } ?: return
        val peerUri = peerContact.contact.uri.uri
        PeerServicesBottomSheet
            .newInstance(path.accountId, peerUri)
            .show(childFragmentManager, PeerServicesBottomSheet.TAG)
    }

    override fun goToCallActivity(conferenceId: String, withCamera: Boolean) {
        startActivity(Intent(Intent.ACTION_VIEW)
            .setClass(requireContext(), CallActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(NotificationService.KEY_CALL_ID, conferenceId))
    }

    override fun goToCallActivityWithResult(accountId: String, conversationUri: net.jami.model.Uri, contactUri: net.jami.model.Uri, withCamera: Boolean) {
        startActivity(Intent(Intent.ACTION_CALL)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setClass(requireContext(), CallActivity::class.java)
            .putExtras(ConversationPath.toBundle(accountId, conversationUri))
            .putExtra(Intent.EXTRA_PHONE_NUMBER, contactUri.uri)
            .putExtra(CallFragment.KEY_HAS_VIDEO, withCamera))
    }

    /**
     * Go to the group call activity
     */
    override fun goToGroupCall(
        conversation: Conversation, contactUri: net.jami.model.Uri, hasVideo: Boolean
    ) {
        // Attempt to find an existing call
        val conf = conversation.currentCall
        // Join existing call (active, even if no participants yet)
        if (conf != null && conf.isOnGoing) {
            startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setClass(requireContext(), CallActivity::class.java)
                    .putExtra(NotificationService.KEY_CALL_ID, conf.id)
            )
        } else { // Otherwise, start a new call
            val intent = Intent(Intent.ACTION_CALL)
                .setClass(requireContext(), CallActivity::class.java)
                .putExtras(ConversationPath.toBundle(conversation))
                .putExtra(Intent.EXTRA_PHONE_NUMBER, contactUri.uri)
                .putExtra(CallFragment.KEY_HAS_VIDEO, hasVideo)
            startActivityForResult(intent, ConversationDetailsActivity.REQUEST_CODE_CALL)
        }
    }

    private fun setupActionbar(conversation: ConversationItemViewModel, img: AvatarDrawable) {
        binding?.apply {
            conversationAvatar.setImageDrawable(img)
            contactTitle.text = conversation.title
            threadConversationName.text = conversation.title
            if (conversation.uriTitle != conversation.title) {
                contactSubtitle.text = conversation.uriTitle
                contactSubtitle.visibility = View.VISIBLE
            } else {
                contactSubtitle.text = ""
                contactSubtitle.visibility = View.GONE
            }
        }
    }


//    fun blockContactRequest() {
//        presenter.onBlockIncomingContactRequest()
//    }
//
//    fun refuseContactRequest() {
//        presenter.onRefuseIncomingContactRequest()
//    }
//
//    fun acceptContactRequest() {
//        presenter.onAcceptIncomingContactRequest()
//    }

    override fun switchToUnknownView() {
        binding?.apply {
            toolbar.menu.findItem(R.id.conv_search).isVisible = false
            toolbar.menu.findItem(R.id.conv_action_videocall).isVisible = false
            toolbar.menu.findItem(R.id.conv_action_audiocall).isVisible = false
            toolbar.menu.findItem(R.id.conv_action_peer_services).isVisible = false
            cvMessageInput.visibility = View.GONE
            unknownContactPrompt.visibility = View.VISIBLE
            trustRequestPrompt.visibility = View.GONE
            trustRequestMessage.text = getString(R.string.outgoing_contact_invitation_message)
            trustRequestMessage.visibility = View.VISIBLE
            currentBottomView = unknownContactPrompt
        }
        updatePaddings()
    }

    override fun switchToIncomingTrustRequestView(name: String, requestMode: Conversation.Mode) {
        binding?.apply {
            toolbar.menu.findItem(R.id.conv_search).isVisible = false
            toolbar.menu.findItem(R.id.conv_action_videocall).isVisible = false
            toolbar.menu.findItem(R.id.conv_action_audiocall).isVisible = false
            toolbar.menu.findItem(R.id.conv_action_peer_services).isVisible = false
            cvMessageInput.visibility = View.GONE
            unknownContactPrompt.visibility = View.GONE
            trustRequestPrompt.visibility = View.VISIBLE
            btnBlock.isVisible = requestMode == Conversation.Mode.OneToOne
            trustRequestMessage.text = getString(R.string.invitation_received_message, name)
            trustRequestMessage.visibility = View.VISIBLE
            currentBottomView = trustRequestPrompt
        }
        updatePaddings()
    }

    override fun switchToConversationView() {
        binding?.apply {
            toolbar.menu.findItem(R.id.conv_search).isVisible = true
            toolbar.menu.findItem(R.id.conv_action_videocall).isVisible = true
            toolbar.menu.findItem(R.id.conv_action_audiocall).isVisible = true
            cvMessageInput.visibility = View.VISIBLE
            unknownContactPrompt.visibility = View.GONE
            trustRequestPrompt.visibility = View.GONE
            trustRequestMessage.visibility = View.GONE
            currentBottomView = cvMessageInput
            cvMessageInput.doOnNextLayout {
                cvMessageInput.height.takeIf { it > 0 }?.let {
                    histList.updatePadding(bottom = it + marginPxTotal)
                }
            }
        }
        updatePaddings()
    }

    override fun switchToSyncingView() {
        binding?.apply {
            toolbar.menu.findItem(R.id.conv_search).setVisible(false)
            toolbar.menu.findItem(R.id.conv_action_videocall).setVisible(false)
            toolbar.menu.findItem(R.id.conv_action_audiocall).setVisible(false)
            toolbar.menu.findItem(R.id.conv_action_peer_services).setVisible(false)
            cvMessageInput.visibility = View.GONE
            unknownContactPrompt.visibility = View.GONE
            trustRequestPrompt.visibility = View.GONE
            trustRequestMessage.visibility = View.VISIBLE
            trustRequestMessage.text = getText(R.string.conversation_syncing)
        }
        currentBottomView = null
        updatePaddings()
    }

    override fun switchToBlockedView() {
        binding?.apply {
            toolbar.menu.findItem(R.id.conv_search).setVisible(false)
            toolbar.menu.findItem(R.id.conv_action_videocall).setVisible(false)
            toolbar.menu.findItem(R.id.conv_action_audiocall).setVisible(false)
            toolbar.menu.findItem(R.id.conv_action_peer_services).setVisible(false)
            cvMessageInput.visibility = View.GONE
            unknownContactPrompt.visibility = View.GONE
            trustRequestPrompt.visibility = View.GONE
            trustRequestMessage.visibility = View.VISIBLE
            trustRequestMessage.text = getText(R.string.conversation_blocked)
        }
        updatePaddings()
    }

    override fun switchToEndedView(canSwitch: Boolean) {
        binding?.apply {
            toolbar.menu.findItem(R.id.conv_search).isVisible = false
            toolbar.menu.findItem(R.id.conv_action_videocall).isVisible = false
            toolbar.menu.findItem(R.id.conv_action_audiocall).isVisible = false
            toolbar.menu.findItem(R.id.conv_action_peer_services).isVisible = false
            cvMessageInput.visibility = View.GONE
            unknownContactPrompt.visibility = View.GONE
            trustRequestPrompt.visibility = View.GONE
            trustRequestMessage.visibility = View.VISIBLE
            trustRequestMessage.text = getText(R.string.conversation_ended)
            btnDelete.setOnClickListener {
                context?.run {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.menu_delete)
                        .setMessage(getString(R.string.delete_conversation_confirmation))
                        .setPositiveButton(R.string.menu_delete) { _, _ -> presenter.deleteConversation() }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
            }
            btnSwitch.visibility = if (canSwitch) View.VISIBLE else View.GONE
            btnSwitch.setOnClickListener { presenter.switchConversation() }
            endedConversationPrompt.visibility = View.VISIBLE
        }
        val endedView = binding?.endedConversationPrompt ?: return
        currentBottomView = endedView
        endedView.doOnNextLayout {
            endedView.height.takeIf { it > 0 }?.let {
                binding?.histList?.updatePadding(bottom = it + marginPxTotal)
            }
        }
        updatePaddings()
    }


    private fun setLoading(isLoading: Boolean) {
        val binding = binding ?: return
        if (isLoading) {
            binding.btnTakePicture.visibility = View.GONE
            binding.pbDataTransfer.visibility = View.VISIBLE
        } else {
            binding.btnTakePicture.visibility = View.VISIBLE
            binding.pbDataTransfer.visibility = View.GONE
        }
    }

    override fun openConversation(accountId: String, conversationUri: net.jami.model.Uri) {
        activity?.apply {
            if (this is HomeActivity)
                this.startConversation(accountId, conversationUri)
            else
                startActivity(Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, conversationUri), this, HomeActivity::class.java))
        }
    }

    fun handleShareIntent(intent: Intent) {
        Log.w(TAG, "handleShareIntent $intent")
        val action = intent.action
        if (Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action) {
            val replyTo = outgoingReplyTarget()
            intent.getShareItems(requireContext()).forEach { shareItem ->
                if (shareItem.type == "text/plain" && shareItem.text != null) {
                    binding!!.msgInputTxt.setText(shareItem.text)
                } else if (shareItem.data != null){
                    startFileSend(AndroidFileUtils.getCacheFile(requireContext(), shareItem.data)
                        .flatMapCompletable { file -> sendFile(file, replyTo) })
                }
            }
        } else if (Intent.ACTION_VIEW == action) {
            val path = ConversationPath.fromIntent(intent)
            if (path != null && intent.getBooleanExtra(EXTRA_SHOW_MAP, false)) {
                shareLocation()
            }
        }
        // After consuming the intent, replace it to prevent it from being handled again
        intent.action = Intent.ACTION_VIEW
        activity?.intent = intent
    }

    /**
     * Creates an intent using Android Storage Access Framework
     * This intent is then received by applications that can handle it like
     * Downloads or Google drive
     * @param file DataTransfer of the file that is going to be stored
     * @param fileAbsolutePath absolute path of the file we want to save
     */
    override fun startSaveFile(file: DataTransfer, fileAbsolutePath: String) {
        //Get the current file absolute path and store it
        mCurrentFileAbsolutePath = fileAbsolutePath
        try {
            //Use Android Storage File Access to download the file
            val downloadFileIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = AndroidFileUtils.getMimeTypeFromExtension(file.extension)
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, file.displayName)
            }
            startActivityForResult(downloadFileIntent, REQUEST_CODE_SAVE_FILE)
        } catch (e: Exception) {
            Log.i(TAG, "No app detected for saving files.")
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            writeToFile(Uri.fromFile(File(directory, file.displayName)))
        }
    }

    override fun startReplyTo(interaction: Interaction) {
        if (threadState.enabled) {
            val id = interaction.messageId
            val root = id?.let { ConversationThreads(mAdapter?.history.orEmpty()).rootId(it) }
            if (root != null && root != threadState.rootId) {
                openThread(root) {
                    showReplyPreview(interaction, focus = false)
                    threadScreenOnShown = { showReplyPreview(interaction, focus = true) }
                }
                return
            }
        }
        showReplyPreview(interaction, focus = true)
    }

    private fun showReplyPreview(interaction: Interaction, focus: Boolean) {
        replyingTo = interaction
        mPreferences?.edit { putString(threadState.replyDraftKey, interaction.messageId) }
        binding?.apply {
            replyMessage.text = interaction.body
            replyMessage.isVisible = !interaction.body.isNullOrEmpty()
            replyGroup.isVisible = true
            if (!focus) return@apply
            msgInputTxt.post {
                if (!msgInputTxt.hasFocus()) {
                    msgInputTxt.requestFocus()
                    (requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                        ?.showSoftInput(msgInputTxt, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }
    }

    override fun displayNetworkErrorPanel() {
        binding?.apply {
            errorMsgPane.visibility = View.VISIBLE
            errorMsgPane.setOnClickListener(null)
            errorMsgPane.setText(R.string.error_no_network)
        }
        lastInsets?.let { updatePaddings(it) }
    }

    override fun displayAccountOfflineErrorPanel() {
        accountOffline = true
        binding?.apply {
            errorMsgPane.visibility = View.VISIBLE
            errorMsgPane.setOnClickListener(null)
            errorMsgPane.setText(R.string.error_account_offline)
            for (idx in 0 until btnContainer.childCount) {
                btnContainer.getChildAt(idx).isEnabled = false
            }
        }
        lastInsets?.let { updatePaddings(it) }
        updateThreadHeader()
    }

    override fun setSettings(linkPreviews: Boolean) {
        mAdapter?.showLinkPreviews = linkPreviews
        mSearchAdapter?.showLinkPreviews = linkPreviews
        threadAdapter?.showLinkPreviews = linkPreviews
    }

    override fun hideErrorPanel() {
        if (binding?.errorMsgPane?.isVisible == true) historyExhausted = false
        accountOffline = false
        binding?.errorMsgPane?.visibility = View.GONE
        lastInsets?.let { updatePaddings(it) }
        updateThreadHeader()
    }

    override fun goToSearchMessage(messageId: String) {
        binding?.toolbar?.menu?.findItem(R.id.conv_search)?.collapseActionView()
        if (threadState.enabled) {
            revealThreadMessage(messageId)
            return
        }
        binding?.histList?.doOnNextLayout {
            presenter.scrollToMessage(messageId)
        }
    }

    companion object {
        val TAG = ConversationFragment::class.simpleName
        const val REQ_ADD_CONTACT = 42
        const val KEY_PREFERENCE_PENDING_MESSAGE = ConversationThreadState.MAIN_DRAFT_KEY
        private const val STATE_THREAD_ROOT = "threadRoot"
        private const val STATE_SCROLL_MESSAGE = "threadScrollMessage"
        private const val STATE_SCROLL_OFFSET = "threadScrollOffset"
        private const val STATE_MAIN_SCROLL_MESSAGE = "mainScrollMessage"
        private const val STATE_MAIN_SCROLL_OFFSET = "mainScrollOffset"
        private const val STATE_ATTACHMENT_REPLY = "attachmentReply"
        private const val STATE_AUDIO_REPLY = "audioReply"
        private const val STATE_PHOTO = "pendingPhoto"
        @Deprecated("Use daemon feature")
        const val KEY_PREFERENCE_CONVERSATION_LAST_READ = "lastRead"
        const val EXTRA_SHOW_MAP = "showMap"
        private const val REQUEST_CODE_FILE_PICKER = 1000
        private const val REQUEST_PERMISSION_CAMERA = 1001
        private const val REQUEST_CODE_TAKE_PICTURE = 1002
        private const val REQUEST_CODE_SAVE_FILE = 1003
        private const val REQUEST_CODE_CAPTURE_AUDIO = 1004
        private const val REQUEST_CODE_CAPTURE_VIDEO = 1005
        const val REQUEST_CODE_EDIT_MESSAGE = 1006
        private const val MAX_AUDIO_DURATION_MS = 300_000 // 5 minutes
        private const val AUDIO_RECORDER_TAG = "audio_recorder"
        private const val DEFAULT_AUDIO_MAX_SIZE = Long.MAX_VALUE
        private fun getIndex(spinner: Spinner, myString: net.jami.model.Uri): Int {
            var i = 0
            val n = spinner.count
            while (i < n) {
                if ((spinner.getItemAtPosition(i) as Phone).number == myString) {
                    return i
                }
                i++
            }
            return 0
        }

        private val SUPPORTED_MIME_TYPES = arrayOf("image/png", "image/jpg", "image/gif", "image/webp")
    }
}
