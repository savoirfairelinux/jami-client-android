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
import net.jami.model.AccountCreationModel

class JamiAccountCreationFragment : Fragment() {
    private var mBinding: FragAccJamiCreateBinding? = null
    private var mCurrentFragment: Fragment? = null
    private val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (mCurrentFragment is ProfileCreationFragment) {
                    val fragment = mCurrentFragment as ProfileCreationFragment
                    (activity as AccountWizardActivity?)?.profileCreated(fragment.model!!, false)
                    return
                }
                mBinding!!.pager.currentItem = mBinding!!.pager.currentItem - 1
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
        return FragAccJamiCreateBinding.inflate(inflater, container, false).apply { mBinding = this }.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        retainInstance = true
        val pagerAdapter = ScreenSlidePagerAdapter(childFragmentManager)
        mBinding!!.pager.adapter = pagerAdapter
        mBinding!!.pager.disableScroll(true)
        mBinding!!.pager.offscreenPageLimit = 1
        mBinding!!.indicator.setupWithViewPager(mBinding!!.pager, true)
        mBinding!!.pager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int,positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                mCurrentFragment = pagerAdapter.getRegisteredFragment(position)
                val enable = mCurrentFragment is JamiAccountPasswordFragment || mCurrentFragment is ProfileCreationFragment
                onBackPressedCallback.isEnabled = enable
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        val tabStrip = mBinding!!.indicator.getChildAt(0) as LinearLayout
        for (i in 0 until tabStrip.childCount) {
            tabStrip.getChildAt(i).setOnTouchListener { v, event -> true }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    fun scrollPagerFragment(accountCreationModel: AccountCreationModel?) {
        if (accountCreationModel == null) {
            mBinding!!.pager.currentItem = mBinding!!.pager.currentItem - 1
            return
        }
        mBinding!!.pager.currentItem = mBinding!!.pager.currentItem + 1
        for (fragment in childFragmentManager.fragments) {
            if (fragment is JamiAccountPasswordFragment) {
                fragment.setUsername(accountCreationModel.username)
            }
        }
    }

    private class ScreenSlidePagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        var mRegisteredFragments = SparseArray<Fragment>()
        private var mCurrentPosition = -1
        override fun getItem(position: Int): Fragment {
            val accountViewModel = AccountCreationModelImpl()
            return when (position) {
                0 -> JamiAccountUsernameFragment.newInstance(accountViewModel)
                1 -> JamiAccountPasswordFragment.newInstance(accountViewModel)
                2 -> ProfileCreationFragment.newInstance(accountViewModel)
                else -> throw IllegalStateException()
            }
        }

        override fun setPrimaryItem(container: ViewGroup, position: Int, o: Any) {
            super.setPrimaryItem(container, position, o)
            if (position != mCurrentPosition && container is WizardViewPager) {
                val fragment = o as Fragment
                if (fragment.view != null) {
                    mCurrentPosition = position
                    container.measureCurrentView(fragment.view)
                }
            }
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as Fragment
            mRegisteredFragments.put(position, fragment)
            return super.instantiateItem(container, position)
        }

        override fun destroyItem(container: ViewGroup, position: Int, o: Any) {
            mRegisteredFragments.remove(position)
            super.destroyItem(container, position, o)
        }

        override fun getCount(): Int {
            return NUM_PAGES
        }

        fun getRegisteredFragment(position: Int): Fragment? {
            return mRegisteredFragments[position]
        }
    }

    companion object {
        private const val NUM_PAGES = 3
    }
}