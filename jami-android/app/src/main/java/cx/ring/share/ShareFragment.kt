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
package cx.ring.share

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cx.ring.R
import cx.ring.databinding.FragShareBinding
import cx.ring.mvp.BaseSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.model.Contact
import net.jami.model.Uri
import net.jami.share.SharePresenter
import net.jami.share.ShareView

@AndroidEntryPoint
class ShareFragment : BaseSupportFragment<SharePresenter, ShareView>() {

    private var mBinding: FragShareBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val contactUri = Uri.fromString(arguments?.getString(ARG_CONTACT_URI) ?: "")

        return FragShareBinding.inflate(inflater, container, false).apply {
            presenter.loadQRCodeData(
                contactUri,
                requireContext().getColor(R.color.color_primary_dark),
                requireContext().getColor(R.color.transparent)
            ) { qrCodeData ->
                qrImage.setImageBitmap(
                    Bitmap.createBitmap(
                        qrCodeData.width, qrCodeData.height, Bitmap.Config.ARGB_8888
                    ).apply {
                        setPixels(
                            qrCodeData.data, 0, qrCodeData.width,
                            0, 0, qrCodeData.width, qrCodeData.height
                        )
                    }
                )
            }

            shareButton.isEnabled = false
            presenter.loadContact(contactUri) { contact ->
                if (!contact.isUser)
                    shareButton.text = getText(R.string.share_contact_information)
                shareButton.setOnClickListener { shareContact(contact) }
                shareButton.isEnabled = true
            }

            mBinding = this
        }.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    private fun shareContact(contact: Contact) {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(
            Intent.EXTRA_SUBJECT,
            getText(
                if (contact.isUser) R.string.account_contact_me
                else R.string.share_contact_intent_title
            )
        )
        val displayName = contact.username?.blockingGet()
            .let { if (it.isNullOrEmpty()) contact.uri.uri else it }
        sharingIntent.putExtra(
            Intent.EXTRA_TEXT,
            getString(
                if (contact.isUser) R.string.account_share_body
                else R.string.share_contact_intent_body,
                displayName, getText(R.string.app_website)
            )
        )

        startActivity(Intent.createChooser(sharingIntent, getText(R.string.share_via)))
    }

    companion object {
        private const val ARG_CONTACT_URI = "contact_uri"

        fun newInstance(contactUri: Uri): ShareFragment {
            val fragment = ShareFragment()
            val args = Bundle()
            args.putString(ARG_CONTACT_URI, contactUri.uri)
            fragment.arguments = args
            return fragment
        }
    }
}