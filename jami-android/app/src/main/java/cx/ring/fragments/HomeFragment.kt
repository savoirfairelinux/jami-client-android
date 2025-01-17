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
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
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
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.Behavior.DragCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import net.jami.smartlist.ConversationItemViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.google.android.material.search.SearchView.TransitionState
import com.google.android.material.shape.MaterialShapeDrawable
import cx.ring.databinding.FragHomeBinding
import cx.ring.utils.ActionHelper.openJamiDonateWebPage
import io.reactivex.rxjava3.disposables.Disposable
import net.jami.model.Uri
import net.jami.services.HardwareService
import net.jami.services.NotificationService
import javax.inject.Singleton

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
    private val querySubject = BehaviorSubject.createDefault("")
    private val debouncedQuery = querySubject.debounce { item ->
        if (item.isEmpty()) Observable.empty()
        else Observable.timer(350, TimeUnit.MILLISECONDS)
    }.distinctUntilChanged()

    @Inject
    lateinit var mAccountService: AccountService

    @Inject
    @Singleton
    lateinit var mHardwareService: HardwareService

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragHomeBinding.inflate(inflater, container, false).apply {

        qrCode.setOnClickListener { goToQRFragment() }
        newSwarm.setOnClickListener { startNewSwarm() }

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
            mSmartListFragment?.getRecyclerView()?.setPadding(
                    0,
                    appBar.height - DeviceUtils.getStatusBarHeight(requireContext()),
                    0, 0
            )
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
                newSwarmFab.isVisible = false
                startSearch()
            }
        }

        // Setup floating button.
        newSwarmFab.setOnClickListener {
            mHardwareService.isSpeakerphoneOn()
        /* expandSearchActionView() */ }

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

        // Setup invitation card
        invitationCard.invitationGroup.setOnClickListener {
            expandPendingView()
        }

        // Return to search
        invitationCard.pendingToolbar.setNavigationOnClickListener { collapsePendingView() }

        mBinding = this
    }.root

    private fun displayConversationRequestDialog(conversation: Conversation) {
        if (conversation.request!!.mode == Conversation.Mode.OneToOne)
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
                .setItems(R.array.swarm_request_group_actions) { _, which ->
                    when (which) {
                        0 -> mConversationFacade.acceptRequest(conversation)
                        1 -> mConversationFacade
                            .discardRequest(conversation.accountId, conversation.uri)
                    }
                }.show()
    }

    private fun startSearch() {
        searchDisposable?.dispose()
        val disposable = mConversationFacade.getSearchResults(debouncedQuery)
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { searchAdapter?.update(it) }
        searchDisposable = disposable
        mDisposable.add(disposable)
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

        // Transitions to animate the changes
        // Make the search bar slide down
        TransitionManager.beginDelayedTransition(binding.searchBar, Slide())
        // Make the invitation card expand.
        TransitionManager.beginDelayedTransition(
            binding.invitationCard.invitationGroup,
            ChangeBounds().setInterpolator(DecelerateInterpolator())
        )

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

        // Animate back to search
        // Make the search bar slide up
        TransitionManager.beginDelayedTransition(
            binding.searchBar,
            Slide().setInterpolator(DecelerateInterpolator())
        )
        // Make the invitation card collapse.
        TransitionManager.beginDelayedTransition(
            binding.appBar,
            ChangeBounds().setInterpolator(DecelerateInterpolator())
        )
        // Make the invitation card text fade in.
        TransitionManager.beginDelayedTransition(
            binding.invitationCard.invitationSummary,
            Fade()
        )

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
        mSmartListFragment = mBinding!!.fragmentContainer.getFragment()

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
        super.onDestroyView()
        mSmartListFragment = null
        pendingAdapter = null
        searchAdapter = null
        mBinding = null
        mDisposable.dispose()
    }

    override fun onStart() {
        super.onStart()
        activity?.intent?.let { handleIntent(it) }

        // Enable account settings menu option when an account is loaded
        mDisposable.add(mAccountService.currentAccountSubject
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe {
                mBinding?.newSwarm?.isVisible = !it.isSip
                mBinding?.searchBar?.menu?.findItem(R.id.menu_account_settings)?.isEnabled = true
            }
        )

        // Subscribe on invitation pending list to show a badge counter
        mDisposable.add(mAccountService
            .currentAccountSubject
            .switchMap { account ->
                account.getPendingSubject()
                    .switchMap { list ->
                        Log.w(TAG, "setBadge getPendingSubject ${list.size}")
                        if (list.isEmpty()) Observable.just(Pair(emptyList(), emptyList()))
                        else mConversationFacade.observeConversations(
                            account, list.take(3), false
                        ).map { Pair(it, list) }
                    }
            }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { count ->
                // Collapse pending view if there is no more pending invitations
                if(count.first.isEmpty()) {
                    collapsePendingView()
                }
                setInvitationBadge(count.second, count.first)
            }
        )

        // Subscribe on current account to display avatar on navigation (searchbar)
        mDisposable.add(mAccountService
            .currentAccountSubject
            .switchMap { mAccountService.getObservableAccountProfile(it.accountId) }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { profile ->
                mBinding ?: return@subscribe
                mBinding!!.searchBar.navigationIcon =
                    BitmapUtils.withPadding(
                        AvatarDrawable.build(
                            mBinding!!.root.context,
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
            }
        )

        if (mBinding!!.searchView.isShowing)
            startSearch()
    }

    override fun onStop() {
        super.onStop()
        mDisposable.clear()
    }

    /**
     * Set a badge to display how many invitations are pending.
     */
    private fun setInvitationBadge(conversations: List<Conversation>, snip: List<ConversationItemViewModel>) {
        val binding = mBinding ?: return
        pendingAdapter?.update(conversations)
        val hasInvites = conversations.isNotEmpty()
        binding.invitationCard.invitationGroup.isVisible = hasInvites
        if (hasInvites) {
            binding.invitationCard.invitationBadge.text = conversations.size.toString()
            binding.invitationCard.invitationReceivedTxt.text = snip.joinToString(", ") { it.title }
        }
        updateAppBarLayoutBottomPadding(hasInvites)
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
                expandPendingView()
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
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(requireView().windowToken, 0)

        QRCodeFragment.newInstance(
            QRCodeFragment.MODE_SHARE or QRCodeFragment.MODE_SCAN,
            QRCodeFragment.MODE_SCAN,
            Uri.fromString(mAccountService.currentAccount?.uri!!)
        ).show(parentFragmentManager, QRCodeFragment.TAG)

        collapseSearchActionView()
    }

    private fun startNewSwarm() {
        ContactPickerFragment().show(parentFragmentManager, ContactPickerFragment.TAG)
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
    }

}