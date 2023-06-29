/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
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
import cx.ring.databinding.FragHomeBinding

@AndroidEntryPoint
class HomeFragment : BaseSupportFragment<HomePresenter, HomeView>(),
    SearchView.OnQueryTextListener, HomeView {

    private var mBinding: FragHomeBinding? = null
    private var pagerContent: Fragment? = null
    private var mHasConversationBadge = false
    private var mHasPendingBadge = false
    private val mDisposable = CompositeDisposable()
    private var mSearchView: SearchView? = null
    private val searchDisposable = CompositeDisposable()
    private var searchAdapter: SmartListAdapter? = null
    private var pendingAdapter: SmartListAdapter? = null
    private val querySubject = BehaviorSubject.createDefault("")
    private val debouncedQuery = querySubject.debounce { item ->
        if (item.isEmpty()) Observable.empty()
        else Observable.timer(350, TimeUnit.MILLISECONDS)
    }.distinctUntilChanged()

    @Inject
    lateinit
    var mAccountService: AccountService

    @Inject
    lateinit
    var mConversationFacade: ConversationFacade

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
    ): View =
        FragHomeBinding.inflate(inflater, container, false).let { binding ->

            // Connect SearchView buttons:
            // - QRCode
            // - New Swarm
            binding.qrCode.setOnClickListener {
                presenter.clickQRSearch()
            }
            binding.newSwarm.setOnClickListener {
                presenter.clickNewSwarm()
            }

            // SearchBar is composed of:
            // - Account selection (navigation)
            // - Search bar (search for swarms or for new contacts)
            // - Menu (for settings, about jami)
            binding.searchBar.setNavigationOnClickListener { // Account selection
                val accounts = mAccountService.observableAccountList.blockingFirst()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Select Account")
                    .setAdapter(
                        AccountAdapter(
                            requireContext(),
                            accounts,
                            mDisposable, mAccountService, mConversationFacade
                        )
                    ) { _, index ->
                        if (index >= accounts.size)
                            startActivity(Intent(activity, AccountWizardActivity::class.java))
                        else
                            mAccountService.currentAccount = accounts[index]
                    }
                    .show()
            }
            binding.searchView.editText.addTextChangedListener { // Search bar
                querySubject.onNext(it.toString())
            }
            // Inflate Menu and connect it
            binding.searchBar.inflateMenu(R.menu.smartlist_menu)
            binding.searchBar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_account_settings -> {
                        (requireActivity() as HomeActivity).goToAccountSettings()
                    }

                    R.id.menu_advanced_settings -> {
                        (requireActivity() as HomeActivity).goToAdvancedSettings()
                    }

                    R.id.menu_about -> {
                        (requireActivity() as HomeActivity).goToAbout()
                    }
                }
                true
            }

            // Setup search result adapter
            binding.searchResult.adapter =
                SmartListAdapter(
                    null,
                    object : SmartListViewHolder.SmartListListeners {
                        override fun onItemClick(item: Conversation) {
                            collapseSearchActionView()
                            (requireActivity() as HomeActivity).startConversation(
                                item.accountId,
                                item.uri
                            )
                        }

                        override fun onItemLongClick(item: Conversation) {}
                    },
                    mConversationFacade,
                    mDisposable
                ).apply { searchAdapter = this }
            // Setup search result list
            binding.searchResult.setHasFixedSize(true)
            binding.searchResult.layoutManager =
                LinearLayoutManager(requireContext()).apply { orientation = RecyclerView.VERTICAL }

            binding.searchView.addTransitionListener { _, previousState, newState ->
                // When using the SearchView, we have to :
                // - Manage back press
                // - Manage search results

                if (newState === TransitionState.SHOWN) { // Shown
                    // Hide AppBar and SmartList to avoid weird animation
                    binding.fragmentContainer.isVisible = false
                    binding.appBar.isVisible = false

                    searchBackPressedCallback.isEnabled = true
                } else if (previousState === TransitionState.SHOWN) { // Hiding
                    // Make SmartList and appbar visible again
                    binding.fragmentContainer.isVisible = true
                    binding.appBar.isVisible = true

                    searchBackPressedCallback.isEnabled = false
                }

                if (newState === TransitionState.HIDDEN) { // Hidden
                    // Hide floating button to avoid weird animation
                    binding.newSwarmFab.isVisible = true

                    searchDisposable.clear()
                } else if (previousState === TransitionState.HIDDEN) { // Showing
                    // Make floating button visible again
                    binding.newSwarmFab.isVisible = false

                    searchDisposable.add(
                        mConversationFacade.getSearchResults(
                            mConversationFacade.currentAccountSubject, debouncedQuery
                        )
                            .observeOn(DeviceUtils.uiScheduler)
                            .subscribe { searchAdapter?.update(it) }
                    )
                }
            }

            // Setup floating button.
            binding.newSwarmFab.setOnClickListener { expandSearchActionView() }

            // Setup invitation card adapter.
            binding.invitationCard.pendingList.adapter =
                SmartListAdapter(
                    null,
                    object : SmartListViewHolder.SmartListListeners {
                        override fun onItemClick(item: Conversation) {
                            (requireActivity() as HomeActivity).startConversation(
                                item.accountId,
                                item.uri
                            )
                        }

                        override fun onItemLongClick(item: Conversation) {}
                    },
                    mConversationFacade,
                    mDisposable
                ).apply { pendingAdapter = this }
            // Setup invitation card
            binding.invitationCard.invitationGroup.setOnClickListener {
                expandPendingView()
            }
            binding.invitationCard.pendingToolbar // Return to search
                .setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }

            mBinding = binding
            binding.root
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

        // Disable possibility to scroll the appbar.
        (binding.appBarContainer.layoutParams as AppBarLayout.LayoutParams).scrollFlags = 0
        // Adapt the margins of the invitation card.
        requireContext().resources.getDimensionPixelSize(R.dimen.bottom_sheet_radius).let {
            (binding.invitationCard.invitationGroup.layoutParams as ViewGroup.MarginLayoutParams)
                .setMargins(it, it, it, it)
        }
        // Enable to possibility to scroll the invitation pending list.
        (binding.appBar.layoutParams as CoordinatorLayout.LayoutParams).behavior = null

        // Hide everything unneeded.
        binding.searchBar.isVisible = false
        binding.invitationCard.invitationSummary.isVisible = false
        binding.newSwarmFab.isVisible = false

        // Display pending list.
        binding.invitationCard.pendingListGroup.isVisible = true

        // Enable back press.
        conversationBackPressedCallback.isEnabled = true
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

        // Make the invitation card wrap content (not take all space available anymore).
        binding.appBar.updateLayoutParams {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        // Enable possibility to scroll the appbar.
        (binding.appBarContainer.layoutParams as AppBarLayout.LayoutParams).scrollFlags =
            SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS or
                    SCROLL_FLAG_SNAP or SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
        // Adapt the margins of the invitation card.
        requireContext().resources.getDimensionPixelSize(R.dimen.bottom_sheet_radius).let {
            (binding.invitationCard.invitationGroup.layoutParams as ViewGroup.MarginLayoutParams)
                .setMargins(it, 0, it, 0)
        }
        // Disable possibility to scroll the invitation pending list.
        (binding.appBar.layoutParams as CoordinatorLayout.LayoutParams).behavior =
            AppBarLayout.Behavior()

        // Show everything needed.
        binding.searchBar.isVisible = true
        binding.invitationCard.invitationSummary.isVisible = true
        binding.newSwarmFab.isVisible = true

        // Hide pending list.
        binding.invitationCard.pendingListGroup.isVisible = false

        // Disable back press.
        conversationBackPressedCallback.isEnabled = false
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pagerContent = mBinding!!.fragmentContainer.getFragment()

        // FragmentContainer will contains the smartlist when it will be loaded.
        // When it will be loaded, we subscribe to the list to override the scroll listener.
        // Thanks to that, we can hide and expand the FAB when the user scroll the list.
        mDisposable.add(
            (pagerContent as SmartListFragment).listAvailable
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe {
                    it.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            val canScrollUp =
                                recyclerView.canScrollVertically(-1)
                            val isExtended = mBinding!!.newSwarmFab.isExtended
                            if (dy > 0 && isExtended) { // Going down
                                mBinding!!.newSwarmFab.shrink()
                            } else if ((dy < 0 || !canScrollUp) && !isExtended) { // Going up
                                mBinding!!.newSwarmFab.extend()
                            }
                        }
                    })
                }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pagerContent = null
        mDisposable.dispose()
        mBinding = null
    }

    override fun onStart() {
        super.onStart()
        mDisposable.add(searchDisposable)
        activity?.intent?.let { handleIntent(it) }

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
            .subscribe { count -> setBadge(TAB_INVITATIONS, count.second, count.first) }
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
                            profile.first.isRegistered
                        ),
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            6f,
                            resources.displayMetrics
                        ).toInt()
                    )
            }
        )
    }

    override fun onStop() {
        super.onStop()
        mDisposable.clear()
    }

    /**
     * Set a badge to display how many invitations are pending.
     */
    private fun setBadge(
        menuId: Int,
        conversations: List<Conversation>,
        snip: List<ConversationItemViewModel>,
    ) {
        Log.w(TAG, "setBadge ${conversations.size}")
        val binding = mBinding ?: return

        binding.invitationCard.invitationBadge.text = conversations.size.toString()
        binding.invitationCard.invitationGroup.isVisible = true
        binding.invitationCard.invitationReceivedTxt.text =
            snip.joinToString(", ") { it.title }

        if (menuId == TAB_INVITATIONS) {
            pendingAdapter?.update(conversations)
            if (conversations.isEmpty()) { // No pending invitations = no badge
                binding.invitationCard.invitationGroup.isVisible = false
            } else {
                binding.invitationCard.invitationBadge.text = conversations.size.toString()
                binding.invitationCard.invitationGroup.isVisible = true
                binding.invitationCard.invitationReceivedTxt.text =
                    snip.joinToString(", ") { it.title }
            }
        }
    }

    fun handleIntent(intent: Intent) {
        val searchView = mSearchView ?: return
        when (intent.action) {
            Intent.ACTION_CALL -> {
                expandSearchActionView()
                searchView.setQuery(intent.dataString, true)
            }

            Intent.ACTION_DIAL -> {
                expandSearchActionView()
                searchView.setQuery(intent.dataString, false)
            }

            Intent.ACTION_SEARCH -> {
                expandSearchActionView()
                searchView.setQuery(intent.getStringExtra(SearchManager.QUERY), true)
            }

            else -> {}
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        querySubject.onNext(newText)
        return true
    }

    override fun goToQRFragment() {
        val qrCodeFragment = QRCodeFragment.newInstance(QRCodeFragment.INDEX_SCAN)
        qrCodeFragment.show(parentFragmentManager, QRCodeFragment.TAG)
        collapseSearchActionView()
    }

    override fun startNewSwarm() {
        ContactPickerFragment().show(parentFragmentManager, ContactPickerFragment.TAG)
        collapseSearchActionView()
    }

    private fun expandSearchActionView(): Boolean {
        mBinding ?: return false
        mBinding!!.searchView.show()
        return true
    }

    fun collapseSearchActionView() {
        mBinding ?: return
        mBinding!!.searchView.hide()
    }

    companion object {
        private val TAG = HomeFragment::class.simpleName!!
        const val TAB_CONVERSATIONS = 0
        const val TAB_INVITATIONS = 1
    }


}