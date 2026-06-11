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

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import cx.ring.R
import cx.ring.databinding.FragExposedServicesSettingsBinding
import cx.ring.databinding.ItemExposedServiceBinding
import cx.ring.viewmodel.ExposedServicesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.jami.services.ExposedServiceInfo
import net.jami.services.ExposedServiceType

@AndroidEntryPoint
class ExposedServicesSettingsFragment : Fragment() {

    private val viewModel: ExposedServicesViewModel by viewModels()
    private var binding: FragExposedServicesSettingsBinding? = null
    private var accountId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountId = arguments?.getString(ACCOUNT_ID_KEY) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragExposedServicesSettingsBinding.inflate(inflater, container, false)
        .also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return

        val adapter = ExposedServiceAdapter(
            onEdit = { service -> openEditDialog(service) },
            onDelete = { service -> deleteService(service) },
            onToggle = { service -> toggleService(service) }
        )

        b.recyclerServices.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerServices.adapter = adapter

        b.fabAddService.setOnClickListener { openAddDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    b.progressLoading.isVisible = state.isLoading
                    when {
                        state.isLoading -> {
                            b.layoutEmpty.isVisible = false
                            b.recyclerServices.isVisible = false
                        }
                        state.errorMessage != null -> {
                            Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                        state.services.isEmpty() -> {
                            b.layoutEmpty.isVisible = true
                            b.recyclerServices.isVisible = false
                        }
                        else -> {
                            b.layoutEmpty.isVisible = false
                            b.recyclerServices.isVisible = true
                            adapter.submitList(state.services)
                        }
                    }
                }
            }
        }

        viewModel.loadServices(accountId)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun openAddDialog() {
        AddEditExposedServiceDialog.newInstance(null) { service ->
            viewModel.addService(accountId, service)
        }.show(childFragmentManager, "add_service_dialog")
    }

    private fun openEditDialog(service: ExposedServiceInfo) {
        AddEditExposedServiceDialog.newInstance(service) { updated ->
            viewModel.updateService(accountId, updated)
        }.show(childFragmentManager, "edit_service_dialog")
    }

    private fun deleteService(service: ExposedServiceInfo) {
        viewModel.removeService(accountId, service.id)
    }

    private fun toggleService(service: ExposedServiceInfo) {
        viewModel.toggleEnabled(accountId, service)
    }

    companion object {
        private const val ACCOUNT_ID_KEY = "account_id"

        fun newInstance(accountId: String) = ExposedServicesSettingsFragment().apply {
            arguments = bundleOf(ACCOUNT_ID_KEY to accountId)
        }
    }
}

private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ExposedServiceInfo>() {
    override fun areItemsTheSame(oldItem: ExposedServiceInfo, newItem: ExposedServiceInfo) =
        oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: ExposedServiceInfo, newItem: ExposedServiceInfo) =
        oldItem == newItem
}

class ExposedServiceAdapter(
    private val onEdit: (ExposedServiceInfo) -> Unit,
    private val onDelete: (ExposedServiceInfo) -> Unit,
    private val onToggle: (ExposedServiceInfo) -> Unit,
) : ListAdapter<ExposedServiceInfo, ExposedServiceViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ExposedServiceViewHolder(
            ItemExposedServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onEdit, onDelete, onToggle
        )

    override fun onBindViewHolder(holder: ExposedServiceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ExposedServiceViewHolder(
    private val binding: ItemExposedServiceBinding,
    private val onEdit: (ExposedServiceInfo) -> Unit,
    private val onDelete: (ExposedServiceInfo) -> Unit,
    private val onToggle: (ExposedServiceInfo) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(service: ExposedServiceInfo) {
        val ctx = itemView.context
        val isWebsite = service.type == ExposedServiceType.EMBEDDED

        if (isWebsite) {
            binding.serviceTypeIcon.setImageResource(R.drawable.baseline_language_18)
            val containerColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorPrimaryContainer)
            val iconColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorOnPrimaryContainer)
            binding.iconContainer.backgroundTintList = ColorStateList.valueOf(containerColor)
            binding.serviceTypeIcon.imageTintList = ColorStateList.valueOf(iconColor)
            binding.serviceTypeLabel.text = ctx.getString(R.string.shared_services_type_website_label)
            binding.serviceTypeLabel.backgroundTintList = ColorStateList.valueOf(containerColor)
            binding.serviceTypeLabel.setTextColor(iconColor)
        } else {
            binding.serviceTypeIcon.setImageResource(R.drawable.baseline_dns_24)
            val containerColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorSecondaryContainer)
            val iconColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorOnSecondaryContainer)
            binding.iconContainer.backgroundTintList = ColorStateList.valueOf(containerColor)
            binding.serviceTypeIcon.imageTintList = ColorStateList.valueOf(iconColor)
            binding.serviceTypeLabel.text = ctx.getString(R.string.shared_services_type_custom_label)
            binding.serviceTypeLabel.backgroundTintList = ColorStateList.valueOf(containerColor)
            binding.serviceTypeLabel.setTextColor(iconColor)
        }

        binding.serviceName.text = service.name

        binding.servicePolicy.text = when (service.policy) {
            "contacts" -> ctx.getString(R.string.shared_services_policy_contacts)
            "public"   -> ctx.getString(R.string.shared_services_policy_public)
            "specific" -> ctx.getString(R.string.shared_services_policy_specific)
            else       -> ""
        }

        val desc = service.description.trim()
        binding.serviceDescription.isVisible = desc.isNotEmpty()
        binding.serviceDescription.text = desc

        val address = when {
            isWebsite && service.localPort > 0 ->
                ctx.getString(R.string.shared_services_local_port, service.localPort)
            !isWebsite && service.localPort > 0 ->
                ctx.getString(R.string.shared_services_local_address, service.localHost, service.localPort)
            else -> ""
        }
        binding.serviceAddress.isVisible = address.isNotEmpty()
        binding.serviceAddress.text = address

        binding.toggleEnabled.setOnCheckedChangeListener(null)
        binding.toggleEnabled.isChecked = service.enabled
        binding.toggleEnabled.setOnCheckedChangeListener { _, _ -> onToggle(service) }

        // Open-in-browser button: visible only when a local port is assigned
        val port = service.localPort
        val hasServer = port > 0
        binding.btnOpen.isVisible = hasServer
        if (hasServer) {
            val scheme = if (isWebsite) "http" else (service.scheme.ifBlank { "http" })
            val host = service.localHost.ifBlank { "localhost" }
            val url = "$scheme://$host:$port"
            binding.btnOpen.setOnClickListener {
                it.context.startActivity(
                    android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(url))
                )
            }
        }

        binding.btnMore.setOnClickListener { anchor ->
            val popup = PopupMenu(anchor.context, anchor)
            popup.menuInflater.inflate(R.menu.menu_service_item, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit   -> { onEdit(service); true }
                    R.id.action_delete -> { onDelete(service); true }
                    else               -> false
                }
            }
            popup.show()
        }
    }
}
