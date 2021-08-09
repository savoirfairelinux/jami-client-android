/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Loïc Siret <loic.siret@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.tv.account

import android.app.AlertDialog
import android.app.DownloadManager
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import cx.ring.R
import cx.ring.utils.AndroidFileUtils.getMimeType
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.JamiAccountSummaryPresenter
import net.jami.account.JamiAccountSummaryView
import net.jami.model.Account
import java.io.File

@AndroidEntryPoint
class TVAccountExport : JamiGuidedStepFragment<JamiAccountSummaryPresenter>(), JamiAccountSummaryView {
    private var mWaitDialog: ProgressDialog? = null
    private lateinit var mIdAccount: String
    private var mHasPassword = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.setAccountId(mIdAccount)
    }

    override fun onCreateGuidance(savedInstanceState: Bundle): Guidance {
        val title = getString(R.string.account_export_title)
        val breadcrumb = ""
        val description = getString(R.string.account_link_export_info_light)
        val icon = requireContext().getDrawable(R.drawable.baseline_devices_24)
        return Guidance(title, description, breadcrumb, icon)
    }

    override fun onCreateActions(actions: List<GuidedAction>, savedInstanceState: Bundle) {
        if (mHasPassword) {
            addPasswordAction(activity, actions, PASSWORD, getString(R.string.account_enter_password), "", "")
        } else {
            addAction(context, actions, ACTION, R.string.account_start_export_button)
        }
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        presenter.startAccountExport("")
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        presenter.startAccountExport(action.description.toString())
        return GuidedAction.ACTION_ID_NEXT
    }

    override fun onProvideTheme(): Int {
        return R.style.Theme_Ring_Leanback_GuidedStep_First
    }

    override fun showExportingProgressDialog() {
        mWaitDialog = ProgressDialog.show(activity,
            getString(R.string.export_account_wait_title),
            getString(R.string.export_account_wait_message)
        )
    }

    override fun showPasswordProgressDialog() {}
    override fun accountChanged(account: Account) {}
    override fun showNetworkError() {
        mWaitDialog!!.dismiss()
        AlertDialog.Builder(activity)
            .setTitle(R.string.account_export_end_network_title)
            .setMessage(R.string.account_export_end_network_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun showPasswordError() {
        mWaitDialog!!.dismiss()
        AlertDialog.Builder(activity)
            .setTitle(R.string.account_export_end_error_title)
            .setMessage(R.string.account_export_end_decryption_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun showGenericError() {
        mWaitDialog!!.dismiss()
        AlertDialog.Builder(activity)
            .setTitle(R.string.account_export_end_error_title)
            .setMessage(R.string.account_export_end_error_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun showPIN(pin: String) {
        mWaitDialog!!.dismiss()
        val pined = getString(R.string.account_end_export_infos).replace("%%", pin)
        val styledResultText = SpannableString(pined)
        val pos = pined.lastIndexOf(pin)
        styledResultText.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            pos,
            pos + pin.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        styledResultText.setSpan(
            StyleSpan(Typeface.BOLD),
            pos,
            pos + pin.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        styledResultText.setSpan(
            RelativeSizeSpan(2.8f),
            pos,
            pos + pin.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        AlertDialog.Builder(activity)
            .setMessage(styledResultText)
            .setPositiveButton(android.R.string.ok) { _, _ -> parentFragmentManager.popBackStack() }
            .show()
    }

    override fun passwordChangeEnded(ok: Boolean) {}
    override fun displayCompleteArchive(dest: File) {
        val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.addCompletedDownload(
            dest.name,
            dest.name,
            true,
            getMimeType(dest.absolutePath),
            dest.absolutePath,
            dest.length(),
            true
        )
    }

    override fun gotToImageCapture() {}
    override fun askCameraPermission() {}
    override fun goToGallery() {}
    override fun askGalleryPermission() {}
    override fun updateUserView(account: Account) {}
    override fun goToMedia(accountId: String) {}
    override fun goToSystem(accountId: String) {}
    override fun goToAdvanced(accountId: String) {}
    override fun goToAccount(accountId: String) {}
    override fun setSwitchStatus(account: Account) {}
    override fun showRevokingProgressDialog() {}
    override fun deviceRevocationEnded(device: String, status: Int) {}
    override fun updateDeviceList(devices: Map<String, String>, currentDeviceId: String) {}

    companion object {
        private const val PASSWORD = 1L
        private const val ACTION = 2L
        fun createInstance(idAccount: String, hasPassword: Boolean): TVAccountExport {
            val fragment = TVAccountExport()
            fragment.mIdAccount = idAccount
            fragment.mHasPassword = hasPassword
            return fragment
        }
    }
}