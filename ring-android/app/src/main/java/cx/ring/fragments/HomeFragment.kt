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

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
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
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.contactrequests.ContactRequestsPresenter
import net.jami.contactrequests.ContactRequestsView
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import net.jami.services.NotificationService
import net.jami.smartlist.ConversationItemViewModel
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment: Fragment(), TabLayout.OnTabSelectedListener, SearchView.OnQueryTextListener {
    private var mBinding: FragHomeBinding? = null
    private var pagerContent: Fragment? = null
    private var fConversation: ConversationFragment? = null
    private var mPagerAdapter: HomeFragment.ScreenSlidePagerAdapter? = null
    private var mHasConversationBadge = false
    private var mHasPendingBadge = false
    private var mAccountAdapter: AccountSpinnerAdapter? = null
    private val mDisposable = CompositeDisposable()

    @Inject
    lateinit
    var mContactService: ContactService

    @Inject
    lateinit
    var mAccountService: AccountService

    @Inject
    lateinit
    var mConversationFacade: ConversationFacade

    @Inject
    lateinit
    var mNotificationService: NotificationService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragHomeBinding.inflate(inflater, container, false).apply {
            setHasOptionsMenu(true)
            mPagerAdapter = ScreenSlidePagerAdapter(requireActivity())

            pager.adapter = mPagerAdapter
            pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    tabLayout.getTabAt(position)!!.select()
                    pagerContent = mPagerAdapter!!.fragments[position] as Fragment
                }
            })

            mBinding = this
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding!!.tabLayout.addOnTabSelectedListener(this)
        (activity as AppCompatActivity?)!!.setSupportActionBar(mBinding!!.toolbar)
        mBinding!!.spinnerToolbar.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pagerContent = null
        mDisposable.dispose()
        mBinding = null
    }

    override fun onStart() {
        super.onStart()
        mDisposable.add(mAccountService
            .currentAccountSubject
            .switchMap { obj -> obj.unreadPending }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { count -> setBadge(TAB_INVITATIONS, count) })
        mDisposable.add(mAccountService
            .currentAccountSubject
            .switchMap { obj -> obj.unreadConversations }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                    count -> setBadge(TAB_CONVERSATIONS, count)
            })
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
//                    if (pagerContent is SmartListFragment) {
//                        showProfileInfo()
//                    }
                }) { e -> Log.e(HomeActivity.TAG, "Error loading account list !", e) })

        // Select first conversation in tablet mode
        if (DeviceUtils.isTablet(requireActivity())) {
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
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.smartlist_menu, menu)
        val searchMenuItem = menu.findItem(R.id.menu_contact_search)
        val dialpadMenuItem = menu.findItem(R.id.menu_contact_dial)
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
//                dialpadMenuItem.isVisible = false
//                mbin!!.newconvFab.show()
//                setOverflowMenuVisible(menu, true)
//                binding!!.qrCode.visibility = View.GONE
//                binding!!.newGroup.visibility = View.GONE
                return true
            }

            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
//                dialpadMenuItem.isVisible = true
//                binding!!.newconvFab.hide()
//                setOverflowMenuVisible(menu, false)
//                binding!!.qrCode.visibility = View.VISIBLE
//                binding!!.newGroup.visibility = if (presenter.isAddGroupEnabled()) View.VISIBLE else View.GONE
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
//        mSearchMenuItem = searchMenuItem
//        mDialpadMenuItem = dialpadMenuItem
//        mSearchView = searchView
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
//            R.id.menu_contact_search -> {
//                mSearchView?.inputType = (EditorInfo.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
//                return false
//            }
//            R.id.menu_contact_dial -> {
//                val searchView = mSearchView ?: return false
//                if (searchView.inputType == EditorInfo.TYPE_CLASS_PHONE) {
//                    searchView.inputType = EditorInfo.TYPE_CLASS_TEXT
//                    mDialpadMenuItem?.setIcon(R.drawable.baseline_dialpad_24)
//                } else {
//                    searchView.inputType = EditorInfo.TYPE_CLASS_PHONE
//                    mDialpadMenuItem?.setIcon(R.drawable.baseline_keyboard_24)
//                }
//                return true
//            }
            R.id.menu_account_settings -> {
                (requireActivity() as HomeActivity).goToAccountSettings()
                return true
            }
            R.id.menu_advanced_settings -> {
                (requireActivity() as HomeActivity).goToAdvancedSettings()
                return true
            }
            R.id.menu_about -> {
                (requireActivity() as HomeActivity).goToAbout()
                return true
            }
            else -> return false
        }
    }

    override fun onStop() {
        super.onStop()
        mDisposable.clear()
    }

    fun goToHome() {
        mBinding!!.tabLayout.getTabAt(TAB_CONVERSATIONS)!!.select()
        pagerContent = SmartListFragment()
    }

    private fun setBadge(menuId: Int, number: Int) {
        val tab = mBinding!!.tabLayout.getTabAt(menuId)
        if (number == 0) {
            tab!!.removeBadge()
            if (menuId == TAB_CONVERSATIONS) mHasConversationBadge = false else mHasPendingBadge = false
            if (pagerContent is ContactRequestsFragment) goToHome()
        } else {
            tab!!.orCreateBadge.number = number
            mBinding!!.tabLayout.isVisible = true
            if (menuId == TAB_CONVERSATIONS) mHasConversationBadge = true else mHasPendingBadge = true
        }
        mBinding!!.tabLayout.isVisible = mHasPendingBadge
        mBinding!!.pager.isUserInputEnabled = mHasConversationBadge || mHasPendingBadge
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        mBinding?.pager?.setCurrentItem(tab!!.position, true)
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {}

    override fun onTabReselected(tab: TabLayout.Tab?) {}

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        val fragments = listOf(SmartListFragment(), ContactRequestsFragment())

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment {
            return fragments[position] as Fragment
        }
    }

    companion object {
        private val TAG = HomeFragment::class.simpleName!!
        private const val CONVERSATIONS_CATEGORY = "conversations"
        private const val TAB_CONVERSATIONS = 0
        private const val TAB_INVITATIONS = 1
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        TODO("Not yet implemented")
    }

}