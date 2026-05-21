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
package cx.ring.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cx.ring.databinding.FragPeerServicesBinding
import cx.ring.databinding.ItemPeerServiceBinding
import cx.ring.viewmodel.PeerServicesUiState
import cx.ring.viewmodel.PeerServicesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.jami.services.PeerServiceInfo
import net.jami.services.TunnelInfo

@AndroidEntryPoint
class PeerServicesBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: PeerServicesViewModel by viewModels()
    private var binding: FragPeerServicesBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragPeerServicesBinding.inflate(inflater, container, false)
        .also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = binding ?: return
        val adapter = ServiceAdapter(
            onConnect    = { viewModel.openTunnel(it) },
            onDisconnect = { serviceId -> viewModel.closeTunnel(serviceId) },
            onCopyLink   = { url -> copyToClipboard(url) },
        )
        b.recyclerServices.adapter = adapter
        b.btnRefresh.setOnClickListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(b, adapter, it) } }
                launch {
                    viewModel.launchUrl.collect { url ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }
                launch {
                    viewModel.copyUrl.collect { url -> copyToClipboard(url) }
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Service endpoint", text))
        Toast.makeText(requireContext(), cx.ring.R.string.peer_services_link_copied, Toast.LENGTH_SHORT).show()
    }

    private fun render(b: FragPeerServicesBinding, adapter: ServiceAdapter, state: PeerServicesUiState) {
        b.progressLoading.isVisible = state.isLoading
        when {
            state.isLoading -> {
                b.textStatus.isVisible = false
                b.recyclerServices.isVisible = false
            }
            state.errorMessage != null -> {
                b.textStatus.text = state.errorMessage
                b.textStatus.isVisible = true
                b.recyclerServices.isVisible = false
            }
            state.services.isEmpty() -> {
                b.textStatus.setText(cx.ring.R.string.peer_services_empty)
                b.textStatus.isVisible = true
                b.recyclerServices.isVisible = false
            }
            else -> {
                b.textStatus.isVisible = false
                b.recyclerServices.isVisible = true
                adapter.submit(state.services, state.activeTunnels, state.pendingTunnels)
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private inner class ServiceAdapter(
        private val onConnect: (PeerServiceInfo) -> Unit,
        private val onDisconnect: (String) -> Unit,
        private val onCopyLink: (String) -> Unit,
    ) : RecyclerView.Adapter<ServiceAdapter.Holder>() {

        private var services: List<PeerServiceInfo> = emptyList()
        private var tunnels: Map<String, TunnelInfo> = emptyMap()
        private var pending: Set<String> = emptySet()

        fun submit(newServices: List<PeerServiceInfo>, newTunnels: Map<String, TunnelInfo>, newPending: Set<String>) {
            services = newServices
            tunnels  = newTunnels
            pending  = newPending
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemPeerServiceBinding.inflate(layoutInflater, parent, false))

        override fun getItemCount() = services.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val service = services[position]
            val tunnel  = tunnels[service.id]
            holder.bind(service, tunnel, pending.contains(service.id))
        }

        inner class Holder(private val b: ItemPeerServiceBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(service: PeerServiceInfo, tunnel: TunnelInfo?, isPending: Boolean) {
                b.serviceName.text = service.name

                val scheme = service.scheme.lowercase()
                val isHttp = scheme == "http" || scheme == "https"
                val endpoint = tunnel?.let { "${service.scheme}://127.0.0.1:${it.localPort}" }

                b.serviceDetail.text = endpoint ?: service.description.ifEmpty { service.scheme }
                b.serviceDetail.setOnClickListener(null)

                val connected = tunnel != null

                if (isHttp) {
                    b.btnConnect.setText(cx.ring.R.string.peer_services_connect)
                    b.btnConnect.isVisible    = !connected
                    b.btnConnect.isEnabled    = !isPending
                    b.btnConnect.setOnClickListener { onConnect(service) }

                    b.btnOpen.isVisible = connected
                    b.btnOpen.setOnClickListener {
                        endpoint?.let { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    }

                    b.btnDisconnect.isVisible = connected
                    b.btnDisconnect.setOnClickListener { onDisconnect(service.id) }
                } else {
                    b.btnConnect.setText(cx.ring.R.string.peer_services_copy)
                    b.btnConnect.isVisible = true
                    b.btnConnect.isEnabled = !isPending
                    b.btnConnect.setOnClickListener {
                        if (connected && endpoint != null) {
                            onCopyLink(endpoint)
                        } else {
                            onConnect(service)
                        }
                    }

                    b.btnOpen.isVisible = false

                    b.btnDisconnect.isVisible = connected
                    b.btnDisconnect.setOnClickListener { onDisconnect(service.id) }
                }

                b.root.contentDescription = buildString {
                    append(service.name)
                    if (tunnel != null) append(", connected on port ${tunnel.localPort}")
                    else if (service.description.isNotEmpty()) append(", ${service.description}")
                }
            }
        }
    }

    companion object {
        const val TAG = "PeerServicesBottomSheet"

        fun newInstance(accountId: String, peerUri: String) = PeerServicesBottomSheet().apply {
            arguments = bundleOf(
                PeerServicesViewModel.ARG_ACCOUNT_ID to accountId,
                PeerServicesViewModel.ARG_PEER_URI   to peerUri,
            )
        }
    }
}
