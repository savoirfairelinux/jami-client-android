/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
package cx.ring.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cx.ring.R
import cx.ring.databinding.DialogAddEditExposedServiceBinding
import net.jami.services.ExposedServiceInfo
import net.jami.services.ExposedServiceType

class AddEditExposedServiceDialog(
    private val editingService: ExposedServiceInfo?,
    private val onSave: ((ExposedServiceInfo) -> Unit)?
) : BottomSheetDialogFragment() {
    private var selectedDirectoryUri: String = ""
    private var binding: DialogAddEditExposedServiceBinding? = null

    private val pickDirectory = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@registerForActivityResult
        Log.i(TAG, "pickDirectory: uri='$uri'")

        // Persist read permission across app restarts
        requireContext().contentResolver.takePersistableUriPermission(
            uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        selectedDirectoryUri = uri.toString()

        val displayPath = uri.path
            ?.substringAfterLast(':')   // e.g. "/tree/primary:Download/site" → "Download/site"
            ?.ifEmpty { uri.toString() }
            ?: uri.toString()

        Log.i(TAG, "pickDirectory: stored URI='$selectedDirectoryUri' display='$displayPath'")

        val b = binding ?: return@registerForActivityResult
        b.etDirectory.setText(displayPath)
        if (b.etWebsiteName.text.isNullOrBlank()) {
            b.etWebsiteName.setText(displayPath.substringAfterLast('/').ifEmpty { displayPath })
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = DialogAddEditExposedServiceBinding.inflate(inflater, container, false)
        .also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return

        setupPolicyDropdown(b)
        setupTypeToggle(b)
        populateForEditing(b)

        b.btnChooseDirectory.setOnClickListener { pickDirectory.launch(null) }
        b.btnCancel.setOnClickListener { dismiss() }
        b.btnSave.setOnClickListener { saveService() }

        b.etAllowedContacts.addTextChangedListener { /* live validation could go here */ }
    }

    private fun setupPolicyDropdown(b: DialogAddEditExposedServiceBinding) {
        val labels = listOf(
            getString(R.string.shared_services_policy_contacts),
            getString(R.string.shared_services_policy_public),
            getString(R.string.shared_services_policy_specific)
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, labels)
        b.dropdownPolicy.setAdapter(adapter)
        b.dropdownPolicy.setText(labels[0], false)

        b.dropdownPolicy.setOnItemClickListener { _, _, position, _ ->
            b.etAllowedContactsLayout.isVisible = POLICY_VALUES[position] == "specific"
        }
    }

    private fun setupTypeToggle(b: DialogAddEditExposedServiceBinding) {
        b.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val isCustom = checkedId == R.id.btn_type_custom
            b.sectionCustom.isVisible = isCustom
            b.sectionWebsite.isVisible = !isCustom
            b.dialogTitle.text = getString(
                if (editingService != null) R.string.shared_services_edit_title
                else if (isCustom) R.string.shared_services_add_custom_title
                else R.string.shared_services_add_website_title
            )
        }
        // Default selection
        val initialType = editingService?.type ?: ExposedServiceType.CUSTOM
        b.toggleType.check(if (initialType == ExposedServiceType.CUSTOM) R.id.btn_type_custom else R.id.btn_type_website)
    }

    private fun populateForEditing(b: DialogAddEditExposedServiceBinding) {
        val service = editingService ?: return
        if (service.type == ExposedServiceType.CUSTOM) {
            b.etName.setText(service.name)
            b.etDescription.setText(service.description)
            b.etHost.setText(service.localHost.ifEmpty { "localhost" })
            b.etPort.setText(if (service.localPort > 0) service.localPort.toString() else "")
            b.etScheme.setText(service.scheme)
        } else {
            selectedDirectoryUri = service.directory
            val displayPath = if (service.directory.startsWith("content://")) {
                android.net.Uri.parse(service.directory).path
                    ?.substringAfterLast(':')
                    ?.ifEmpty { service.directory }
                    ?: service.directory
            } else service.directory
            b.etDirectory.setText(displayPath)
            b.etWebsiteName.setText(service.name)
        }
        val policyIndex = POLICY_VALUES.indexOf(service.policy).coerceAtLeast(0)
        val labels = listOf(
            getString(R.string.shared_services_policy_contacts),
            getString(R.string.shared_services_policy_public),
            getString(R.string.shared_services_policy_specific)
        )
        b.dropdownPolicy.setText(labels[policyIndex], false)
        if (service.policy == "specific") {
            b.etAllowedContactsLayout.isVisible = true
            b.etAllowedContacts.setText(service.allowedContacts)
        }
    }

    private fun saveService() {
        val b = binding ?: return
        val isCustom = b.toggleType.checkedButtonId == R.id.btn_type_custom
        val policyIndex = run {
            val labels = listOf(
                getString(R.string.shared_services_policy_contacts),
                getString(R.string.shared_services_policy_public),
                getString(R.string.shared_services_policy_specific)
            )
            labels.indexOf(b.dropdownPolicy.text.toString()).coerceAtLeast(0)
        }
        val policy = POLICY_VALUES[policyIndex]
        val allowedContacts = if (policy == "specific") b.etAllowedContacts.text.toString().trim() else ""

        if (isCustom) {
            saveCustomService(b, policy, allowedContacts)
        } else {
            saveWebsiteService(b, policy, allowedContacts)
        }
    }

    private fun saveCustomService(b: DialogAddEditExposedServiceBinding, policy: String, allowedContacts: String) {
        val name = b.etName.text.toString().trim()
        val description = b.etDescription.text.toString().trim()
        val host = b.etHost.text.toString().trim().ifEmpty { "localhost" }
        val portText = b.etPort.text.toString().trim()
        val scheme = b.etScheme.text.toString().trim()

        var valid = true
        if (name.isEmpty()) {
            b.etNameLayout.error = getString(R.string.shared_services_name_required)
            valid = false
        } else {
            b.etNameLayout.error = null
        }
        val port = portText.toIntOrNull()
        if (portText.isEmpty() || port == null || port < 1 || port > 65535) {
            b.etPortLayout.error = if (portText.isEmpty())
                getString(R.string.shared_services_port_required)
            else
                getString(R.string.shared_services_port_invalid)
            valid = false
        } else {
            b.etPortLayout.error = null
        }
        if (!valid) return

        val service = (editingService ?: ExposedServiceInfo()).copy(
            type = ExposedServiceType.CUSTOM,
            name = name,
            description = description,
            localHost = host,
            localPort = port!!,
            scheme = scheme,
            directory = "",
            policy = policy,
            allowedContacts = allowedContacts
        )
        onSave?.invoke(service)
        dismiss()
    }

    private fun saveWebsiteService(b: DialogAddEditExposedServiceBinding, policy: String, allowedContacts: String) {
        val displayDirectory = b.etDirectory.text.toString().trim()
        val name = b.etWebsiteName.text.toString().trim()
        Log.i(TAG, "saveWebsiteService: name='$name' displayDir='$displayDirectory' uri='$selectedDirectoryUri' policy='$policy'")

        var valid = true
        if (selectedDirectoryUri.isEmpty() && displayDirectory.isEmpty()) {
            b.etDirectoryLayout.error = getString(R.string.shared_services_directory_required)
            valid = false
        } else {
            b.etDirectoryLayout.error = null
        }
        if (name.isEmpty()) {
            b.etWebsiteNameLayout.error = getString(R.string.shared_services_name_required)
            valid = false
        } else {
            b.etWebsiteNameLayout.error = null
        }
        if (!valid) return

        // Store the full content:// URI as directory so AndroidExposedServicesService
        // can use DocumentFile (SAF) to read files. Falls back to display path for
        // existing services migrated before this change.
        val directoryValue = selectedDirectoryUri.ifEmpty { displayDirectory }
        val service = (editingService ?: ExposedServiceInfo()).copy(
            type = ExposedServiceType.EMBEDDED,
            name = name,
            description = "",
            localHost = "localhost",
            localPort = 0,
            scheme = "http",
            directory = directoryValue,
            policy = policy,
            allowedContacts = allowedContacts
        )
        Log.i(TAG, "saveWebsiteService: invoking onSave with service=$service")
        onSave?.invoke(service)
        dismiss()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "AddEditExposedServiceDialog"
        private val POLICY_VALUES = listOf("contacts", "public", "specific")

        fun newInstance(
            service: ExposedServiceInfo?,
            onSave: (ExposedServiceInfo) -> Unit
        ): AddEditExposedServiceDialog = AddEditExposedServiceDialog(service, onSave)
    }
}
