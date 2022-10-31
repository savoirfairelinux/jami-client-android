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
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import cx.ring.R
import cx.ring.account.AccountWizardActivity
import cx.ring.client.AccountSpinnerAdapter
import cx.ring.client.HomeActivity
import cx.ring.contactrequests.ContactRequestsFragment
import cx.ring.databinding.FragHomeBinding
import cx.ring.mvp.BaseSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.home.HomePresenter
import net.jami.home.HomeView
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import javax.inject.Inject

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
    private var mSearchMenuItem: MenuItem? = null
    private var mDialPadMenuItem: MenuItem? = null
    private var mSmartListFragment: Fragment? = null

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
            mPagerAdapter = ScreenSlidePagerAdapter(requireActivity())
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
                    pagerContent = mPagerAdapter!!.fragments[position]
                    conversationBackPressedCallback.isEnabled = position != TAB_CONVERSATIONS
                }
            })
            binding.tabLayout.addOnTabSelectedListener(this)
            binding.spinnerToolbar.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val adapter = mAccountAdapter ?: return
                    val type = adapter.getItemViewType(position)
                    if (type == AccountSpinnerAdapter.TYPE_ACCOUNT) {
                        adapter.getItem(position)?.let { account ->
                            mAccountService.currentAccount = account
                            //                showAccountStatus(frameContent is AccountEditionFragment && !account.isSip)
                        }
                    } else {
                        val intent = Intent(activity, AccountWizardActivity::class.java)
                        startActivity(intent)
                        mBinding!!.spinnerToolbar.setSelection(0)
                    }
                }
                override fun onNothingSelected(arg0: AdapterView<*>?) {}
            }
            binding.toolbar.inflateMenu(R.menu.smartlist_menu)
            mBinding = binding
            binding.root
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSmartListFragment = mPagerAdapter!!.fragments[TAB_CONVERSATIONS] as SmartListFragment
        val menu = mBinding!!.toolbar.menu
        val searchMenuItem = menu.findItem(R.id.menu_contact_search)
        val dialpadMenuItem = menu.findItem(R.id.menu_contact_dial)
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                dialpadMenuItem.isVisible = false
                (mSmartListFragment as SmartListFragment?)?.showFab(true)
                setOverflowMenuVisible(menu, true)
                mBinding!!.qrCode.visibility = View.GONE
                mBinding!!.newGroup.visibility = View.GONE
                searchBackPressedCallback.isEnabled = false
                return true
            }

            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                dialpadMenuItem.isVisible = true
                (mSmartListFragment as SmartListFragment).showFab(false)
                setOverflowMenuVisible(menu, false)
                mBinding!!.qrCode.visibility = View.VISIBLE
                mBinding!!.newGroup.visibility = if (presenter.isAddGroupEnabled()) View.VISIBLE else View.GONE
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
        searchView.imeOptions = EditorInfo.IME_ACTION_GO
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            searchView.findViewById<EditText>(R.id.search_src_text)
                ?.setAutofillHints(View.AUTOFILL_HINT_USERNAME)
        }
        mSearchMenuItem = searchMenuItem
        mDialPadMenuItem = dialpadMenuItem
        mSearchView = searchView
        mBinding!!.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_contact_search -> {
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
                }
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
        activity?.intent?.let { handleIntent(it) }
        mDisposable.add(mAccountService
            .currentAccountSubject
            .switchMap { obj -> obj.unreadPending }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { count -> setBadge(TAB_INVITATIONS, count) })
        mDisposable.add(mAccountService
            .currentAccountSubject
            .switchMap { obj -> obj.unreadConversations }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { count -> setBadge(TAB_CONVERSATIONS, count) })
        mDisposable.add(
            mAccountService.observableAccountList
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ accounts ->
                    mAccountAdapter?.apply {
                        clear()
                        addAll(accounts)
                        notifyDataSetChanged()
                        if (accounts.isNotEmpty()) {
                            mBinding!!.spinnerToolbar.setSelection(0)
                        }
                    } ?: run {
                        AccountSpinnerAdapter(activity!!, ArrayList(accounts), mDisposable, mAccountService, mConversationFacade).apply {
                            mAccountAdapter = this
                            setNotifyOnChange(false)
                            mBinding?.spinnerToolbar?.adapter = this
                        }
                    }
                }) { e -> Log.e(HomeActivity.TAG, "Error loading account list !", e) })

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

    fun presentForAccount(accountId: String) {
        val fragment = mPagerAdapter!!.fragments[TAB_INVITATIONS]
        (fragment as ContactRequestsFragment).presentForAccount(accountId)
    }

    fun goToHome() {
        mBinding!!.tabLayout.getTabAt(TAB_CONVERSATIONS)!!.select()
        pagerContent = mPagerAdapter!!.fragments[TAB_CONVERSATIONS]
    }

    private fun setBadge(menuId: Int, number: Int) {
        val tab = mBinding!!.tabLayout.getTabAt(menuId)
        if (number == 0) {
            tab!!.removeBadge()
            if (menuId == TAB_CONVERSATIONS) mHasConversationBadge = false else mHasPendingBadge = false
            if (!mHasPendingBadge && pagerContent is ContactRequestsFragment) goToHome()
        } else {
            tab!!.orCreateBadge.number = number
            mBinding!!.tabLayout.isVisible = true
            if (menuId == TAB_CONVERSATIONS) mHasConversationBadge = true else mHasPendingBadge = true
        }
        mBinding!!.tabLayout.isVisible = mHasPendingBadge
        mBinding!!.pager.isUserInputEnabled = mHasConversationBadge || mHasPendingBadge
    }

    fun handleIntent(intent: Intent) {
        val searchView = mSearchView ?: return
        when (intent.action) {
            Intent.ACTION_CALL -> {
                mSearchMenuItem?.expandActionView()
                searchView.setQuery(intent.dataString, true)
            }
            Intent.ACTION_DIAL -> {
                mSearchMenuItem?.expandActionView()
                searchView.setQuery(intent.dataString, false)
            }
            Intent.ACTION_SEARCH -> {
                mSearchMenuItem?.expandActionView()
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
        (mSmartListFragment as SmartListFragment).searchQueryTextChanged(newText)
        return true
    }

    override fun goToQRFragment() {
        val qrCodeFragment = QRCodeFragment.newInstance(QRCodeFragment.INDEX_SCAN)
        qrCodeFragment.show(parentFragmentManager, QRCodeFragment.TAG)
        mSearchMenuItem!!.collapseActionView()
    }

    override fun startNewGroup() {
        ContactPickerFragment().show(parentFragmentManager, ContactPickerFragment.TAG)
        mSearchMenuItem!!.collapseActionView()
    }

    fun isSearchActionViewExpanded(): Boolean {
        return mSearchMenuItem!!.isActionViewExpanded
    }

    fun collapseSearchActionView() {
        mSearchMenuItem!!.collapseActionView()
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        val fragments: List<Fragment> = listOf(SmartListFragment(), ContactRequestsFragment())

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]
    }

    companion object {
        private val TAG = HomeFragment::class.simpleName!!
        const val TAB_CONVERSATIONS = 0
        const val TAB_INVITATIONS = 1
    }

}