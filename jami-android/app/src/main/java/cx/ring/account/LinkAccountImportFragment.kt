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
package cx.ring.account

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import cx.ring.R
import cx.ring.account.pinInput.EditTextPinInputViewModel
import cx.ring.account.pinInput.QrCodePinInputViewModel
import cx.ring.databinding.FragAccJamiLinkBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.account.LinkAccountImportPresenter
import net.jami.account.LinkAccountImportView
import net.jami.services.AccountService
import net.jami.utils.Log
import net.jami.utils.QRCodeUtils

@AndroidEntryPoint
class LinkAccountImportFragment :
    BaseSupportFragment<LinkAccountImportPresenter, LinkAccountImportView>(),
    LinkAccountImportView {
    private val model: AccountCreationViewModel by activityViewModels()
    private var binding: FragAccJamiLinkBinding? = null
    val compositeDisposable = CompositeDisposable()

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

//        val adapter = SectionsPagerAdapter(this@JamiLinkAccountFragment)
//        adapter.addFragment(QrCodePinInputFragment(), getString(R.string.connect_device_scanqr))
//        adapter.addFragment(EditTextPinInputFragment(), getString(R.string.connect_device_enterPIN))
//        pager.adapter = adapter
//        pager.currentItem = 0
//        TabLayoutMediator(tabLayout, pager) { tab, position ->
//            tab.text = adapter.getTabTitle(position)
//        }.attach()

//        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab?) {
//                presenter.resetPin()
//                // emit the pin again when switching tabs
//                if (tab?.position == 0) {
//                    qrCodePinInputViewModel.emitPinAgain()
//                } else {
//                    editTextPinInputViewModel.emitPinAgain()
//                }
//            }
//
//            override fun onTabUnselected(tab: TabLayout.Tab?) {}
//
//            override fun onTabReselected(tab: TabLayout.Tab?) {}
//        })

        linkButton.setOnClickListener { presenter.linkClicked() }

//        existingPassword.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable) {
//                presenter.passwordChanged(s.toString())
//            }
//        })

        binding = this
    }.root

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = setLayout(inflater, container)

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        qrCodePinInputViewModel.init({ presenter.pinChanged(it) }, { presenter.resetPin() })
//        editTextPinInputViewModel.init({ presenter.pinChanged(it) }, { presenter.resetPin() })
//    }

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

    override fun initPresenter(presenter: LinkAccountImportPresenter) {
        presenter.init(model.model)
    }

    override fun enableLinkButton(enable: Boolean) {
//        binding!!.linkButton.isEnabled = enable
    }

//    override fun showPin(show: Boolean) {
//        val binding = binding ?: return
//        binding.pager.visibility = if (show) View.VISIBLE else View.GONE
//        binding.tabLayout.visibility = if (show) View.VISIBLE else View.GONE
//        binding.linkButton.setText(if (show) R.string.account_link_device else R.string.account_link_archive_button)
//    }

    override fun createAccount() {
        (activity as AccountWizardActivity?)?.createAccount()
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
//        imm?.hideSoftInputFromWindow(binding!!.existingPassword.windowToken, 0)
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


    override fun showLoadingToken() {
        Log.w("devdebug", "JamiLinkAccountFragment showLoadingToken")
        binding?.loadingIndicator?.isVisible = true


    }

    override fun showTokenAvailable(token: String) {
        Log.w("devdebug", "JamiLinkAccountFragment showTokenAvailable")
        binding?.loadingIndicator?.isVisible = false
        binding?.qrText?.text = token

        compositeDisposable.add(
            Maybe.fromCallable {
                QRCodeUtils.encodeStringAsQRCodeData(
                    token,
                    requireContext().getColor(R.color.color_primary_dark),
                    requireContext().getColor(R.color.transparent)
                )
            }.observeOn(DeviceUtils.uiScheduler)
                .subscribe { qrCodeData ->
                    Log.w("devdebug", "JamiLinkAccountFragment showTokenAvailable qrCodeData=$qrCodeData width=${qrCodeData.width} height=${qrCodeData.height} data=${qrCodeData.data.size} token=$token")
            binding!!.qrImage!!.setImageBitmap(
                Bitmap.createBitmap(
                    qrCodeData.width, qrCodeData.height, Bitmap.Config.ARGB_8888
                ).apply {
                    setPixels(
                        qrCodeData.data, 0, qrCodeData.width,
                        0, 0, qrCodeData.width, qrCodeData.height
                    )
                }
            )
        })
    }

    override fun showConnecting() {
//        TODO("Not yet implemented")
    }

    override fun showAuthenticating() {
//        TODO("Not yet implemented")
    }

    override fun showDone() {
//        TODO("Not yet implemented")
    }

    override fun showError(linkDeviceError: AccountService.DeviceAuthStateError) {
//        TODO("Not yet implemented")
    }

    companion object {
        val TAG = LinkAccountImportFragment::class.simpleName!!
    }
}