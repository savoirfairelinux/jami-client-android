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

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import cx.ring.R
import cx.ring.account.pinInput.EditTextPinInputFragment
import cx.ring.account.pinInput.EditTextPinInputViewModel
import cx.ring.account.pinInput.QrCodePinInputFragment
import cx.ring.account.pinInput.QrCodePinInputViewModel
import cx.ring.databinding.FragAccJamiLinkBinding
import cx.ring.mvp.BaseSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.JamiLinkAccountPresenter
import net.jami.account.JamiLinkAccountView

@AndroidEntryPoint
class JamiLinkAccountFragment :
    BaseSupportFragment<JamiLinkAccountPresenter, JamiLinkAccountView>(),
    JamiLinkAccountView {
    private val model: AccountCreationViewModel by activityViewModels()
    private var binding: FragAccJamiLinkBinding? = null

    // the 2 view models connected to this fragment
    private val qrCodePinInputViewModel by lazy {
        ViewModelProvider(this)[QrCodePinInputViewModel::class.java]
    }
    private val editTextPinInputViewModel by lazy {
        ViewModelProvider(this)[EditTextPinInputViewModel::class.java]
    }

    private fun setLayout(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): View = FragAccJamiLinkBinding.inflate(inflater, container, false).apply {

        val adapter = SectionsPagerAdapter(this@JamiLinkAccountFragment)
        adapter.addFragment(QrCodePinInputFragment(), getString(R.string.connect_device_scanqr))
        adapter.addFragment(EditTextPinInputFragment(), getString(R.string.connect_device_enterPIN))
        pager.adapter = adapter
        pager.currentItem = 0
        TabLayoutMediator(tabLayout, pager) { tab, position ->
            tab.text = adapter.getTabTitle(position)
        }.attach()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                presenter.resetPin()
                // emit the pin again when switching tabs
                if (tab?.position == 0) {
                    qrCodePinInputViewModel.emitPinAgain()
                } else {
                    editTextPinInputViewModel.emitPinAgain()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        linkButton.setOnClickListener { presenter.linkClicked() }

        existingPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                presenter.passwordChanged(s.toString())
            }
        })

        binding = this
    }.root

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = setLayout(inflater, container)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        qrCodePinInputViewModel.init({ presenter.pinChanged(it) }, { presenter.resetPin() })
        editTextPinInputViewModel.init({ presenter.pinChanged(it) }, { presenter.resetPin() })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val rootView = (view as ViewGroup)
        rootView.removeAllViews()
        rootView.addView(setLayout(LayoutInflater.from(activity), rootView))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun initPresenter(presenter: JamiLinkAccountPresenter) {
        presenter.init(model.model)
    }

    override fun enableLinkButton(enable: Boolean) {
        binding!!.linkButton.isEnabled = enable
    }

    override fun showPin(show: Boolean) {
        val binding = binding ?: return
        binding.pager.visibility = if (show) View.VISIBLE else View.GONE
        binding.tabLayout.visibility = if (show) View.VISIBLE else View.GONE
        binding.linkButton.setText(if (show) R.string.account_link_device else R.string.account_link_archive_button)
    }

    override fun createAccount() {
        (activity as AccountWizardActivity?)?.createAccount()
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding!!.existingPassword.windowToken, 0)
    }

    override fun cancel() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    internal class SectionsPagerAdapter(hostFragment: Fragment) : FragmentStateAdapter(hostFragment) {
        private val mFragmentList = ArrayList<Fragment>(2)
        private val mFragmentTitleList = ArrayList<String>(2)

        fun getTabTitle(position: Int): String {
            return mFragmentTitleList[position]
        }

        fun addFragment(fragment: Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }

        override fun getItemCount(): Int {
            return mFragmentList.size
        }

        override fun createFragment(position: Int): Fragment {
            return mFragmentList[position]
        }

    }
    companion object {
        val TAG = JamiLinkAccountFragment::class.simpleName!!
    }
}