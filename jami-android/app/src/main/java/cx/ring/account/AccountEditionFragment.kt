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
package cx.ring.account

import android.os.Bundle
import android.view.*
import androidx.annotation.StringRes
import androidx.fragment.app.*
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import cx.ring.R
import cx.ring.fragments.BlockListFragment
import cx.ring.databinding.FragAccountSettingsBinding
import cx.ring.fragments.AdvancedAccountFragment
import cx.ring.fragments.GeneralAccountFragment
import cx.ring.fragments.MediaPreferenceFragment
import cx.ring.fragments.SecurityAccountFragment
import cx.ring.mvp.BaseSupportFragment
import cx.ring.settings.extensionssettings.ExtensionsListSettingsFragment
import cx.ring.utils.ActionHelper.openJamiDonateWebPage
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.AccountEditionPresenter
import net.jami.account.AccountEditionView
import net.jami.utils.DonationUtils

@AndroidEntryPoint
class AccountEditionFragment : BaseSupportFragment<AccountEditionPresenter, AccountEditionView>(),
    AccountEditionView {
    private var mBinding: FragAccountSettingsBinding? = null

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
        super.onViewCreated(view, savedInstanceState)
        initViewPager(requireArguments().getString(ACCOUNT_ID_KEY)!!)
    }

    override fun initViewPager(accountId: String) {
        mBinding?.apply {
            pager.offscreenPageLimit = 4
            pager.adapter = PreferencesPagerAdapter(this@AccountEditionFragment, accountId)
            TabLayoutMediator(slidingTabs, pager) { tab, position ->
                tab.text = context?.getString(getSIPPanelTitle(position))
            }.attach()
        }

        val existingFragment = childFragmentManager.findFragmentByTag(BlockListFragment.TAG) as? BlockListFragment
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

    private class PreferencesPagerAdapter(
        f: Fragment,
        private val accountId: String
    ) : FragmentStateAdapter(f) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> GeneralAccountFragment.newInstance(accountId)
            1 -> MediaPreferenceFragment.newInstance(accountId)
            2 -> fragmentWithBundle(AdvancedAccountFragment())
            3 -> fragmentWithBundle(SecurityAccountFragment())
            4 -> fragmentWithBundle(ExtensionsListSettingsFragment())
            else -> throw IllegalArgumentException()
        }

        private fun fragmentWithBundle(result: Fragment): Fragment = result.apply {
            arguments = Bundle().apply { putString(ACCOUNT_ID_KEY, accountId) }
        }
    }

    companion object {
        val TAG = AccountEditionFragment::class.simpleName!!
        val ACCOUNT_ID_KEY = AccountEditionFragment::class.qualifiedName + "accountId"
        val ACCOUNT_HAS_PASSWORD_KEY = AccountEditionFragment::class.qualifiedName + "hasPassword"
        private const val SCROLL_DIRECTION_UP = -1

        @StringRes
        private fun getSIPPanelTitle(position: Int): Int = when (position) {
            0 -> R.string.account_preferences_basic_tab
            1 -> R.string.account_preferences_media_tab
            2 -> R.string.account_preferences_advanced_tab
            3 -> R.string.account_preferences_security_tab
            4 -> R.string.account_preferences_extension_tab
            else -> -1
        }
    }
}