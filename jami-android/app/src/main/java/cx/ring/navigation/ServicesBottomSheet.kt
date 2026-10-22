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
package cx.ring.navigation

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.databinding.ItemOfferedServiceBinding
import cx.ring.databinding.SheetServicesBinding
import cx.ring.repositories.TargetRepository
import cx.ring.utils.DeviceUtils
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import net.jami.model.Account
import net.jami.model.Target
import net.jami.model.TargetCapability
import net.jami.services.AccountService
import net.jami.services.OfferedService
import net.jami.services.OfferedServices
import net.jami.services.PeerServicesResult
import net.jami.services.PeerServicesService
import net.jami.services.PeerServicesStatus
import net.jami.services.PeerTunnelState
import net.jami.services.TunnelInfo
import net.jami.utils.Log
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The shared services the contacts of the account offer, each with the contact
 * it comes from. Tapping one connects to it: a web service then opens in the
 * browser, any other gives the local address it is reached at.
 *
 * "My services" leads to the services the user shares himself.
 */
@AndroidEntryPoint
class ServicesBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var targetRepository: TargetRepository

    @Inject
    lateinit var peerServicesService: PeerServicesService

    private val disposables = CompositeDisposable()
    private val gathered = BehaviorSubject.create<Gathering>()
    private var binding: SheetServicesBinding? = null
    private val adapter = ServiceAdapter()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            // Shown whole, the sheet grows with the list, up to most of the screen
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.maxHeight = maxSheetHeight()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = SheetServicesBinding.inflate(inflater, container, false).apply {
        serviceList.adapter = adapter
        myServices.setOnClickListener {
            val home = activity as? HomeActivity
            dismiss()
            home?.goToAdvancedSettings(true)
        }
        binding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        disposables.add(accountService.currentAccountSubject
            .firstOrError()
            .flatMapObservable { account -> gather(account) }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe(gathered::onNext) { e ->
                Log.e(TAG, "Can't list the services offered", e)
                gathered.onNext(Gathering("", emptyMap(), OfferedServices.asking(emptyList())))
            })
        disposables.add(gathered
            .switchMap { gathering -> listing(gathering) }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { listing -> show(listing) })
        // A web service opens in the browser as soon as it is connected
        disposables.add(gathered
            .map { gathering -> gathering.accountId to gathering.providers }
            .distinctUntilChanged()
            .switchMap { (accountId, providers) ->
                Observable.merge(providers.map { peerServicesService.observeTunnelUrl(accountId, it) })
            }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { url -> openInBrowser(url) })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // The activity handles rotations itself: keep to the new screen height
        (dialog as? BottomSheetDialog)?.behavior?.maxHeight = maxSheetHeight()
    }

    override fun onDestroyView() {
        disposables.clear()
        binding = null
        super.onDestroyView()
    }

    private fun maxSheetHeight() = (resources.displayMetrics.heightPixels * MAX_HEIGHT_RATIO).toInt()

    /** Asks each contact of the account, once, what it offers. */
    private fun gather(account: Account): Observable<Gathering> {
        val accountId = account.accountId
        if (!account.isJami)
            return Observable.just(Gathering(accountId, emptyMap(), OfferedServices.asking(emptyList())))
        return targetRepository.getTargets(TargetCapability.CAN_MESSAGE)
            .flatMapObservable { targets ->
                val contacts = targets.filterIsInstance<Target.ContactTarget>().associateBy { it.uri.uri }
                Observable.fromIterable(contacts.keys)
                    .flatMap { peer -> ask(accountId, peer) }
                    .scan(OfferedServices.asking(contacts.keys)) { offered, answer -> offered.with(answer) }
                    .map { offered -> Gathering(accountId, contacts, offered) }
            }
    }

    private fun ask(accountId: String, peer: String): Observable<PeerServicesResult> =
        Observable.defer { peerServicesService.queryPeerServices(accountId, peer) }
            // The daemon answers at once from what it already knows: an answer that
            // never comes (lost, or the account not ready) must not keep the list loading
            .timeout(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .onErrorReturn { e ->
                Log.w(TAG, "No answer from $peer about its services", e)
                PeerServicesResult(0, accountId, peer, PeerServicesStatus.TIMEOUT, emptyList())
            }

    /** The services offered so far, each with the state of its tunnel. */
    private fun listing(gathering: Gathering): Observable<Listing> {
        val offered = gathering.offered
        val providers = gathering.providers
        if (providers.isEmpty()) return Observable.just(Listing(emptyList(), offered.isLoading))
        return Observable.combineLatest(
            providers.map { peerServicesService.observeTunnelState(gathering.accountId, it) }
        ) { values ->
            val tunnels = providers.zip(values.map { it as PeerTunnelState }).toMap()
            val rows = offered.services.map { service ->
                val state = tunnels.getValue(service.peerUri)
                ServiceRow(
                    accountId = gathering.accountId,
                    offered = service,
                    provider = gathering.contacts.getValue(service.peerUri),
                    tunnel = state.activeTunnels[service.service.id],
                    connecting = service.service.id in state.pendingServices,
                )
            }
            Listing(rows, offered.isLoading)
        }
    }

    private fun show(listing: Listing) {
        val b = binding ?: return
        b.serviceList.isVisible = listing.rows.isNotEmpty()
        b.progress.isVisible = listing.loading
        b.emptyHint.isVisible = !listing.loading && listing.rows.isEmpty()
        adapter.submitList(listing.rows)
    }

    private fun activate(row: ServiceRow) {
        val endpoint = row.endpoint
        when {
            row.connecting -> {}
            endpoint == null -> peerServicesService.openTunnel(row.accountId, row.offered.peerUri, row.offered.service)
            row.isWeb -> openInBrowser(endpoint)
            else -> copyEndpoint(endpoint)
        }
    }

    private fun disconnect(row: ServiceRow) {
        peerServicesService.closeTunnel(row.accountId, row.offered.peerUri, row.offered.service.id)
    }

    private fun openInBrowser(url: String) {
        val context = context ?: return
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Nothing can open $url", e)
        }
    }

    private fun copyEndpoint(endpoint: String) {
        val context = context ?: return
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("Service endpoint", endpoint))
        Toast.makeText(context, R.string.peer_services_link_copied, Toast.LENGTH_SHORT).show()
    }

    /** The contacts asked, by uri, and what they offer so far. */
    private class Gathering(
        val accountId: String,
        val contacts: Map<String, Target.ContactTarget>,
        val offered: OfferedServices,
    ) {
        /** The contacts that offer something. */
        val providers: List<String> = offered.services.map { it.peerUri }.distinct()
    }

    private class Listing(val rows: List<ServiceRow>, val loading: Boolean)

    private data class ServiceRow(
        val accountId: String,
        val offered: OfferedService,
        val provider: Target.ContactTarget,
        /** Set once connected. */
        val tunnel: TunnelInfo?,
        val connecting: Boolean,
    ) {
        val isWeb: Boolean
            get() = offered.service.scheme.lowercase().let { it == "http" || it == "https" }

        /** Where the service is reached from this device, once connected. */
        val endpoint: String?
            get() = tunnel?.let { tunnel ->
                val address = "127.0.0.1:${tunnel.localPort}"
                // A custom service may come without a scheme: its address alone then
                offered.service.scheme.takeIf { it.isNotEmpty() }?.let { "$it://$address" } ?: address
            }
    }

    private inner class ServiceAdapter : ListAdapter<ServiceRow, ServiceHolder>(DIFF) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ServiceHolder(
            ItemOfferedServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: ServiceHolder, position: Int) {
            val row = getItem(position)
            val service = row.offered.service
            val b = holder.binding
            val context = b.root.context
            val endpoint = row.endpoint
            val connected = endpoint != null
            b.serviceName.text = service.name
            b.providerAvatar.setImageDrawable(row.provider.viewModel?.let { vm ->
                AvatarDrawable.Builder()
                    .withViewModel(vm)
                    .withCircleCrop(true)
                    .build(context)
            })
            b.providerName.text = context.getString(R.string.services_offered_by, row.provider.displayName)
            b.serviceDetails.text = endpoint ?: service.description.ifEmpty { service.scheme }
            b.serviceDetails.isVisible = b.serviceDetails.text.isNotEmpty()
            b.serviceDetails.setTextColor(if (connected) ColorStateList.valueOf(SERVICES_COLOR) else holder.detailsColor)
            b.serviceBadge.backgroundTintList =
                ColorStateList.valueOf(if (connected) SERVICES_COLOR else SERVICES_BACKGROUND)
            b.serviceIcon.imageTintList = ColorStateList.valueOf(if (connected) Color.WHITE else SERVICES_COLOR)
            b.disconnect.isVisible = connected
            b.disconnect.setOnClickListener { disconnect(row) }
            b.root.alpha = if (row.connecting) CONNECTING_ALPHA else 1f
            b.root.setOnClickListener { activate(row) }
        }
    }

    private class ServiceHolder(val binding: ItemOfferedServiceBinding) : RecyclerView.ViewHolder(binding.root) {
        val detailsColor: ColorStateList = binding.serviceDetails.textColors
    }

    companion object {
        const val TAG = "ServicesBottomSheet"

        private const val MAX_HEIGHT_RATIO = 0.85f
        private const val QUERY_TIMEOUT_SECONDS = 10L
        private const val CONNECTING_ALPHA = 0.5f

        /** The colour of the Services function, as on its card. */
        private const val SERVICES_COLOR = 0xFF14B8A6.toInt()
        private const val SERVICES_BACKGROUND = 0x2614B8A6

        private val DIFF = object : DiffUtil.ItemCallback<ServiceRow>() {
            override fun areItemsTheSame(oldItem: ServiceRow, newItem: ServiceRow) =
                oldItem.offered.peerUri == newItem.offered.peerUri &&
                    oldItem.offered.service.id == newItem.offered.service.id

            override fun areContentsTheSame(oldItem: ServiceRow, newItem: ServiceRow) =
                oldItem == newItem
        }

        fun newInstance() = ServicesBottomSheet()
    }
}
