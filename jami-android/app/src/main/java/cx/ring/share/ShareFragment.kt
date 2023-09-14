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
package cx.ring.share

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cx.ring.R
import cx.ring.databinding.FragShareBinding
import cx.ring.mvp.BaseSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.mvp.GenericView
import net.jami.share.SharePresenter
import net.jami.share.ShareViewModel

@AndroidEntryPoint
class ShareFragment : BaseSupportFragment<SharePresenter, GenericView<ShareViewModel>>(), GenericView<ShareViewModel> {
    private var mUriToShow: String? = null
    private var isShareLocked = false
    private var binding: FragShareBinding? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragShareBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        binding!!.shareButton.setOnClickListener { if (!isShareLocked) shareAccount() }
    }

    private fun shareAccount() {
        if (!TextUtils.isEmpty(mUriToShow)) {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.account_contact_me))
            sharingIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.account_share_body, mUriToShow, getText(R.string.app_website)))
            startActivity(Intent.createChooser(sharingIntent, getText(R.string.share_via)))
        }
    }

    override fun showViewModel(viewModel: ShareViewModel) {
        if (binding == null) return
        val qrCodeData = viewModel.getAccountQRCodeData(
            resources.getColor(R.color.color_primary_dark, null),
            resources.getColor(R.color.transparent, null)
        )
        if (qrCodeData == null) {
            binding!!.qrImage.visibility = View.INVISIBLE
            binding!!.shareInstruction.setText(R.string.share_message_no_account)
        } else {
            val bitmap = Bitmap.createBitmap(qrCodeData.width, qrCodeData.height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(qrCodeData.data, 0, qrCodeData.width, 0, 0, qrCodeData.width, qrCodeData.height)
            binding!!.qrImage.setImageBitmap(bitmap)
            binding!!.shareInstruction.setText(R.string.share_message)
            binding!!.qrImage.visibility = View.VISIBLE
        }
        mUriToShow = viewModel.accountDisplayUri
        isShareLocked = TextUtils.isEmpty(mUriToShow)
    }
}