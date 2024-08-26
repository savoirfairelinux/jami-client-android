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
package cx.ring.settings.extensionssettings

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import cx.ring.databinding.FragExtensionsPathPreferenceBinding
import cx.ring.interfaces.AppBarStateListener
import cx.ring.settings.extensionssettings.PathListAdapter.PathListItemListener
import cx.ring.utils.AndroidFileUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import java.io.File

class ExtensionPathPreferenceFragment : Fragment(), PathListItemListener {
    private val pathList: MutableList<String> = ArrayList()
    private lateinit var mExtensionDetails: ExtensionDetails
    private lateinit var mCurrentKey: String
    private var mCurrentValue: String? = null
    private var subtitle: String = ""
    private var supportedMimeTypes = arrayOf("*/*")
    private var binding: FragExtensionsPathPreferenceBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = requireArguments()
        val details = ExtensionDetails(arguments.getString("name")!!, arguments.getString("rootPath")!!, arguments.getBoolean("enabled"))
        mExtensionDetails = details
        val key = arguments.getString("preferenceKey")!!
        mCurrentKey = key
        val mPreferencesAttributes = details.extensionPreferences
        if (mPreferencesAttributes.isNotEmpty()) {
            mCurrentValue = details.extensionPreferencesValues[key]
            setHasOptionsMenu(true)
            for (preferenceAttributes in mPreferencesAttributes) {
                if (preferenceAttributes["key"] == key) {
                    val mimeType = preferenceAttributes["mimeType"]
                    if (!mimeType.isNullOrEmpty())
                        supportedMimeTypes = mimeType.split(',').toTypedArray()
                    subtitle = details.name + " - " + preferenceAttributes["title"]
                    var defaultPath = preferenceAttributes["defaultValue"]
                    if (!defaultPath.isNullOrEmpty()) {
                        defaultPath = defaultPath.substring(0, defaultPath.lastIndexOf("/"))
                        for (file in File(defaultPath).listFiles()!!) {
                            if (supportedMimeTypes.equals("*/*")) {
                                pathList.add(file.toString())
                            } else {
                                for (mime in supportedMimeTypes) {
                                    if (file.toString().endsWith(mime.replace("*/", ".")))
                                        pathList.add(file.toString())
                                }
                            }
                        }
                        break
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragExtensionsPathPreferenceBinding.inflate(inflater, container, false).apply {
            if (pathList.isNotEmpty())
                pathPreferences.adapter = PathListAdapter(pathList, this@ExtensionPathPreferenceFragment)
            binding = this
            extensionSettingSubtitle.text = subtitle
            extensionsPathPreferenceFab.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = supportedMimeTypes[0]
                    putExtra(Intent.EXTRA_MIME_TYPES, supportedMimeTypes)
                }
                startActivityForResult(intent, PATH_REQUEST_CODE)
            }
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val currentValue = mCurrentValue
        val binding = binding ?: return
        if (!currentValue.isNullOrEmpty()) {
            binding.currentPathItemIcon.visibility = View.VISIBLE
            val file = File(currentValue)
            if (file.exists()) {
                if (AndroidFileUtils.isImage(currentValue)) {
                    binding.currentPathItemName.visibility = View.INVISIBLE
                    val icon = Drawable.createFromPath(currentValue)
                    if (icon != null) {
                        binding.currentPathItemIcon.setImageDrawable(icon)
                    }
                } else {
                    binding.currentPathItemName.visibility = View.VISIBLE
                    binding.currentPathItemName.text = file.name
                }
            }
        } else {
            binding.currentPathItemIcon.visibility = View.INVISIBLE
            binding.currentPathItemName.visibility = View.INVISIBLE
            binding.extensionsPathPreferenceFab.performClick()
        }

        (parentFragment as? AppBarStateListener)?.onAppBarScrollTargetViewChanged(binding.pathPreferences)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PATH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                AndroidFileUtils.getCacheFile(requireContext(), uri)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ file: File -> setPreferencePath(file.absolutePath) })
                    { e: Throwable -> context?.let { c -> Toast.makeText(c, e.message, Toast.LENGTH_LONG).show() }}
            }
        }
    }

    override fun onResume() {
//        (requireActivity() as HomeActivity).setToolbarTitle(R.string.menu_item_extension_list)
        super.onResume()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun setPreferencePath(path: String) {
        if (mExtensionDetails.setExtensionPreference(mCurrentKey, path)) {
            mCurrentValue = path
            val binding = binding ?: return
            if (path.isNotEmpty()) {
                binding.currentPathItemIcon.visibility = View.VISIBLE
                if (AndroidFileUtils.isImage(path)) {
                    val icon = Drawable.createFromPath(path)
                    binding.currentPathItemIcon.setImageDrawable(icon)
                    binding.currentPathItemName.visibility = View.INVISIBLE
                } else {
                    binding.currentPathItemName.text = File(path).name
                    binding.currentPathItemName.visibility = View.VISIBLE
                }
            } else {
                binding.currentPathItemIcon.visibility = View.INVISIBLE
                binding.currentPathItemName.visibility = View.INVISIBLE
            }
        }
    }

    override fun onPathItemClicked(path: String) {
        setPreferencePath(path)
    }

    companion object {
        val TAG = ExtensionPathPreferenceFragment::class.simpleName!!
        private const val PATH_REQUEST_CODE = 1
        fun newInstance(extensionDetails: ExtensionDetails, preferenceKey: String?): ExtensionPathPreferenceFragment {
            val ppf = ExtensionPathPreferenceFragment()
            ppf.arguments = Bundle().apply {
                putString("name", extensionDetails.name)
                putString("rootPath", extensionDetails.rootPath)
                putBoolean("enabled", extensionDetails.isEnabled)
                putString("preferenceKey", preferenceKey)
            }
            return ppf
        }
    }
}