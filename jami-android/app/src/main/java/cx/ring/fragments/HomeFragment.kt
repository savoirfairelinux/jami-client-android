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

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.material.R as MaterialR
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.Behavior.DragCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.motion.MotionUtils
import cx.ring.R
import cx.ring.account.AccountWizardActivity
import cx.ring.adapters.SmartListAdapter
import cx.ring.client.AccountAdapter
import cx.ring.client.HomeActivity
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.BitmapUtils
import cx.ring.utils.DeviceUtils
import cx.ring.viewholders.SmartListViewHolder
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import net.jami.home.HomePresenter
import net.jami.home.HomeView
import net.jami.model.Conversation
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import net.jami.smartlist.ConversationFilter
import net.jami.smartlist.ConversationItemViewModel
import java.util.concurrent.TimeUnit
import java.text.NumberFormat
import javax.inject.Inject
import com.google.android.material.search.SearchView.TransitionState
import com.google.android.material.shape.MaterialShapeDrawable
import cx.ring.databinding.FragHomeBinding
import cx.ring.utils.ActionHelper.openJamiDonateWebPage
import io.reactivex.rxjava3.disposables.Disposable
import net.jami.model.Uri
import net.jami.services.NotificationService

internal fun beginPendingViewTransition(root: ViewGroup) {
    TransitionManager.endTransitions(root)
    root.findViewById<ViewGroup>(R.id.app_bar)?.let(TransitionManager::endTransitions)
    val context = root.context
    val transition = TransitionSet().apply {
        ordering = TransitionSet.ORDERING_SEQUENTIAL
        interpolator = MotionUtils.resolveThemeInterpolator(
            context, MaterialR.attr.motionEasingStandardInterpolator, FastOutSlowInInterpolator()
        )
        // Hide outgoing content before its space is reclaimed by the filters and list.
        addTransition(Fade(Fade.OUT).apply {
            duration = MotionUtils.resolveThemeDuration(context, MaterialR.attr.motionDurationShort1, 50).toLong()
        })
        addTransition(ChangeBounds().apply {
            duration = MotionUtils.resolveThemeDuration(context, MaterialR.attr.motionDurationShort3, 150).toLong()
        })
        addTransition(Fade(Fade.IN).apply {
            duration = MotionUtils.resolveThemeDuration(context, MaterialR.attr.motionDurationShort2, 100).toLong()
        })
        excludeChildren(RecyclerView::class.java, true)
    }
    TransitionManager.beginDelayedTransition(root, transition)
}

