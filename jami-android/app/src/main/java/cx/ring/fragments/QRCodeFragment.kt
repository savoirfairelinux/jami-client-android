/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
package cx.ring.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import cx.ring.R
import cx.ring.databinding.FragQrcodeBinding
import cx.ring.share.ScanFragment
import cx.ring.share.ShareFragment

class QRCodeFragment : BottomSheetDialogFragment() {
    private var mBinding: FragQrcodeBinding? = null
    private var mStartPageIndex = arguments?.getInt(ARG_START_PAGE_INDEX, 0) ?: 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return FragQrcodeBinding.inflate(inflater, container, false).apply {
            SectionsPagerAdapter(requireActivity()).apply {
                addFragment(ShareFragment(), getTabTitle(INDEX_CODE))
                addFragment(ScanFragment(), getTabTitle(INDEX_SCAN))
                viewPager.adapter = this
                viewPager.currentItem = mStartPageIndex
                val tabs: TabLayout = tabs
                TabLayoutMediator(tabs, viewPager) { tab, position ->
                    tab.text = getTabTitle(position)
                }.attach()
            }
            mBinding = this
        }.root
    }

    override fun onDestroyView() {
        mBinding = null
        super.onDestroyView()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as BottomSheetDialog).behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        return dialog
    }

    override fun onResume() {
        super.onResume()
        addGlobalLayoutListener(requireView())
    }

    private fun addGlobalLayoutListener(view: View) {
        view.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int,
            ) {
                setPeekHeight(v.measuredHeight)
                v.removeOnLayoutChangeListener(this)
            }
        })
    }

    private fun setPeekHeight(peekHeight: Int) {
        bottomSheetBehaviour?.peekHeight = peekHeight
    }

    private val bottomSheetBehaviour: BottomSheetBehavior<*>?
        get() {
            val layoutParams =
                (requireView().parent as View).layoutParams as CoordinatorLayout.LayoutParams
            val behavior = layoutParams.behavior
            return if (behavior is BottomSheetBehavior<*>) {
                behavior
            } else null
        }

    internal class SectionsPagerAdapter(val activity: FragmentActivity) :
        FragmentStateAdapter(activity) {
        private val mFragmentList: MutableList<Fragment> = ArrayList()
        private val mFragmentTitleList: MutableList<String> = ArrayList()

        fun getTabTitle(position: Int): String =
            activity.resources.getString(TAB_TITLES[position])

        fun addFragment(fragment: Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }

        override fun getItemCount() = mFragmentList.size

        override fun createFragment(position: Int) = mFragmentList[position]

        companion object {
            @StringRes
            private val TAB_TITLES = intArrayOf(R.string.tab_code, R.string.tab_scan)
        }
    }

    companion object {
        val TAG = QRCodeFragment::class.simpleName!!
        const val ARG_START_PAGE_INDEX = "start_page"
        const val INDEX_CODE = 0
        const val INDEX_SCAN = 1

        fun newInstance(startPage: Int): QRCodeFragment {
            val fragment = QRCodeFragment()
            val args = Bundle()
            args.putInt(ARG_START_PAGE_INDEX, startPage)
            fragment.arguments = args
            return fragment
        }
    }
}