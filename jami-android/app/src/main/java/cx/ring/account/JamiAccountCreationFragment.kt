/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import cx.ring.databinding.FragAccJamiCreateBinding

class JamiAccountCreationFragment : Fragment() {
    private var binding: FragAccJamiCreateBinding? = null
    private var currentFragment: Fragment? = null
    private val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (currentFragment is ProfileCreationFragment) {
                    (activity as AccountWizardActivity?)?.profileCreated( false)
                    return
                }
                binding?.apply { pager.currentItem = pager.currentItem - 1 }
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View =
        FragAccJamiCreateBinding.inflate(inflater, container, false).apply {
            val pagerAdapter = ScreenSlidePagerAdapter(this@JamiAccountCreationFragment)
            pager.apply {
                adapter = pagerAdapter
                isUserInputEnabled = false // Disable possibility to user to slide between pages.
                offscreenPageLimit = 1

                // Make indicator moving when sliding between pages.
                // Disable clickable setting to not allow user to skip steps.
                TabLayoutMediator(indicator, pager) { indicator, _ ->
                    indicator.view.isClickable = false
                }.attach()
            }
            binding = this
        }.root

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    fun scrollPagerFragment() {
        binding?.apply { pager.currentItem = pager.currentItem + 1 }
    }

    private class ScreenSlidePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment){
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> JamiAccountUsernameFragment()
            1 -> JamiAccountPasswordFragment()
            2 -> ProfileCreationFragment()
            else -> throw IllegalStateException()
        }
    }
}