@AndroidEntryPoint
class HomeFragment: BaseSupportFragment<HomePresenter, HomeView>(),
    SearchView.OnQueryTextListener, HomeView {

    private var mBinding: FragHomeBinding? = null
    private var mSmartListFragment: SmartListFragment? = null
    private val mDisposable = CompositeDisposable()
    private var mSearchView: SearchView? = null
    private var searchDisposable: Disposable? = null
    private var searchAdapter: SmartListAdapter? = null
    private var pendingAdapter: SmartListAdapter? = null
    private var pendingRequests = emptyList<Conversation>()
    private var pendingFeeds = false
    private val conversationFilter = BehaviorSubject.createDefault(ConversationFilter.All)
    private val querySubject = BehaviorSubject.createDefault("")
    private val debouncedQuery = querySubject.debounce { item ->
        if (item.isEmpty()) Observable.empty()
        else Observable.timer(350, TimeUnit.MILLISECONDS)
    }.distinctUntilChanged()

    @Inject
    lateinit var mAccountService: AccountService

    @Inject
    lateinit var mConversationFacade: ConversationFacade

    private val searchBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            collapseSearchActionView()
        }
    }

    private val conversationBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            collapsePendingView()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.let {
            it.addCallback(this, conversationBackPressedCallback)
            it.addCallback(this, searchBackPressedCallback)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(STATE_CONVERSATION_FILTER)?.let { name ->
            val filter = ConversationFilter.entries.find { it.name == name }
            if (filter != null) conversationFilter.onNext(filter)
            else Log.w(TAG, "Unknown saved conversation filter: $name")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CONVERSATION_FILTER, checkNotNull(conversationFilter.value).name)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragHomeBinding.inflate(inflater, container, false).apply {

        qrCode.setOnClickListener { goToQRFragment() }
        newSwarm.setOnClickListener { startNewSwarm() }
        newFeed.setOnClickListener { startNewFeed() }

        conversationFilters.check(
            when (checkNotNull(conversationFilter.value)) {
                ConversationFilter.All -> R.id.filter_all
                ConversationFilter.Contacts -> R.id.filter_contacts
                ConversationFilter.Groups -> R.id.filter_groups
                ConversationFilter.Feeds -> R.id.filter_feeds
            }
        )
        conversationFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.singleOrNull()) {
                R.id.filter_all -> ConversationFilter.All
                R.id.filter_contacts -> ConversationFilter.Contacts
                R.id.filter_groups -> ConversationFilter.Groups
                R.id.filter_feeds -> ConversationFilter.Feeds
                else -> return@setOnCheckedStateChangeListener
            }
            conversationFilter.onNext(filter)
            mSmartListFragment?.setConversationFilter(filter)
            mSmartListFragment?.scrollToTop()
        }

        // SearchBar is composed of:
        // - Account selection (navigation)
        // - Search bar (search for swarms or for new contacts)
        // - Menu (for settings, about jami)
        searchBar.setNavigationOnClickListener { // Account selection
            mDisposable.add(mAccountService.observableAccountList.firstElement().subscribe { accounts ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.account_selection))
                    .setAdapter(
                            AccountAdapter(
                                requireContext(),
                                accounts,
                                mDisposable, mAccountService, mConversationFacade
                            )
                    ) { _, index ->
                        if (index >= accounts.size) // Add account
                            startActivity(Intent(activity, AccountWizardActivity::class.java))
                        else if (mAccountService.currentAccount != accounts[index]) {
                            // Disable account settings menu option when account is loading
                            searchBar.menu.findItem(R.id.menu_account_settings).isEnabled = false
                            mAccountService.currentAccount = accounts[index]
                        }
                    }.show()
            })
        }
        searchView.editText.addTextChangedListener { // Search bar
            querySubject.onNext(it.toString())
        }

        // Inflate Menu and connect it
        searchBar.inflateMenu(R.menu.smartlist_menu)
        searchBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_account_settings -> (activity as? HomeActivity)?.goToAccountSettings()

                R.id.menu_advanced_settings -> (activity as? HomeActivity)?.goToAdvancedSettings()

                R.id.menu_about -> (activity as? HomeActivity)?.goToAbout()

                R.id.menu_donate -> openJamiDonateWebPage(requireContext())
            }
            true
        }

        // Update padding of the list depending on the AppBarLayout height
        appBar.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            if (fragmentContainer.isVisible) {
                mSmartListFragment?.getRecyclerView()?.setPadding(
                    0,
                    appBar.height - DeviceUtils.getStatusBarHeight(requireContext()),
                    0, 0
                )
            }
        }

        // Make the appBarLayout not going under the status bar.
        appBar.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(requireContext())

        // Setup search result list
        searchResult.setHasFixedSize(true)
        searchResult.adapter = SmartListAdapter(null,
                object : SmartListViewHolder.SmartListListeners {
                    override fun onItemClick(item: Conversation) {
                        collapseSearchActionView()
                        (requireActivity() as HomeActivity).startConversation(item.accountId, item.uri)
                    }

                    override fun onItemLongClick(item: Conversation) {}
                },
                mConversationFacade,
                mDisposable
        ).apply { searchAdapter = this }

        searchView.addTransitionListener { _, previousState, newState ->
            // When using the SearchView, we have to :
            // - Manage back press
            // - Manage search results

            if (newState === TransitionState.SHOWN) { // Shown
                // Hide AppBar and SmartList to avoid weird animation
                fragmentContainer.isVisible = false
                appBar.isVisible = false

                searchBackPressedCallback.isEnabled = true
            } else if (previousState === TransitionState.SHOWN) { // Hiding
                // Make SmartList and appbar visible again
                fragmentContainer.isVisible = true
                appBar.isVisible = true

                searchBackPressedCallback.isEnabled = false
            }

            if (newState === TransitionState.HIDDEN) { // Hidden
                // Hide floating button to avoid weird animation
                newSwarmFab.isVisible = true

                searchDisposable?.dispose()
                querySubject.onNext("")
                searchAdapter?.update(ConversationFacade.ConversationList())
                searchDisposable = null
            } else if (previousState === TransitionState.HIDDEN) { // Showing
                searchView.toolbar.navigationIcon = AppCompatResources.getDrawable(
                    requireContext(), R.drawable.baseline_arrow_back_24
                )
                newSwarmFab.isVisible = false
                startSearch()
            }
        }

        // Setup floating button.
        newSwarmFab.setOnClickListener { expandSearchActionView() }

        // Setup donation card
        donationCard.donationCard.visibility = View.GONE
        donationCard.donationCard.setOnClickListener {
            openJamiDonateWebPage(requireContext())
        }
        donationCard.donationCardDonateButton.setOnClickListener {
            openJamiDonateWebPage(requireContext())
        }
        donationCard.donationCardNotNowButton.setOnClickListener {
            presenter.setDonationReminderDismissed()
        }

        // Setup invitation card adapter.
        invitationCard.pendingList.adapter = SmartListAdapter(null,
                object : SmartListViewHolder.SmartListListeners {
                    override fun onItemClick(item: Conversation) {
                        (requireActivity() as HomeActivity).startConversation(item.accountId, item.uri)
                    }

                    override fun onItemLongClick(item: Conversation) {
                        displayConversationRequestDialog(item)
                    }
                },
                mConversationFacade,
                mDisposable
        ).apply { pendingAdapter = this }

        invitationCard.invitationGroup.setOnClickListener {
            expandPendingView()
        }

        // Return to search
        invitationCard.pendingToolbar.setNavigationOnClickListener { collapsePendingView() }

        mBinding = this
    }.root

    private fun pendingTitle(isFeed: Boolean): Int =
        if (isFeed) R.string.joinable_feeds else R.string.invitation_card_title

    private fun displayConversationRequestDialog(conversation: Conversation) {
        val request = conversation.request ?: return
        if (request.mode == Conversation.Mode.OneToOne)
            MaterialAlertDialogBuilder(requireContext())
                .setItems(R.array.swarm_request_one_to_one_actions) { _, which ->
                    when (which) {
                        0 -> mConversationFacade.acceptRequest(conversation)
                        1 -> mConversationFacade
                            .discardRequest(conversation.accountId, conversation.uri)
                        2 -> mConversationFacade
                            .blockConversation(conversation.accountId, conversation.uri)
                    }
                }.show()
        else
            MaterialAlertDialogBuilder(requireContext())
                .apply {
                    if (conversation.effectiveMode == Conversation.Mode.Feed) {
                        setTitle(R.string.joinable_feeds)
                        setIcon(R.drawable.ic_feed)
                    }
                }
                .setItems(
                    if (conversation.effectiveMode == Conversation.Mode.Feed) R.array.feed_request_actions
                    else R.array.swarm_request_group_actions
                ) { _, which ->
                    when (which) {
                        0 -> mConversationFacade.acceptRequest(conversation)
                        1 -> mConversationFacade
                            .discardRequest(conversation.accountId, conversation.uri)
                    }
                }.show()
    }

    private fun startSearch() {
        searchDisposable?.dispose()
        searchDisposable = mConversationFacade.getSearchResults(debouncedQuery)
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { searchAdapter?.update(it) }
            .apply { mDisposable.add(this) }
    }

    /**
     * Expand the appBarLayoutBottom to give fixed space between it and fragmentList.
     */
    private fun updateAppBarLayoutBottomPadding(hasInvites: Boolean) {
        mBinding?.appBarContainer?.updatePadding(top = 0, bottom = if (hasInvites)
            resources.getDimensionPixelSize(R.dimen.bottom_sheet_radius) else 0)
        if (hasInvites)
            mSmartListFragment?.scrollToTop()
    }

    private fun expandPendingView() {
        val binding = mBinding ?: return
        if (pendingFeeds) {
            binding.conversationFilters.check(R.id.filter_feeds)
        } else if (conversationFilter.value == ConversationFilter.Feeds) {
            binding.conversationFilters.check(R.id.filter_all)
        }
        pendingAdapter?.update(pendingRequests)
        binding.invitationCard.pendingToolbar.setTitle(pendingTitle(pendingFeeds))

        beginPendingViewTransition(binding.root)

        // Make the invitation card take all the height.
        binding.appBar.updateLayoutParams {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        // Adapt the margins of the invitation card.
        requireContext().resources.getDimensionPixelSize(R.dimen.bottom_sheet_radius).let {
            (binding.invitationCard.invitationGroup.layoutParams as ViewGroup.MarginLayoutParams)
                .setMargins(it, it, it, 2*it)
        }

        // Enable invitation pending list scroll (remove side effect with appbar behavior).
        (binding.appBar.layoutParams as CoordinatorLayout.LayoutParams).behavior = null

        // Hide everything unneeded.
        binding.donationCard.donationCard.isVisible = false
        binding.searchBar.isVisible = false
        binding.invitationCard.invitationSummary.isVisible = false
        binding.fragmentContainer.isVisible = false
        binding.newSwarmFab.isVisible = false
        binding.filterAll.isVisible = !pendingFeeds
        binding.filterContacts.isVisible = !pendingFeeds
        binding.filterGroups.isVisible = !pendingFeeds
        binding.filterFeeds.isVisible = pendingFeeds

        // Display pending list.
        binding.invitationCard.pendingListGroup.isVisible = true

        // Enable back press.
        conversationBackPressedCallback.isEnabled = true

        val insetsCompat = ViewCompat.getRootWindowInsets(binding.invitationCard.invitationGroup) ?: return
        val insets = insetsCompat.getInsets(WindowInsetsCompat.Type.systemBars())
        binding.appBar.updatePadding(bottom = insets.bottom)
    }

    fun collapsePendingView() {
        val binding = mBinding ?: return

        beginPendingViewTransition(binding.root)

        binding.appBar.updatePadding(bottom = 0)

        // Make the invitation card wrap content (not take all space available anymore).
        binding.appBar.updateLayoutParams {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        // Adapt the margins of the invitation card.
        requireContext().resources.getDimensionPixelSize(R.dimen.bottom_sheet_radius).let {
            (binding.invitationCard.invitationGroup.layoutParams as ViewGroup.MarginLayoutParams)
                .setMargins(it, 0, it, 0)
        }

        disableAppBarScroll()

        // Show everything needed.
        binding.donationCard.donationCard.isVisible = presenter.donationCardIsVisible
        binding.searchBar.isVisible = true
        binding.invitationCard.invitationSummary.isVisible = true
        binding.newSwarmFab.isVisible = true
        binding.fragmentContainer.isVisible = true
        binding.filterAll.isVisible = true
        binding.filterContacts.isVisible = true
        binding.filterGroups.isVisible = true
        binding.filterFeeds.isVisible = true

        // Hide pending list.
        binding.invitationCard.pendingListGroup.isVisible = false

        // Disable back press.
        conversationBackPressedCallback.isEnabled = false
    }

    // Will hide the floating button when scrolling down and show it when scrolling up.
    private val fabScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val canScrollUp = recyclerView.canScrollVertically(-1)
            val isExtended = mBinding!!.newSwarmFab.isExtended
            if (dy > 0 && isExtended) { // Going down
                mBinding!!.newSwarmFab.shrink()
            } else if ((dy < 0 || !canScrollUp) && !isExtended) { // Going up
                mBinding!!.newSwarmFab.extend()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(mBinding!!.searchView) { v, insets ->
            if (v.paddingTop > 0) {
                // Ensure searchView ignores top insets for proper fullscreen appearance
                v.setPadding(v.paddingLeft, 0, v.paddingRight, v.paddingBottom)
            }
            insets
        }

        val accessibilityManager =
            requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        adjustLayoutForAccessibility(accessibilityManager.isEnabled )

        accessibilityManager.addAccessibilityStateChangeListener { isEnabled ->
            adjustLayoutForAccessibility(isEnabled)
        }

        mSmartListFragment = mBinding!!.fragmentContainer.getFragment()
        mSmartListFragment?.setConversationFilter(checkNotNull(conversationFilter.value))

        disableAppBarScroll()

        // Subscribe on fragmentContainer to add scroll listener on the recycler view.
        mSmartListFragment?.viewLifecycleOwnerLiveData?.observe(viewLifecycleOwner) {
            it.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    mSmartListFragment?.getRecyclerView()?.addOnScrollListener(fabScrollListener)
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    mSmartListFragment?.getRecyclerView()?.removeOnScrollListener(fabScrollListener)
                }
            })
        }
    }

    override fun onDestroyView() {
        mBinding?.root?.let(TransitionManager::endTransitions)
        super.onDestroyView()
        mSmartListFragment = null
        pendingAdapter = null
        pendingRequests = emptyList()
        pendingFeeds = false
        conversationBackPressedCallback.isEnabled = false
        searchAdapter = null
        mBinding = null
        mDisposable.clear()
    }

    override fun onStart() {
        super.onStart()
        activity?.intent?.let { handleIntent(it) }

        // Enable account settings menu option when an account is loaded
        mDisposable.add(mAccountService.currentAccountSubject
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe {
                mBinding?.newSwarm?.isVisible = !it.isSip
                mBinding?.newFeed?.isVisible = !it.isSip
                mBinding?.searchBar?.menu?.findItem(R.id.menu_account_settings)?.isEnabled = true
            }
        )

        mDisposable.add(mAccountService
            .currentAccountSubject
            .switchMap { account ->
                Observable.combineLatest(
                    account.getPendingSubject(),
                    conversationFilter.distinctUntilChanged()
                ) { list, filter ->
                    list.filter {
                        filter.matches(it) &&
                            (filter == ConversationFilter.Feeds || it.effectiveMode != Conversation.Mode.Feed)
                    }
                }
                    .switchMap { requests ->
                        val previews = if (requests.isEmpty()) {
                            Observable.just(emptyList<ConversationItemViewModel>())
                        } else {
                            mConversationFacade.observeConversations(account, requests.take(3), false)
                        }
                        previews.map { Pair(requests, it) }.observeOn(DeviceUtils.uiScheduler)
                    }
            }
            .subscribe { (requests, previews) ->
                setPendingRequests(requests, previews)
            }
        )

        // Subscribe on current account to display avatar on navigation (searchbar)
        mDisposable.add(mAccountService
            .currentAccountSubject
            .switchMap { mAccountService.getObservableAccountProfile(it.accountId) }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { profile ->
                val binding = mBinding ?: return@subscribe
                binding.searchBar.navigationIcon =
                    BitmapUtils.withPadding(
                        AvatarDrawable.build(
                            binding.root.context,
                            profile.first,
                            profile.second,
                            true,
                            profile.first.presenceStatus
                        ),
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            6f,
                            resources.displayMetrics
                        ).toInt()
                    )
                binding.searchView.toolbar.navigationIcon = AppCompatResources.getDrawable(
                    binding.root.context, R.drawable.baseline_arrow_back_24
                )
            }
        )

        if (mBinding!!.searchView.isShowing)
            startSearch()
    }

    override fun onStop() {
        super.onStop()
        mDisposable.clear()
    }

    private fun adjustLayoutForAccessibility(enabled: Boolean) {
        val params = mBinding?.appBarContainer?.layoutParams as? AppBarLayout.LayoutParams ?: return
        val recyclerView = mSmartListFragment?.getRecyclerView() ?: return

        params.scrollFlags = if (enabled) 0 else (
                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
                )
        mBinding?.appBarContainer?.layoutParams = params
        recyclerView.clipToPadding = enabled
    }

    private fun setPendingRequests(requests: List<Conversation>, previews: List<ConversationItemViewModel>) {
        val binding = mBinding ?: return
        val isFeed = requests.firstOrNull()?.effectiveMode == Conversation.Mode.Feed
        if (conversationBackPressedCallback.isEnabled && (requests.isEmpty() || pendingFeeds != isFeed)) {
            collapsePendingView()
        }
        pendingRequests = requests
        pendingFeeds = isFeed
        pendingAdapter?.update(requests)
        val hasRequests = requests.isNotEmpty()
        binding.invitationCard.apply {
            invitationGroup.isVisible = hasRequests
            invitationReceivedLabel.setText(pendingTitle(isFeed))
            invitationIcon.setImageResource(if (isFeed) R.drawable.ic_feed else R.drawable.new_invitation)
            invitationIcon.imageTintList = if (isFeed) invitationReceivedLabel.textColors else null
            invitationBadge.text = NumberFormat.getIntegerInstance().format(requests.size)
            invitationReceivedTxt.text = previews.joinToString(", ") { it.title }
        }
        updateAppBarLayoutBottomPadding(hasRequests)
    }

    fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_CALL -> {
                expandSearchActionView()
                mSearchView?.setQuery(intent.dataString, true)
            }

            Intent.ACTION_DIAL -> {
                expandSearchActionView()
                mSearchView?.setQuery(intent.dataString, false)
            }

            Intent.ACTION_SEARCH -> {
                expandSearchActionView()
                mSearchView?.setQuery(intent.getStringExtra(SearchManager.QUERY), true)
            }

            NotificationService.NOTIF_TRUST_REQUEST_MULTIPLE -> {
                mDisposable.add(mAccountService.currentAccountSubject
                    .switchMap { it.getPendingSubject() }
                    .firstOrError()
                    .observeOn(DeviceUtils.uiScheduler)
                    .subscribe({ list ->
                        val (feeds, invitations) = list.partition { it.effectiveMode == Conversation.Mode.Feed }
                        val isFeed = invitations.isEmpty()
                        mBinding?.conversationFilters?.check(
                            if (isFeed) R.id.filter_feeds else R.id.filter_all
                        )
                        pendingRequests = if (isFeed) feeds else invitations
                        pendingFeeds = isFeed
                        if (pendingRequests.isNotEmpty()) expandPendingView()
                    }, { error ->
                        Log.e(TAG, "Unable to open pending requests", error)
                    })
                )
            }

            else -> {}
        }
    }

    override fun showDonationReminder(show: Boolean) {
        mBinding?.appBar?.let {
            TransitionManager.beginDelayedTransition(it, AutoTransition())
        }
        mBinding?.donationCard?.donationCard?.isVisible = show
        mBinding?.fragmentContainer?.getFragment<SmartListFragment>()?.scrollToTop()
    }

    override fun onQueryTextSubmit(query: String?) = true

    override fun onQueryTextChange(newText: String): Boolean {
        querySubject.onNext(newText)
        return true
    }

    private fun goToQRFragment() {
        // Hide keyboard to prevent any glitch.
        val accountUri = mAccountService.currentAccount?.uri ?: return
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(requireView().windowToken, 0)

        QRCodeFragment.newInstance(
            QRCodeFragment.MODE_SHARE or QRCodeFragment.MODE_SCAN,
            QRCodeFragment.MODE_SCAN,
            Uri.fromString(accountUri)
        ).show(parentFragmentManager, QRCodeFragment.TAG)

        collapseSearchActionView()
    }

    private fun startNewSwarm() {
        ContactPickerFragment().show(parentFragmentManager, ContactPickerFragment.TAG)
        collapseSearchActionView()
    }

    private fun startNewFeed() {
        val account = mAccountService.currentAccount ?: return
        if (account.isSip || parentFragmentManager.findFragmentByTag(CreateFeedFragment.TAG) != null)
            return
        CreateFeedFragment.newInstance(account.accountId)
            .show(parentFragmentManager, CreateFeedFragment.TAG)
        collapseSearchActionView()
    }

    private fun expandSearchActionView() {
        mBinding?.searchView?.show()
    }

    fun collapseSearchActionView() {
        mBinding?.searchView?.hide()
    }

    /** Prevent appbar to be collapsed by direct scroll. */
    private fun disableAppBarScroll() {
        (mBinding!!.appBar.layoutParams as CoordinatorLayout.LayoutParams).behavior =
            AppBarLayout.Behavior().apply {
                setDragCallback(object : DragCallback() {
                    override fun canDrag(appBarLayout: AppBarLayout): Boolean = false
                })
            }
    }

    companion object {
        private val TAG = HomeFragment::class.simpleName!!
        private const val STATE_CONVERSATION_FILTER = "conversation_filter"
    }

}