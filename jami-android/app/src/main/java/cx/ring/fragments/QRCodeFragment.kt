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
package cx.ring.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
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
import net.jami.model.Uri

class QRCodeFragment : BottomSheetDialogFragment() {
    private var mBinding: FragQrcodeBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val mode = arguments?.getInt(ARG_MODE) ?: 0
        val startMode = arguments?.getInt(ARG_START_MODE) ?: 0
        val contactUri = Uri.fromString(arguments?.getString(ARG_CONTACT_URI) ?: "")

        return FragQrcodeBinding.inflate(inflater, container, false).apply {
            when (mode) { // Check on mode
                MODE_SCAN, MODE_SHARE -> {}
                MODE_SCAN or MODE_SHARE ->
                    if (startMode == 0) throw IllegalArgumentException("Start mode not set")

                0 -> throw IllegalArgumentException("Mode not set")
                else -> throw IllegalArgumentException("Unknown mode: $mode")
            }

            SectionsPagerAdapter(this@QRCodeFragment).apply {
                // Add fragments based on mode
                if (mode and MODE_SHARE != 0) {
                    ShareFragment.newInstance(contactUri).apply {
                        addFragment(this, getTabTitle(INDEX_SHARE))
                    }
                }
                if (mode and MODE_SCAN != 0) addFragment(ScanFragment(), getTabTitle(INDEX_SCAN))
                viewPager.adapter = this

                // If multi-mode, setup the tabs. If not, hide them.
                if (mode and MODE_SHARE != 0 && mode and MODE_SCAN != 0) {
                    viewPager.currentItem = when (startMode) {
                        MODE_SHARE -> INDEX_SHARE
                        MODE_SCAN -> INDEX_SCAN
                        else -> INDEX_SCAN
                    }
                    val tabs: TabLayout = tabs
                    TabLayoutMediator(tabs, viewPager) { tab, position ->
                        tab.text = getTabTitle(position)
                    }.attach()
                } else tabs.visibility = View.GONE
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
                v: View, left: Int, top: Int, right: Int, bottom: Int,
                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int,
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

    internal class SectionsPagerAdapter(val fragment:  Fragment) :
        FragmentStateAdapter(fragment) {
        private val mFragmentList: MutableList<Fragment> = ArrayList()
        private val mFragmentTitleList: MutableList<String> = ArrayList()

        fun getTabTitle(position: Int): String =
            fragment.resources.getString(TAB_TITLES[position])

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
        const val ARG_MODE = "mode"
        const val ARG_START_MODE = "start_mode"
        const val ARG_CONTACT_URI = "contact_uri"
        const val INDEX_SHARE = 0
        const val INDEX_SCAN = 1

        const val MODE_SCAN = 1
        const val MODE_SHARE = 2

        @IntDef(flag = true, value = [MODE_SCAN, MODE_SHARE])
        @Retention(AnnotationRetention.SOURCE)
        annotation class QRCodeMode

        /**
         * Create a new instance of QRCodeFragment.
         * @param mode The mode of the fragment.
         * @param startPage The start page of the fragment. Can be null if mode is not multi-mode.
         * @param contactUri The contact URI to share. Can be null if mode is not share.
         */
        fun newInstance(
            @QRCodeMode mode: Int,
            @QRCodeMode startPage: Int? = null,
            contactUri: Uri? = null,
        ): QRCodeFragment {
            val fragment = QRCodeFragment()
            val args = Bundle()
            args.putInt(ARG_MODE, mode)
            if (startPage != null) args.putInt(ARG_START_MODE, startPage)
            if (contactUri != null) args.putString(ARG_CONTACT_URI, contactUri.uri)
            fragment.arguments = args
            return fragment
        }
    }
}