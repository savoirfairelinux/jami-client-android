/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
package cx.ring.tv.account;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;

import java.io.File;
import java.util.List;
import java.util.Map;

import cx.ring.R;
import cx.ring.account.RingAccountSummaryPresenter;
import cx.ring.account.RingAccountSummaryView;
import cx.ring.application.JamiApplication;
import cx.ring.model.Account;
import cx.ring.utils.AndroidFileUtils;

public class TVAccountExport
        extends RingGuidedStepFragment<RingAccountSummaryPresenter>
        implements RingAccountSummaryView {

    private static final long PASSWORD = 1L;
    private ProgressDialog mWaitDialog;
    private String mIdAccount;

    public static TVAccountExport createInstance(String idAccount) {
        TVAccountExport fragment = new TVAccountExport();
        fragment.mIdAccount = idAccount;
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((JamiApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        super.onViewCreated(view, savedInstanceState);
        presenter.setAccountId(mIdAccount);
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.account_export_title);
        String breadcrumb = "";
        String description = getString(R.string.account_link_export_info_light);
        Drawable icon = getActivity().getResources().getDrawable(R.drawable.baseline_devices_24);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        addPasswordAction(getActivity(), actions, PASSWORD, getString(R.string.account_enter_password), "", "");
    }

    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        presenter.startAccountExport(action.getDescription().toString());
        return GuidedAction.ACTION_ID_NEXT;
    }

    @Override
    public int onProvideTheme() {
        return R.style.Theme_Ring_Leanback_GuidedStep_First;
    }

    @Override
    public void showExportingProgressDialog() {
        mWaitDialog = ProgressDialog.show(getActivity(),
                getString(R.string.export_account_wait_title),
                getString(R.string.export_account_wait_message));
    }

    @Override
    public void showRevokingProgressDialog() {

    }

    @Override
    public void showPasswordProgressDialog() {

    }

    @Override
    public void accountChanged(Account account) {

    }

    @Override
    public void showNetworkError() {
        mWaitDialog.dismiss();
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.account_export_end_network_title)
                .setMessage(R.string.account_export_end_network_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void showPasswordError() {
        mWaitDialog.dismiss();
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.account_export_end_error_title)
                .setMessage(R.string.account_export_end_decryption_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void showGenericError() {
        mWaitDialog.dismiss();
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.account_export_end_error_title)
                .setMessage(R.string.account_export_end_error_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void showPIN(final String pin) {
        mWaitDialog.dismiss();
        String pined = getString(R.string.account_end_export_infos).replace("%%", pin);
        final SpannableString styledResultText = new SpannableString(pined);
        int pos = pined.lastIndexOf(pin);
        styledResultText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), pos, (pos + pin.length()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        styledResultText.setSpan(new StyleSpan(Typeface.BOLD), pos, (pos + pin.length()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        styledResultText.setSpan(new RelativeSizeSpan(2.8f), pos, (pos + pin.length()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        new AlertDialog.Builder(getActivity())
                .setMessage(styledResultText)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> getFragmentManager().popBackStack())
                .show();
    }

    @Override
    public void updateDeviceList(Map<String, String> devices, String currentDeviceId) {

    }

    @Override
    public void deviceRevocationEnded(String device, int status) {

    }

    @Override
    public void passwordChangeEnded(boolean ok) {

    }

    public void displayCompleteArchive(File dest)  {
        DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadManager.addCompletedDownload(dest.getName(),
                    dest.getName(),
                    true,
                    AndroidFileUtils.getMimeType(dest.getAbsolutePath()),
                    dest.getAbsolutePath(),
                    dest.length(),
                    true);
        }
    }
}
