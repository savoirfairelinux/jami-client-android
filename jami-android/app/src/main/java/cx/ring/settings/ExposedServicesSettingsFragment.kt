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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.databinding.FragExposedServicesSettingsBinding
import cx.ring.databinding.ItemExposedServiceBinding
import cx.ring.viewmodel.ExposedServicesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.jami.services.ExposedServiceInfo

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

        b.fabAddService.setOnClickListener {
            openAddDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    b.progressLoading.isVisible = state.isLoading
                    when {
                        state.isLoading -> {
                            b.textEmpty.isVisible = false
                            b.recyclerServices.isVisible = false
                        }
                        state.errorMessage != null -> {
                            Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                        state.services.isEmpty() -> {
                            b.textEmpty.setText(R.string.shared_services_empty)
                            b.textEmpty.isVisible = true
                            b.recyclerServices.isVisible = false
                        }
                        else -> {
                            b.textEmpty.isVisible = false
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
        val dialog = AddEditExposedServiceDialog.newInstance(null) { service ->
            viewModel.addService(accountId, service)
        }
        dialog.show(childFragmentManager, "add_service_dialog")
    }

    private fun openEditDialog(service: ExposedServiceInfo) {
        val dialog = AddEditExposedServiceDialog.newInstance(service) { updated ->
            viewModel.updateService(accountId, updated)
        }
        dialog.show(childFragmentManager, "edit_service_dialog")
    }

    private fun deleteService(service: ExposedServiceInfo) {
        viewModel.removeService(accountId, service.id)
    }

    private fun toggleService(service: ExposedServiceInfo) {
        viewModel.toggleEnabled(accountId, service)
    }

    companion object {
        private const val ACCOUNT_ID_KEY = "account_id"

        fun newInstance(accountId: String): ExposedServicesSettingsFragment {
            return ExposedServicesSettingsFragment().apply {
                arguments = bundleOf(ACCOUNT_ID_KEY to accountId)
            }
        }
    }
}

class ExposedServiceAdapter(
    private val onEdit: (ExposedServiceInfo) -> Unit,
    private val onDelete: (ExposedServiceInfo) -> Unit,
    private val onToggle: (ExposedServiceInfo) -> Unit,
) : RecyclerView.Adapter<ExposedServiceViewHolder>() {

    private val items = mutableListOf<ExposedServiceInfo>()

    fun submitList(newItems: List<ExposedServiceInfo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExposedServiceViewHolder {
        return ExposedServiceViewHolder(
            ItemExposedServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onEdit, onDelete, onToggle
        )
    }

    override fun onBindViewHolder(holder: ExposedServiceViewHolder, position: Int) {
        holder.bind(items[position])
    }
}

class ExposedServiceViewHolder(
    private val binding: ItemExposedServiceBinding,
    private val onEdit: (ExposedServiceInfo) -> Unit,
    private val onDelete: (ExposedServiceInfo) -> Unit,
    private val onToggle: (ExposedServiceInfo) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(service: ExposedServiceInfo) {
        binding.serviceName.text = service.name
        binding.serviceDescription.text = if (service.description.isNotEmpty()) {
            service.description
        } else {
            itemView.context.getString(R.string.shared_services_no_description)
        }
        binding.servicePort.text = itemView.context.getString(R.string.shared_services_port, service.localPort)

        // Clear listener before setting checked state to avoid spurious toggle
        // during RecyclerView layout (recycled views fire the old listener)
        binding.toggleEnabled.setOnCheckedChangeListener(null)
        binding.toggleEnabled.isChecked = service.enabled
        binding.toggleEnabled.setOnCheckedChangeListener { _, _ ->
            onToggle(service)
        }

        binding.btnEdit.setOnClickListener { onEdit(service) }
        binding.btnDelete.setOnClickListener { onDelete(service) }
    }
}
