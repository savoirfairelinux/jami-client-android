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
package cx.ring.settings.pluginssettings

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
import cx.ring.databinding.FragPluginsListSettingsBinding
import cx.ring.databinding.ItemProgressDialogBinding
import cx.ring.interfaces.AppBarStateListener
import cx.ring.plugins.PluginUtils.getInstalledPlugins
import cx.ring.plugins.PluginUtils.loadPlugin
import cx.ring.plugins.PluginUtils.unloadPlugin
import cx.ring.settings.SettingsFragment
import cx.ring.settings.pluginssettings.PluginsListAdapter.PluginListItemListener
import cx.ring.utils.AndroidFileUtils.getCacheFile
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.daemon.JamiService
import java.io.File
import java.io.IOException

class PluginsListSettingsFragment : Fragment(), PluginListItemListener {
    private var binding: FragPluginsListSettingsBinding? = null
    private var mAdapter: PluginsListAdapter? = null
    private val mCompositeDisposable = CompositeDisposable()
    private var mProgress: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragPluginsListSettingsBinding.inflate(inflater, container, false)
        val accountId = requireArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY)!!

        val appBarStateListener = parentFragment as? AppBarStateListener
        appBarStateListener?.onToolbarTitleChanged(getString(R.string.menu_item_plugin_list))
        appBarStateListener?.onAppBarScrollTargetViewChanged(binding!!.pluginsList)

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding!!.pluginsList.setHasFixedSize(true)

        mAdapter = PluginsListAdapter(getInstalledPlugins(binding!!.pluginsList.context), this, accountId)
        binding!!.pluginsList.adapter = mAdapter

        //Fab
        if (accountId.isEmpty()) {
            binding!!.pluginsListSettingsFab.visibility = View.VISIBLE
            binding!!.pluginsListSettingsFab.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"
                startActivityForResult(intent, ARCHIVE_REQUEST_CODE)
            }
        }
        return binding!!.root
    }

    /**
     * Implements PluginListItemListener.onPluginItemClicked which is called when we click on
     * a plugin list item
     * @param pluginDetails instance of a plugin details that is sent to PluginSettingsFragment
     */
    override fun onPluginItemClicked(pluginDetails: PluginDetails) {
        (parentFragment as SettingsFragment).goToPluginSettings(pluginDetails)
    }

    /**
     * Implements PluginListItemListener.onPluginEnabled which is called when the checkbox
     * associated with the plugin list item is called
     * @param pluginDetails instance of a plugin details that is sent to PluginSettingsFragment
     */
    override fun onPluginEnabled(pluginDetails: PluginDetails) {
        var status: String?

        if (pluginDetails.isEnabled) {
            pluginDetails.isEnabled = loadPlugin(pluginDetails.rootPath)
            status =
                if (pluginDetails.isEnabled) getString(R.string.load_sucess, pluginDetails.name)
                else {
                    mAdapter?.notifyItemChanged(pluginDetails)
                    getString(R.string.unable_to_load, pluginDetails.name)
                }
        } else {
            unloadPlugin(pluginDetails.rootPath)
            status = getString(R.string.unload_sucess, pluginDetails.name)
        }
        Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ARCHIVE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val uri = data.data
                if (uri != null) {
                    installPluginFromUri(uri, false)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun installPluginFile(pluginFile: File, force: Boolean): String {
        val i = JamiService.installPlugin(pluginFile.absolutePath, force)
        if (!pluginFile.delete()) {
            Log.e(TAG, "Plugin Jpl file in the cache not freed")
        }
        return when (i) {
            0 -> pluginFile.name
            100 -> throw IOException(getString(R.string.plugin_same_version_exception, pluginFile.name))
            200 -> throw IOException(getString(R.string.plugin_recent_version_exception, pluginFile.name))
            300 -> throw IOException(getString(R.string.plugin_invalid_signature, pluginFile.name))
            400 -> throw IOException(getString(R.string.plugin_invalid_authority, pluginFile.name))
            500 -> throw IOException(getString(R.string.plugin_invalid_format, pluginFile.name))
            else -> throw IOException(getString(R.string.plugin_install_failure, pluginFile.name))
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

    private fun installPluginFromUri(uri: Uri, force: Boolean) {
        showLoading(true)
        mCompositeDisposable.add(
            getCacheFile(requireContext(), uri)
                .observeOn(AndroidSchedulers.mainThread())
                .map { file: File -> installPluginFile(file, force) }
                .subscribe({ filename: String ->
                    val plugin = filename.split(".jpl".toRegex()).toTypedArray()
                    val availablePlugins = getInstalledPlugins(requireContext())
                    for (availablePlugin in availablePlugins) {
                        if (availablePlugin.name == plugin[0]) {
                            availablePlugin.isEnabled = true
                            onPluginEnabled(availablePlugin)
                        }
                    }
                    mAdapter!!.updatePluginsList(getInstalledPlugins(requireContext()))
                    showLoading(false)
                    Toast.makeText(requireContext(), getString(R.string.install_sucess, filename), Toast.LENGTH_LONG)
                        .show()
                }) { e: Throwable ->
                    if (binding != null) {
                        Log.e(TAG, "Error importing plugin", e)
                        val sb = Snackbar.make(binding!!.listLayout, getString(R.string.install_error), Snackbar.LENGTH_LONG)
                        sb.setAction(R.string.plugin_force_install) { v: View? -> installPluginFromUri(uri, true) }
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
        val TAG = PluginsListSettingsFragment::class.java.simpleName
        private const val ARCHIVE_REQUEST_CODE = 42

        fun newInstance(accountId: String?): PluginsListSettingsFragment {
            val fragment = PluginsListSettingsFragment()
            fragment.arguments = Bundle().apply { putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId) }
            return fragment
        }
    }
}