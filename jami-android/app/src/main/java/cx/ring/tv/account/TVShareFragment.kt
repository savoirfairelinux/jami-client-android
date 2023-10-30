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
package cx.ring.tv.account

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cx.ring.R
import cx.ring.databinding.TvFragShareBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.services.VCardServiceImpl
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Account
import net.jami.mvp.GenericView
import net.jami.share.SharePresenter
import net.jami.share.ShareViewModel

@AndroidEntryPoint
class TVShareFragment : BaseSupportFragment<SharePresenter, GenericView<ShareViewModel>>(), GenericView<ShareViewModel> {
    private var binding: TvFragShareBinding? = null
    private val disposable = CompositeDisposable()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = TvFragShareBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        disposable.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    override fun showViewModel(viewModel: ShareViewModel) {
        binding?.let { binding ->
            val qrCodeData = viewModel.getAccountQRCodeData(0x00000000, -0x1)
            getUserAvatar(viewModel.account)
            if (qrCodeData == null) {
                binding.qrImage.visibility = View.INVISIBLE
            } else {
                val pad = 56
                val bitmap = Bitmap.createBitmap(qrCodeData.width + 2 * pad, qrCodeData.height + 2 * pad, Bitmap.Config.ARGB_8888)
                bitmap.setPixels(qrCodeData.data, 0, qrCodeData.width, pad, pad, qrCodeData.width, qrCodeData.height)
                binding.qrImage.setImageBitmap(bitmap)
                binding.shareQrInstruction.setText(R.string.share_message)
                binding.qrImage.visibility = View.VISIBLE
            }
        }
    }

    private fun getUserAvatar(account: Account) {
        disposable.add(VCardServiceImpl.loadProfile(requireContext(), account)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                binding?.apply {
                    shareUri?.visibility = View.VISIBLE
                    shareUri?.text = account.displayUsername
                }
            }
            .map { p -> AvatarDrawable.build(requireContext(), account, p, true) }
            .subscribe({ a: AvatarDrawable ->
                binding?.apply {
                    qrUserPhoto?.visibility = View.VISIBLE
                    qrUserPhoto?.setImageDrawable(a)
                }
            }) { e -> Log.e(TVShareFragment::class.simpleName!!, e.message!!) })
    }
}
