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
package cx.ring.tv.contact

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.databinding.FragTvPeerServicesBinding
import cx.ring.databinding.ItemTvPeerServiceBinding
import cx.ring.viewmodel.PeerServicesUiState
import cx.ring.viewmodel.PeerServicesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.jami.services.PeerServiceInfo
import net.jami.services.TunnelInfo

@AndroidEntryPoint
class TVPeerServicesFragment : DialogFragment() {

    private val viewModel: PeerServicesViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val b = FragTvPeerServicesBinding.inflate(LayoutInflater.from(requireContext()))
        setupView(b)

        return MaterialAlertDialogBuilder(
            requireContext(),
            com.google.android.material.R.style.Theme_MaterialComponents_Dialog
        )
            .setTitle(R.string.peer_services_title)
            .setView(b.root)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun setupView(b: FragTvPeerServicesBinding) {
        val adapter = ServiceAdapter(
            onConnect    = { viewModel.openTunnel(it) },
            onDisconnect = { viewModel.closeTunnel(it) },
            onCopyLink   = { copyToClipboard(it) },
        )
        b.recyclerServices.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerServices.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.uiState.collect { render(b, adapter, it) } }
                launch {
                    viewModel.launchUrl.collect { url ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Service endpoint", text))
        Toast.makeText(requireContext(), R.string.peer_services_link_copied, Toast.LENGTH_SHORT).show()
    }

    private fun render(b: FragTvPeerServicesBinding, adapter: ServiceAdapter, state: PeerServicesUiState) {
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
                b.textStatus.setText(R.string.peer_services_empty)
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
            Holder(ItemTvPeerServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = services.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val svc = services[position]
            holder.bind(svc, tunnels[svc.id], pending.contains(svc.id))
        }

        inner class Holder(private val b: ItemTvPeerServiceBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(service: PeerServiceInfo, tunnel: TunnelInfo?, isPending: Boolean) {
                val scheme   = service.scheme.lowercase()
                val isHttp   = scheme == "http" || scheme == "https"
                val endpoint = tunnel?.let { "${service.scheme}://127.0.0.1:${it.localPort}" }
                val connected = tunnel != null

                b.serviceName.text = service.name
                b.serviceDetail.text = endpoint ?: service.description.ifEmpty { service.scheme }
                b.serviceDetail.isVisible = b.serviceDetail.text.isNotEmpty()

                // Icons set entirely in code — FragmentActivity has no AppCompat inflater factory,
                // so app:srcCompat / app:tint in XML are ignored.
                b.serviceIcon.setImageDrawable(
                    ContextCompat.getDrawable(b.root.context, R.drawable.ic_peer_connection_24)
                        ?.mutate()?.apply {
                            setTint(resources.getColor(
                                if (connected) R.color.green_400 else R.color.grey_500, null))
                        }
                )
                b.btnDisconnect.setImageDrawable(
                    ContextCompat.getDrawable(b.root.context, R.drawable.stop_circle_24px)
                        ?.mutate()?.apply { setTint(resources.getColor(R.color.red_400, null)) }
                )

                b.root.alpha = if (isPending) 0.5f else 1.0f

                b.mainAction.setOnClickListener {
                    if (isPending) return@setOnClickListener
                    if (connected && endpoint != null) {
                        if (isHttp) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(endpoint)))
                        else onCopyLink(endpoint)
                    } else {
                        onConnect(service)
                    }
                }

                b.btnDisconnect.isVisible = connected
                b.btnDisconnect.setOnClickListener { onDisconnect(service.id) }
            }
        }
    }

    companion object {
        const val TAG = "TVPeerServicesFragment"

        fun newInstance(accountId: String, peerUri: String) = TVPeerServicesFragment().apply {
            arguments = bundleOf(
                PeerServicesViewModel.ARG_ACCOUNT_ID to accountId,
                PeerServicesViewModel.ARG_PEER_URI   to peerUri,
            )
        }
    }
}
