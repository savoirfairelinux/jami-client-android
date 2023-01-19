/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchView.TransitionState
import com.google.android.material.tabs.TabLayout
import cx.ring.R
import cx.ring.account.AccountWizardActivity
import cx.ring.adapters.SmartListAdapter
import cx.ring.client.AccountAdapter
import cx.ring.client.AccountSpinnerAdapter
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject


fun ViewPager2.findCurrentFragment(fragmentManager: FragmentManager): Fragment? =
    fragmentManager.findFragmentByTag("f$currentItem")

fun ViewPager2.findFragmentAtPosition(
    fragmentManager: FragmentManager,
    position: Int
): Fragment? = fragmentManager.findFragmentByTag("f$position")

@AndroidEntryPoint
class HomeFragment : BaseSupportFragment<HomePresenter, HomeView>(),
        TabLayout.OnTabSelectedListener, SearchView.OnQueryTextListener, HomeView {

    private var mBinding: FragHomeBinding? = null
    private var pagerContent: Fragment? = null
    private var mPagerAdapter: ScreenSlidePagerAdapter? = null
    private var mHasConversationBadge = false
    private var mHasPendingBadge = false
    private var mAccountAdapter: AccountSpinnerAdapter? = null
    private val mDisposable = CompositeDisposable()
    private var mSearchView: SearchView? = null
    private var mDialPadMenuItem: MenuItem? = null

    private val searchDisposable = CompositeDisposable()
    private var searchAdapter: SmartListAdapter? = null
    private val querySubject = BehaviorSubject.createDefault("")
    private val debouncedQuery = querySubject.debounce{ item ->
        if (item.isEmpty()) Observable.empty() else Observable.timer(350, TimeUnit.MILLISECONDS)
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
            setPagerPosition(TAB_CONVERSATIONS)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.let {
            it.addCallback(this, conversationBackPressedCallback)
            it.addCallback(this, searchBackPressedCallback)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragHomeBinding.inflate(inflater, container, false).let { binding ->
            mPagerAdapter = ScreenSlidePagerAdapter(this)
            binding.qrCode.setOnClickListener {
                presenter.clickQRSearch()
            }
            binding.newGroup.setOnClickListener{
                presenter.clickNewGroup()
            }
            binding.pager.adapter = mPagerAdapter
            binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    binding.tabLayout.getTabAt(position)!!.select()
                    pagerContent = binding.pager.findFragmentAtPosition(childFragmentManager, position)
                    conversationBackPressedCallback.isEnabled = position != TAB_CONVERSATIONS
                }
            })
            binding.tabLayout.addOnTabSelectedListener(this)
            binding.toolbar.setNavigationOnClickListener {
                val accounts = mAccountService.observableAccountList.blockingFirst()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Select Account")
                    .setAdapter(
                        AccountAdapter(requireContext(),
                        accounts,
                        mDisposable, mAccountService, mConversationFacade)
                    ) { _, index ->
                        if (index >= accounts.size)
                            startActivity(Intent(activity, AccountWizardActivity::class.java))
                        else
                            mAccountService.currentAccount = accounts[index]
                    }
                    .show()
            }
            binding.toolbar.inflateMenu(R.menu.smartlist_menu)
            binding.searchView.addTransitionListener { _, previousState, newState ->
                if (newState === TransitionState.SHOWN) {
                    searchBackPressedCallback.isEnabled = true
                } else if(previousState === TransitionState.SHOWN) {
                    searchBackPressedCallback.isEnabled = false
                }
                if (newState === TransitionState.HIDDEN) {
                    (pagerContent as? SmartListFragment?)?.showFab(true)
                    searchDisposable.clear()
                } else if (previousState === TransitionState.HIDDEN) {
                    (pagerContent as? SmartListFragment?)?.showFab(false)
                    searchDisposable.add(mConversationFacade.getSearchResults(mConversationFacade.currentAccountSubject, debouncedQuery)
                        .observeOn(DeviceUtils.uiScheduler)
                        .subscribe { searchAdapter!!.update(it) })
                }
            }
            binding.searchResult.adapter = SmartListAdapter(null, object : SmartListViewHolder.SmartListListeners {
                override fun onItemClick(item: Conversation) {
                    collapseSearchActionView()
                    (requireActivity() as HomeActivity).startConversation(item.accountId, item.uri)
                }
                override fun onItemLongClick(item: Conversation) {
                }
            }, mConversationFacade, mDisposable).apply {
                searchAdapter = this
            }
            binding.searchResult.setHasFixedSize(true)
            binding.searchResult.layoutManager = LinearLayoutManager(requireContext()).apply {
                orientation = RecyclerView.VERTICAL
            }
            binding.searchView.editText.addTextChangedListener {
                querySubject.onNext(it.toString())
            }
            /*binding.searchView.editText.setOnEditorActionListener { textView, i, keyEvent ->
                if (i == EditorInfo.IME_ACTION_GO) {

                    //(requireActivity() as HomeActivity).startConversation(item.accountId, item.uri)
                    true
                } else false
            }*/
            binding.searchView.editText.imeOptions = EditorInfo.IME_ACTION_GO
            mBinding = binding
            binding.root
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menu = mBinding!!.toolbar.menu
        val dialpadMenuItem = menu.findItem(R.id.menu_contact_dial)
/*
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                dialpadMenuItem.isVisible = false
                (pagerContent as? SmartListFragment?)?.showFab(true)
                setOverflowMenuVisible(menu, true)
                mBinding!!.qrCode.visibility = View.GONE
                mBinding!!.newGroup.visibility = View.GONE
                searchBackPressedCallback.isEnabled = false
                return true
            }

            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                dialpadMenuItem.isVisible = true
                (pagerContent as? SmartListFragment?)?.showFab(false)
                setOverflowMenuVisible(menu, false)
                mBinding!!.qrCode.visibility = View.VISIBLE
                mBinding!!.newGroup.visibility = View.VISIBLE
                searchBackPressedCallback.isEnabled = true
                return true
            }
        })
        val searchView = searchMenuItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        searchView.queryHint = getString(R.string.searchbar_hint)
        searchView.layoutParams = Toolbar.LayoutParams(
            Toolbar.LayoutParams.WRAP_CONTENT,
            Toolbar.LayoutParams.MATCH_PARENT
        )
        searchView.imeOptions = EditorInfo.IME_ACTION_GO*/
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            searchView.findViewById<EditText>(R.id.search_src_text)
                ?.setAutofillHints(View.AUTOFILL_HINT_USERNAME)
        }*/
        mDialPadMenuItem = dialpadMenuItem
        mBinding!!.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                /*R.id.menu_contact_search -> {
                    mSearchView?.inputType = (EditorInfo.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
                }
                R.id.menu_contact_dial -> {
                    val searchView = mSearchView
                    if (searchView?.inputType == EditorInfo.TYPE_CLASS_PHONE) {
                        searchView.inputType = EditorInfo.TYPE_CLASS_TEXT
                        mDialPadMenuItem?.setIcon(R.drawable.baseline_dialpad_24)
                    } else {
                        searchView?.inputType = EditorInfo.TYPE_CLASS_PHONE
                        mDialPadMenuItem?.setIcon(R.drawable.baseline_keyboard_24)
                    }
                }*/
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
        mDisposable.add(mAccountService
            .currentAccountSubject
            .switchMap { obj -> obj.unreadPending }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { count -> setBadge(TAB_INVITATIONS, count) })
        mDisposable.add(mAccountService
            .currentAccountSubject
            .switchMap { obj -> obj.unreadConversations }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { count -> setBadge(TAB_CONVERSATIONS, count) })

        mDisposable.add(
            mAccountService.currentAccountSubject
                .switchMap { mAccountService.getObservableAccountProfile(it.accountId) }
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe { profile ->
                    mAccountService
                    mBinding!!.toolbar.navigationIcon =
                        BitmapUtils.withPadding(
                            AvatarDrawable.build(mBinding!!.root.context, profile.first, profile.second, true, profile.first.isRegistered),
                            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics).toInt()
                    )
                })
        // Select first conversation in tablet mode
        /*if (DeviceUtils.isTablet(requireActivity())) {
            val intent = activity?.intent
            val uri = intent?.data
            if ((intent == null || uri == null) && fConversation == null) {
                val smartList: Observable<List<Observable<ConversationItemViewModel>>> =
                    if (mBinding?.pager!!.currentItem == TAB_CONVERSATIONS)
                        mConversationFacade.getSmartList(false)
                    else mConversationFacade.pendingList
                mDisposable.add(smartList
                    .filter { list -> list.isNotEmpty() }
                    .map { list -> list[0].firstOrError() }
                    .firstElement()
                    .flatMapSingle { e -> e }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { element ->
                        (activity as HomeActivity).startConversation(element.accountId, element.uri)
                    })
            }
        }*/
    }

    private fun setOverflowMenuVisible(menu: Menu?, visible: Boolean) {
        menu?.findItem(R.id.menu_overflow)?.isVisible = visible
    }

    override fun onStop() {
        super.onStop()
        mDisposable.clear()
    }

    fun isInvitationTabOpen(): Boolean {
        return mBinding!!.pager.visibility == View.VISIBLE && mBinding!!.pager.currentItem == TAB_INVITATIONS
    }

    fun setPagerPosition(position: Int) {
        mBinding!!.pager.currentItem = position
    }


    fun goToHome() {
        mBinding!!.tabLayout.getTabAt(TAB_CONVERSATIONS)!!.select()
        pagerContent = mBinding!!.pager.findCurrentFragment(childFragmentManager)
    }

    private fun setBadge(menuId: Int, number: Int) {
        val binding = mBinding ?: return
        val tab = binding.tabLayout.getTabAt(menuId) ?: return
        if (number == 0) {
            tab.removeBadge()
            if (menuId == TAB_CONVERSATIONS) mHasConversationBadge = false else mHasPendingBadge = false
            if (!mHasPendingBadge && pagerContent is ContactRequestsFragment) goToHome()
        } else {
            tab.orCreateBadge.number = number
            if (menuId == TAB_CONVERSATIONS) mHasConversationBadge = true else mHasPendingBadge = true
        }
        binding.tabLayout.isVisible = mHasPendingBadge
        mPagerAdapter!!.enableRequests(mHasPendingBadge)
        binding.pager.isUserInputEnabled = mHasPendingBadge
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

    override fun onTabSelected(tab: TabLayout.Tab) {
        mBinding?.pager?.setCurrentItem(tab.position, true)
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {}

    override fun onTabReselected(tab: TabLayout.Tab?) {}

    override fun onQueryTextSubmit(query: String?): Boolean {

        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        //(pagerContent as? SmartListFragment?)?.searchQueryTextChanged(newText)
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

    private inner class ScreenSlidePagerAdapter(f: Fragment) : FragmentStateAdapter(f) {
        var hasRequests = false
        override fun getItemCount(): Int = if (hasRequests) 2 else 1

        override fun createFragment(position: Int): Fragment = when(position) {
            TAB_CONVERSATIONS -> SmartListFragment()
            TAB_INVITATIONS -> ContactRequestsFragment()
            else -> throw IllegalArgumentException()
        }

        fun enableRequests(enable: Boolean) {
            if (enable == hasRequests) return
            hasRequests = enable
            if (enable)
                notifyItemInserted(TAB_INVITATIONS)
            else
                notifyItemRemoved(TAB_INVITATIONS)
        }
    }

    companion object {
        private val TAG = HomeFragment::class.simpleName!!
        const val TAB_CONVERSATIONS = 0
        const val TAB_INVITATIONS = 1
    }

}