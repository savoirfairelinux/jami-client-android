/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *          Loïc Siret <loic.siret@savoirfairelinux.com>
 *          AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
package cx.ring.account

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.fragment.app.*
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.contactrequests.BlockListFragment
import cx.ring.databinding.FragAccountSettingsBinding
import cx.ring.fragments.AdvancedAccountFragment
import cx.ring.fragments.GeneralAccountFragment
import cx.ring.fragments.MediaPreferenceFragment
import cx.ring.fragments.SecurityAccountFragment
import cx.ring.mvp.BaseSupportFragment
import cx.ring.settings.pluginssettings.PluginsListSettingsFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.AccountEditionPresenter
import net.jami.account.AccountEditionView

@AndroidEntryPoint
class AccountEditionFragment : BaseSupportFragment<AccountEditionPresenter, AccountEditionView>(),
    AccountEditionView, OnScrollChangedListener {
    private var mBinding: FragAccountSettingsBinding? = null
    private var mIsVisible = false
    private var mAccountId: String? = null
    private var mAccountIsJami = false

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            mIsVisible = false
            if (activity is HomeActivity) (activity as HomeActivity).setToolbarOutlineState(true)
            if (mBinding!!.fragmentContainer.visibility != View.VISIBLE) {
                toggleView(mAccountId, mAccountIsJami)
                return
            }
            val summaryFragment = childFragmentManager.findFragmentByTag(JamiAccountSummaryFragment.TAG) as JamiAccountSummaryFragment?
            if (!childFragmentManager.popBackStackImmediate()) {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragAccountSettingsBinding.inflate(inflater, container, false).apply { mBinding = this }.root

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onViewCreated(view, savedInstanceState)
        mAccountId = requireArguments().getString(ACCOUNT_ID_KEY)
        (this.activity as HomeActivity?)?.setToolbarTitle(R.string.menu_item_account_settings)
        mBinding!!.fragmentContainer.viewTreeObserver.addOnScrollChangedListener(this)
        presenter.init(mAccountId!!)
    }

    override fun displaySummary(accountId: String) {
        toggleView(accountId, true)
        val fragmentManager = childFragmentManager
        val existingFragment = fragmentManager.findFragmentByTag(JamiAccountSummaryFragment.TAG)
        val args = Bundle().apply { putString(ACCOUNT_ID_KEY, accountId) }
        if (existingFragment == null) {
            val fragment = JamiAccountSummaryFragment().apply {
                arguments = args
            }
            fragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragment, JamiAccountSummaryFragment.TAG)
                .commitAllowingStateLoss()
        } else {
            if (existingFragment is JamiAccountSummaryFragment) {
                if (!existingFragment.isStateSaved) existingFragment.arguments = args
                existingFragment.setAccount(accountId)
            }
        }
    }

    override fun displaySIPView(accountId: String) {
        toggleView(accountId, false)
    }

    override fun initViewPager(accountId: String, isJami: Boolean) {
        mBinding?.apply {
            pager.offscreenPageLimit = 4
            pager.adapter = PreferencesPagerAdapter(childFragmentManager, requireContext(), accountId, isJami)
            slidingTabs.setupWithViewPager(pager)
        }
        val existingFragment = childFragmentManager.findFragmentByTag(BlockListFragment.TAG) as BlockListFragment?
        if (existingFragment != null) {
            if (!existingFragment.isStateSaved)
                existingFragment.arguments = Bundle().apply { putString(ACCOUNT_ID_KEY, accountId) }
            existingFragment.setAccount(accountId)
        }
    }

    override fun goToBlackList(accountId: String) {
        val blockListFragment = BlockListFragment()
        blockListFragment.arguments = Bundle().apply { putString(ACCOUNT_ID_KEY, accountId) }
        childFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .addToBackStack(BlockListFragment.TAG)
            .replace(R.id.fragment_container, blockListFragment, BlockListFragment.TAG)
            .commit()
        mBinding?.apply {
            slidingTabs.visibility = View.GONE
            pager.visibility = View.GONE
            fragmentContainer.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    override fun onResume() {
        super.onResume()
        presenter.bindView(this)
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun toggleView(accountId: String?, isJami: Boolean) {
        mAccountId = accountId
        mAccountIsJami = isJami
        mBinding?.apply {
            slidingTabs.visibility = if (isJami) View.GONE else View.VISIBLE
            pager.visibility = if (isJami) View.GONE else View.VISIBLE
            fragmentContainer.visibility = if (isJami) View.VISIBLE else View.GONE
        }
    }

    override fun exit() {
        activity?.onBackPressed()
    }

    private class PreferencesPagerAdapter constructor(
        fm: FragmentManager,
        private val mContext: Context,
        private val accountId: String,
        private val isJamiAccount: Boolean
    ) : FragmentStatePagerAdapter(fm) {
        override fun getCount(): Int = if (isJamiAccount) 3 else 4

        override fun getItem(position: Int): Fragment {
            return if (isJamiAccount) getJamiPanel(position) else getSIPPanel(position)
        }

        override fun getPageTitle(position: Int): CharSequence = mContext.getString(if (isJamiAccount) getRingPanelTitle(position) else getSIPPanelTitle(position))

        private fun getJamiPanel(position: Int): Fragment {
            return when (position) {
                0 -> fragmentWithBundle(GeneralAccountFragment())
                1 -> fragmentWithBundle(MediaPreferenceFragment())
                2 -> fragmentWithBundle(AdvancedAccountFragment())
                3 -> fragmentWithBundle(PluginsListSettingsFragment())
                else -> throw IllegalArgumentException()
            }
        }

        private fun getSIPPanel(position: Int): Fragment {
            return when (position) {
                0 -> GeneralAccountFragment.newInstance(accountId)
                1 -> MediaPreferenceFragment.newInstance(accountId)
                2 -> fragmentWithBundle(AdvancedAccountFragment())
                3 -> fragmentWithBundle(SecurityAccountFragment())
                4 -> fragmentWithBundle(PluginsListSettingsFragment())
                else -> throw IllegalArgumentException()
            }
        }

        private fun fragmentWithBundle(result: Fragment): Fragment = result.apply {
            arguments = Bundle().apply { putString(ACCOUNT_ID_KEY, accountId) }
        }

        companion object {
            @StringRes
            private fun getRingPanelTitle(position: Int): Int {
                return when (position) {
                    0 -> R.string.account_preferences_basic_tab
                    1 -> R.string.account_preferences_media_tab
                    2 -> R.string.account_preferences_advanced_tab
                    3 -> R.string.account_preference_plugin_tab
                    else -> -1
                }
            }

            @StringRes
            private fun getSIPPanelTitle(position: Int): Int {
                return when (position) {
                    0 -> R.string.account_preferences_basic_tab
                    1 -> R.string.account_preferences_media_tab
                    2 -> R.string.account_preferences_advanced_tab
                    3 -> R.string.account_preferences_security_tab
                    4 -> R.string.account_preference_plugin_tab
                    else -> -1
                }
            }
        }
    }

    override fun onScrollChanged() {
        setupElevation()
    }

    private fun setupElevation() {
        val binding = mBinding ?: return
        if (!mIsVisible)
            return
        val activity: FragmentActivity = activity as? HomeActivity ?: return
        val ll = binding.pager.getChildAt(binding.pager.currentItem) as LinearLayout
        val rv = (ll.getChildAt(0) as FrameLayout).getChildAt(0) as RecyclerView
        val homeActivity = activity as HomeActivity
        if (rv.canScrollVertically(SCROLL_DIRECTION_UP)) {
            binding.slidingTabs.elevation = binding.slidingTabs.resources.getDimension(R.dimen.toolbar_elevation)
            homeActivity.setToolbarElevation(true)
            homeActivity.setToolbarOutlineState(false)
        } else {
            binding.slidingTabs.elevation = 0f
            homeActivity.setToolbarElevation(false)
            homeActivity.setToolbarOutlineState(true)
        }
    }

    companion object {
        private val TAG = AccountEditionFragment::class.simpleName
        val ACCOUNT_ID_KEY = AccountEditionFragment::class.qualifiedName + "accountId"
        val ACCOUNT_HAS_PASSWORD_KEY = AccountEditionFragment::class.qualifiedName + "hasPassword"
        private const val SCROLL_DIRECTION_UP = -1
    }
}