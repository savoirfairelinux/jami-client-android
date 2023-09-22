/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package cx.ring.account

import android.os.Bundle
import android.view.*
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.fragment.app.*
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import cx.ring.R
import cx.ring.contactrequests.BlockListFragment
import cx.ring.databinding.FragAccountSettingsBinding
import cx.ring.fragments.AdvancedAccountFragment
import cx.ring.fragments.GeneralAccountFragment
import cx.ring.fragments.MediaPreferenceFragment
import cx.ring.fragments.SecurityAccountFragment
import cx.ring.mvp.BaseSupportFragment
import cx.ring.settings.pluginssettings.PluginsListSettingsFragment
import cx.ring.utils.ActionHelper.openJamiDonateWebPage
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.AccountEditionPresenter
import net.jami.account.AccountEditionView
import net.jami.utils.DonationUtils

@AndroidEntryPoint
class AccountEditionFragment : BaseSupportFragment<AccountEditionPresenter, AccountEditionView>(),
    AccountEditionView, OnScrollChangedListener {
    private var mBinding: FragAccountSettingsBinding? = null
    private var mAccountId: String? = null
    private var mAccountIsJami = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragAccountSettingsBinding.inflate(inflater, container, false).apply {
            toolbar.setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }

            if (DonationUtils.isDonationPeriod()) {
                donateButton.visibility = View.VISIBLE
                donateButton.setOnClickListener {
                    openJamiDonateWebPage(requireContext())
                }
            }

            mBinding = this
        }.root

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onViewCreated(view, savedInstanceState)

        mAccountId = requireArguments().getString(ACCOUNT_ID_KEY)
        mBinding!!.fragmentContainer.viewTreeObserver.addOnScrollChangedListener(this)
        presenter.init(mAccountId!!)

        mBinding?.apply {
            TabLayoutMediator(slidingTabs, pager){ tab, position ->
                tab.text = context?.getString(
                        if (mAccountIsJami) getJamiPanelTitle(position)
                        else getSIPPanelTitle(position))
            }.attach()
        }
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
            pager.adapter = PreferencesPagerAdapter(this@AccountEditionFragment, accountId, isJami)
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
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    private class PreferencesPagerAdapter(
        f: Fragment,
        private val accountId: String,
        private val isJamiAccount: Boolean
    ) : FragmentStateAdapter(f) {
        override fun getItemCount(): Int = if (isJamiAccount) 3 else 4

        override fun createFragment(position: Int): Fragment =
            if (isJamiAccount) getJamiPanel(position) else getSIPPanel(position)

        private fun getJamiPanel(position: Int): Fragment = when (position) {
            0 -> fragmentWithBundle(GeneralAccountFragment())
            1 -> fragmentWithBundle(MediaPreferenceFragment())
            2 -> fragmentWithBundle(AdvancedAccountFragment())
            3 -> fragmentWithBundle(PluginsListSettingsFragment())
            else -> throw IllegalArgumentException()
        }

        private fun getSIPPanel(position: Int): Fragment = when (position) {
            0 -> GeneralAccountFragment.newInstance(accountId)
            1 -> MediaPreferenceFragment.newInstance(accountId)
            2 -> fragmentWithBundle(AdvancedAccountFragment())
            3 -> fragmentWithBundle(SecurityAccountFragment())
            4 -> fragmentWithBundle(PluginsListSettingsFragment())
            else -> throw IllegalArgumentException()
        }

        private fun fragmentWithBundle(result: Fragment): Fragment = result.apply {
            arguments = Bundle().apply { putString(ACCOUNT_ID_KEY, accountId) }
        }
    }

    override fun onScrollChanged() {
        setupElevation()
    }

    private fun setupElevation() {
        val binding = mBinding ?: return
        val ll = binding.pager.getChildAt(binding.pager.currentItem) as? LinearLayout ?: return
        val rv = (ll.getChildAt(0) as FrameLayout).getChildAt(0) as RecyclerView
        if (rv.canScrollVertically(SCROLL_DIRECTION_UP)) {
            binding.slidingTabs.elevation = binding.slidingTabs.resources.getDimension(R.dimen.toolbar_elevation)
        } else {
            binding.slidingTabs.elevation = 0f
        }
    }

    companion object {
        private val TAG = AccountEditionFragment::class.simpleName
        val ACCOUNT_ID_KEY = AccountEditionFragment::class.qualifiedName + "accountId"
        val ACCOUNT_HAS_PASSWORD_KEY = AccountEditionFragment::class.qualifiedName + "hasPassword"
        private const val SCROLL_DIRECTION_UP = -1

        @StringRes
        private fun getJamiPanelTitle(position: Int): Int = when (position) {
            0 -> R.string.account_preferences_basic_tab
            1 -> R.string.account_preferences_media_tab
            2 -> R.string.account_preferences_advanced_tab
            3 -> R.string.account_preference_plugin_tab
            else -> -1
        }

        @StringRes
        private fun getSIPPanelTitle(position: Int): Int = when (position) {
            0 -> R.string.account_preferences_basic_tab
            1 -> R.string.account_preferences_media_tab
            2 -> R.string.account_preferences_advanced_tab
            3 -> R.string.account_preferences_security_tab
            4 -> R.string.account_preference_plugin_tab
            else -> -1
        }
    }
}