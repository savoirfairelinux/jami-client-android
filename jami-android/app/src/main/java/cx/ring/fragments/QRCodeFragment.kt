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
package cx.ring.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cx.ring.R
import cx.ring.databinding.FragQrcodeBinding
import cx.ring.share.ScanFragment
import cx.ring.share.ShareFragment

class QRCodeFragment : BottomSheetDialogFragment() {
    private var mBinding: FragQrcodeBinding? = null
    private var mStartPageIndex = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val args = requireArguments()
        mStartPageIndex = args.getInt(ARG_START_PAGE_INDEX, 0)
        return FragQrcodeBinding.inflate(inflater, container, false).apply {
            viewPager.adapter = SectionsPagerAdapter(root.context, childFragmentManager)
            tabs.setupWithViewPager(viewPager)
            mBinding = this
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (mStartPageIndex != 0) {
            mBinding?.tabs?.getTabAt(mStartPageIndex)?.select()
        }
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

    internal class SectionsPagerAdapter(private val mContext: Context, fm: FragmentManager) :
        FragmentPagerAdapter(fm) {
        @StringRes
        private val TAB_TITLES = intArrayOf(R.string.tab_code, R.string.tab_scan)

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> ShareFragment()
                1 -> ScanFragment()
                else -> throw IllegalArgumentException()
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return mContext.resources.getString(TAB_TITLES[position])
        }

        override fun getCount(): Int {
            return TAB_TITLES.size
        }
    }

    override fun onResume() {
        super.onResume()
        addGlobalLayoutListener(requireView())
    }

    private fun addGlobalLayoutListener(view: View) {
        view.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                setPeekHeight(v.measuredHeight)
                v.removeOnLayoutChangeListener(this)
            }
        })
    }

    fun setPeekHeight(peekHeight: Int) {
        bottomSheetBehaviour?.peekHeight = peekHeight
    }

    private val bottomSheetBehaviour: BottomSheetBehavior<*>?
        get() {
            val layoutParams = (requireView().parent as View).layoutParams as CoordinatorLayout.LayoutParams
            val behavior = layoutParams.behavior
            return if (behavior is BottomSheetBehavior<*>) {
                behavior
            } else null
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