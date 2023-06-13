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
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchView.TransitionState
import cx.ring.R
import cx.ring.account.AccountWizardActivity
import cx.ring.adapters.SmartListAdapter
import cx.ring.client.AccountAdapter
import cx.ring.client.HomeActivity
import cx.ring.contactrequests.ContactRequestsFragment
import cx.ring.databinding.FragHomeBinding
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
    private val debouncedQuery = querySubject.debounce{ item ->
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
            // Connect buttons:
            // - QRCode
            // - New group
            binding.qrCode.setOnClickListener {
                presenter.clickQRSearch()
            }
            binding.newGroup.setOnClickListener {
                presenter.clickNewGroup()
            }

            // Toolbar is composed of:
            // - Account selection (navigation)
            // - Search bar
            // - Menu (for settings, about jami)
            binding.toolbar.setNavigationOnClickListener {
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
            binding.searchView.editText.addTextChangedListener {
                querySubject.onNext(it.toString())
            }
            binding.toolbar.inflateMenu(R.menu.smartlist_menu)
            binding.toolbar.setOnMenuItemClickListener {
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
                // Manage back press for search view
                if (newState === TransitionState.SHOWN) {
                    searchBackPressedCallback.isEnabled = true
                } else if (previousState === TransitionState.SHOWN) {
                    searchBackPressedCallback.isEnabled = false
                }

                // Manage search results
                if (newState === TransitionState.HIDDEN) {
                    (pagerContent as? SmartListFragment?)?.showFab(true)
                    searchDisposable.clear()
                } else if (previousState === TransitionState.HIDDEN) {
                    (pagerContent as? SmartListFragment?)?.showFab(false)
                    searchDisposable.add(
                        mConversationFacade.getSearchResults(
                            mConversationFacade.currentAccountSubject, debouncedQuery
                        )
                            .observeOn(DeviceUtils.uiScheduler)
                            .subscribe { searchAdapter?.update(it) }
                    )
                }
            }

            // Setup invitation card adapter.
            binding.invitationCard.pendingList.adapter =
                SmartListAdapter(
                    null,
                    object : SmartListViewHolder.SmartListListeners {
                        override fun onItemClick(item: Conversation) {
                            collapsePendingView()
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
                TransitionManager.beginDelayedTransition(binding.searchLayer, ChangeBounds())
                binding.toolbar.isVisible = false
                binding.invitationCard.invitationSummary.isVisible = false
                binding.invitationCard.pendingListGroup.isVisible = true

                it.updateLayoutParams {
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
                conversationBackPressedCallback.isEnabled = true
            }
            binding.invitationCard.pendingToolbar // Return to search
                .setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }

            mBinding = binding
            binding.root
        }

    fun collapsePendingView() {
        val binding = mBinding ?: return

        // Animate back to search
        TransitionManager.beginDelayedTransition(
            binding.searchLayer,
            AutoTransition().setInterpolator(DecelerateInterpolator())
        )

        binding.toolbar.isVisible = true
        binding.invitationCard.invitationSummary.isVisible = true
        binding.invitationCard.pendingListGroup.isVisible = false
        binding.invitationCard.invitationGroup.updateLayoutParams {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        conversationBackPressedCallback.isEnabled = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pagerContent = mBinding!!.fragmentContainer.getFragment()
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

        // Subscribe on current account to display avatar on navigation (toolbar)
        mDisposable.add(mAccountService
            .currentAccountSubject
                .switchMap { mAccountService.getObservableAccountProfile(it.accountId) }
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe { profile ->
                    mBinding!!.toolbar.navigationIcon =
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

    override fun startNewGroup() {
        ContactPickerFragment().show(parentFragmentManager, ContactPickerFragment.TAG)
        collapseSearchActionView()
    }

    fun expandSearchActionView(): Boolean {
        mBinding!!.searchView.show()
        return true
    }

    fun collapseSearchActionView() {
        mBinding!!.searchView.hide()
     }

    companion object {
        private val TAG = HomeFragment::class.simpleName!!
        const val TAB_CONVERSATIONS = 0
        const val TAB_INVITATIONS = 1
    }

}