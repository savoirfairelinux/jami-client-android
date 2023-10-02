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
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.activityViewModels
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import cx.ring.R
import cx.ring.databinding.FragAccJamiLinkBinding

class JamiLinkAccountFragment(private val isBackup:Boolean) : Fragment() {
    // isBackup changes the title of the page
    private val model: AccountCreationViewModel by activityViewModels()
    private var mBinding: FragAccJamiLinkBinding? = null
    private var mCurrentFragment: Fragment? = null

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (mCurrentFragment is ProfileCreationFragment) {
                    //val fragment = mCurrentFragment as ProfileCreationFragment
                    (activity as AccountWizardActivity).profileCreated(false)
                    return
                }
                mBinding!!.pager.currentItem = mBinding!!.pager.currentItem - 1
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragAccJamiLinkBinding.inflate(inflater, container, false).apply {
            // change the title of the page depending on the isBackup parameter
            title.setText(if (isBackup) R.string.account_link_archive_button else R.string.account_link_device)
            val pagerAdapter = ScreenSlidePagerAdapter(childFragmentManager)
            pager.apply {
                disableScroll(true)
                adapter = pagerAdapter
                addOnPageChangeListener(object : OnPageChangeListener {
                    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                    override fun onPageSelected(position: Int) {
                        mCurrentFragment = pagerAdapter.getRegisteredFragment(position)
                        onBackPressedCallback.isEnabled = mCurrentFragment is ProfileCreationFragment
                    }

                    override fun onPageScrollStateChanged(state: Int) {}
                })
            }
            mBinding = this
        }.root

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    fun scrollPagerFragment() {
        mBinding?.let { it.pager.currentItem = it.pager.currentItem + 1 }
        //mBinding!!.pager.currentItem = mBinding!!.pager.currentItem + 1
        /*if (accountCreationModel == null) {
            mBinding!!.pager.currentItem = mBinding!!.pager.currentItem - 1
            return
        }
        mBinding!!.pager.currentItem = mBinding!!.pager.currentItem + 1
        for (fragment in childFragmentManager.fragments) {
            if (fragment is JamiAccountPasswordFragment) {
                fragment.setUsername(accountCreationModel.username)
            }
        }*/
    }

    private class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm) {
        //var ringAccountViewModel: AccountCreationModelImpl = model as AccountCreationModelImpl
        var mRegisteredFragments = SparseArray<Fragment>()
        override fun getItem(position: Int): Fragment =
            when (position) {
                0 -> JamiLinkAccountPasswordFragment()
                1 -> ProfileCreationFragment()
                else -> throw IllegalArgumentException()
            }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as Fragment
            mRegisteredFragments.put(position, fragment)
            return super.instantiateItem(container, position)
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            mRegisteredFragments.remove(position)
            super.destroyItem(container, position, `object`)
        }

        override fun getCount(): Int {
            return NUM_PAGES
        }

        fun getRegisteredFragment(position: Int): Fragment {
            return mRegisteredFragments[position]
        }

    }

    companion object {
        val TAG = JamiLinkAccountFragment::class.simpleName!!
        private const val NUM_PAGES = 2
    }
}