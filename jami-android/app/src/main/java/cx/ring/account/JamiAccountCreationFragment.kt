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

import android.content.Context
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import cx.ring.databinding.FragAccJamiCreateBinding
import cx.ring.views.WizardViewPager

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
            val pagerAdapter = ScreenSlidePagerAdapter(childFragmentManager)
            pager.apply {
                adapter = pagerAdapter
                disableScroll(true)
                offscreenPageLimit = 1
                addOnPageChangeListener(object : OnPageChangeListener {
                    override fun onPageScrolled(position: Int,positionOffset: Float, positionOffsetPixels: Int) {}

                    override fun onPageSelected(position: Int) {
                        currentFragment = pagerAdapter.getRegisteredFragment(position)
                        val enable = currentFragment is JamiAccountPasswordFragment || currentFragment is ProfileCreationFragment
                        onBackPressedCallback.isEnabled = enable
                    }

                    override fun onPageScrollStateChanged(state: Int) {}
                })
            }
            indicator.setupWithViewPager(pager, true)
            val tabStrip = indicator.getChildAt(0) as LinearLayout
            for (i in 0 until tabStrip.childCount) {
                tabStrip.getChildAt(i).setOnTouchListener { v, event -> true }
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

    private class ScreenSlidePagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        val registeredFragments = SparseArray<Fragment>()
        private var currentPosition = -1
        override fun getItem(position: Int): Fragment = when (position) {
            0 -> JamiAccountUsernameFragment()
            1 -> JamiAccountPasswordFragment()
            2 -> ProfileCreationFragment()
            else -> throw IllegalStateException()
        }

        override fun getCount(): Int = 3

        override fun setPrimaryItem(container: ViewGroup, position: Int, o: Any) {
            super.setPrimaryItem(container, position, o)
            if (position != currentPosition && container is WizardViewPager) {
                val fragment = o as Fragment
                if (fragment.view != null) {
                    currentPosition = position
                    container.measureCurrentView(fragment.view)
                }
            }
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as Fragment
            registeredFragments.put(position, fragment)
            return fragment
        }

        override fun destroyItem(container: ViewGroup, position: Int, o: Any) {
            registeredFragments.remove(position)
            super.destroyItem(container, position, o)
        }

        fun getRegisteredFragment(position: Int): Fragment? = registeredFragments[position]
    }
}