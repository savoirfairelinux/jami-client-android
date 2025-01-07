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
package cx.ring.settings.extensionssettings

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.account.AccountEditionFragment
import cx.ring.databinding.FragExtensionsListSettingsBinding
import cx.ring.databinding.ItemProgressDialogBinding
import cx.ring.interfaces.AppBarStateListener
import cx.ring.extensions.ExtensionUtils.getInstalledExtensions
import cx.ring.extensions.ExtensionUtils.loadExtension
import cx.ring.extensions.ExtensionUtils.unloadExtension
import cx.ring.settings.SettingsFragment
import cx.ring.settings.extensionssettings.ExtensionsListAdapter.ExtensionListItemListener
import cx.ring.utils.AndroidFileUtils.getCacheFile
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.daemon.JamiService
import java.io.File
import java.io.IOException

class ExtensionsListSettingsFragment : Fragment(), ExtensionListItemListener {
    private var binding: FragExtensionsListSettingsBinding? = null
    private var mAdapter: ExtensionsListAdapter? = null
    private val mCompositeDisposable = CompositeDisposable()
    private var mProgress: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragExtensionsListSettingsBinding.inflate(inflater, container, false)
        val accountId = requireArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY)!!

        val appBarStateListener = parentFragment as? AppBarStateListener
        appBarStateListener?.onToolbarTitleChanged(getString(R.string.menu_item_extension_list))
        appBarStateListener?.onAppBarScrollTargetViewChanged(binding!!.extensionsList)

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding!!.extensionsList.setHasFixedSize(true)

        mAdapter = ExtensionsListAdapter(getInstalledExtensions(binding!!.extensionsList.context), this, accountId)
        binding!!.extensionsList.adapter = mAdapter

        //Fab
        if (accountId.isEmpty()) {
            binding!!.extensionsListSettingsFab.visibility = View.VISIBLE
            binding!!.extensionsListSettingsFab.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"
                startActivityForResult(intent, ARCHIVE_REQUEST_CODE)
            }
        }
        return binding!!.root
    }

    /**
     * Implements ExtensionListItemListener.onExtensionItemClicked which is called when an extension
     * list item is clicked
     * @param extensionDetails instance of an extension details that is sent to ExtensionSettingsFragment
     */
    override fun onExtensionItemClicked(extensionDetails: ExtensionDetails) {
        (parentFragment as SettingsFragment).goToExtensionSettings(extensionDetails)
    }

    /**
     * Implements ExtensionListItemListener.onExtensionEnabled which is called when the checkbox
     * associated with the extension list item is called
     * @param extensionDetails instance of an extension details that is sent to ExtensionSettingsFragment
     */
    override fun onExtensionEnabled(extensionDetails: ExtensionDetails) {
        var status: String?

        if (extensionDetails.isEnabled) {
            extensionDetails.isEnabled = loadExtension(extensionDetails.rootPath)
            status =
                if (extensionDetails.isEnabled) getString(R.string.load_success, extensionDetails.name)
                else {
                    mAdapter?.notifyItemChanged(extensionDetails)
                    getString(R.string.unable_to_load, extensionDetails.name)
                }
        } else {
            unloadExtension(extensionDetails.rootPath)
            status = getString(R.string.unload_success, extensionDetails.name)
        }
        Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ARCHIVE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val uri = data.data
                if (uri != null) {
                    installExtensionFromUri(uri, false)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun installExtensionFile(extensionFile: File, force: Boolean): String {
        val i = JamiService.installPlugin(extensionFile.absolutePath, force)
        if (!extensionFile.delete()) {
            Log.e(TAG, "Extension Jpl file in the cache not freed.")
        }
        return when (i) {
            0 -> extensionFile.name
            100 -> throw IOException(getString(R.string.extension_same_version_exception, extensionFile.name))
            200 -> throw IOException(getString(R.string.extension_recent_version_exception, extensionFile.name))
            300 -> throw IOException(getString(R.string.extension_invalid_signature, extensionFile.name))
            400 -> throw IOException(getString(R.string.extension_invalid_authority, extensionFile.name))
            500 -> throw IOException(getString(R.string.extension_invalid_format, extensionFile.name))
            else -> throw IOException(getString(R.string.extension_install_failure, extensionFile.name))
        }
    }

    private fun showLoading(show: Boolean) {
        val progress = mProgress
        if(show && (progress == null || !progress.isShowing))
            mProgress = MaterialAlertDialogBuilder(requireContext())
                    .setView(ItemProgressDialogBinding.inflate(layoutInflater).root)
                    .setBackground(ColorDrawable(Color.TRANSPARENT))
                    .setCancelable(false)
                    .show()
        else if(!show && progress != null && progress.isShowing)
            progress.dismiss()
    }

    private fun installExtensionFromUri(uri: Uri, force: Boolean) {
        showLoading(true)
        mCompositeDisposable.add(
            getCacheFile(requireContext(), uri)
                .observeOn(AndroidSchedulers.mainThread())
                .map { file: File -> installExtensionFile(file, force) }
                .subscribe({ filename: String ->
                    val extension = filename.split(".jpl".toRegex()).toTypedArray()
                    val availableExtensions = getInstalledExtensions(requireContext())
                    for (availableExtension in availableExtensions) {
                        if (availableExtension.name == extension[0]) {
                            availableExtension.isEnabled = true
                            onExtensionEnabled(availableExtension)
                        }
                    }
                    mAdapter!!.updateExtensionsList(getInstalledExtensions(requireContext()))
                    showLoading(false)
                    Toast.makeText(requireContext(), getString(R.string.install_success, filename), Toast.LENGTH_LONG)
                        .show()
                }) { e: Throwable ->
                    if (binding != null) {
                        Log.e(TAG, "An error occurred while importing the extension.", e)
                        val sb = Snackbar.make(binding!!.listLayout, getString(R.string.install_error), Snackbar.LENGTH_LONG)
                        sb.setAction(R.string.extension_force_install) { v: View? -> installExtensionFromUri(uri, true) }
                        sb.show()
                    }
                    showLoading(false)
                })
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCompositeDisposable.dispose()
    }

    companion object {
        val TAG = ExtensionsListSettingsFragment::class.java.simpleName
        private const val ARCHIVE_REQUEST_CODE = 42

        fun newInstance(accountId: String?): ExtensionsListSettingsFragment {
            val fragment = ExtensionsListSettingsFragment()
            fragment.arguments = Bundle().apply { putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId) }
            return fragment
        }
    }
}