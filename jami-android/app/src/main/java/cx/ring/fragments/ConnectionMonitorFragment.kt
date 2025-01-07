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
package cx.ring.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.databinding.FragConnectionMonitorBinding
import cx.ring.databinding.ItemDeviceConnectionBinding
import cx.ring.databinding.ItemListContactBinding
import cx.ring.interfaces.AppBarStateListener
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.ContactViewModel
import net.jami.services.AccountService
import net.jami.services.AccountService.ConnectionStatus
import net.jami.services.ContactService
import javax.inject.Inject

@AndroidEntryPoint
class ConnectionMonitorFragment: Fragment() {

    @Inject
    lateinit var service: AccountService
    @Inject
    lateinit var contactService: ContactService

    private var list: RecyclerView? = null
    private val disposableBag = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragConnectionMonitorBinding.inflate(inflater, container, false).apply {
            root.adapter = ConnectionAdapter()
            list = root
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (parentFragment as? AppBarStateListener)?.apply {
            onToolbarTitleChanged(getText(R.string.pref_connection_monitor))
            onAppBarScrollTargetViewChanged(list)
        }
    }

    data class DeviceConnectionViewModel(
        val contact: ContactViewModel?,
        val connection: AccountService.DeviceConnection?
    )

    class ConnectionAdapter(var connections: List<DeviceConnectionViewModel> = emptyList()): RecyclerView.Adapter<ConnectionAdapter.ConnectionViewHolder>() {
        class ConnectionViewHolder(val binding: ItemListContactBinding?, val connBinding: ItemDeviceConnectionBinding?): RecyclerView.ViewHolder(binding?.root ?: connBinding!!.root)

        override fun getItemViewType(position: Int): Int =
            if (connections[position].contact != null) 0 else 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder =
            if (viewType == 0)
                ConnectionViewHolder(ItemListContactBinding.inflate(LayoutInflater.from(parent.context), parent, false), null)
            else
                ConnectionViewHolder(null, ItemDeviceConnectionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
            holder.connBinding?.let {
                val connection = connections[position].connection!!
                it.name.text = connection.device
                it.device.text = connection.remoteAddress
                it.device.isVisible = !connection.remoteAddress.isNullOrEmpty()
                it.icon.setImageResource(when (connection.status) {
                    ConnectionStatus.Waiting -> 0
                    ConnectionStatus.Connecting -> R.drawable.baseline_radar_24
                    ConnectionStatus.ICE -> R.drawable.p2p_24
                    ConnectionStatus.TLS -> R.drawable.baseline_private_connectivity_24
                    ConnectionStatus.Connected -> R.drawable.baseline_private_connectivity_24
                })
                it.icon.imageTintList = when (connection.status) {
                    ConnectionStatus.Connected -> ContextCompat.getColorStateList(it.root.context, R.color.green_500)
                    else -> ContextCompat.getColorStateList(it.root.context, R.color.icon_color)
                }
                it.icon.contentDescription = connection.status.toString()
            }
            holder.binding?.let {
                val contact = connections[position].contact!!
                it.photo.setAvatar(AvatarDrawable.Builder()
                    .withContact(contact)
                    .withCircleCrop(true)
                    .build(it.root.context))
                it.convParticipant.text = contact.displayName
                it.convLastItem.text = contact.displayUri
            }
        }

        fun setData(newConnections: List<DeviceConnectionViewModel>) {
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = connections.size
                override fun getNewListSize(): Int = newConnections.size
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = connections[oldItemPosition]
                    val newItem = newConnections[newItemPosition]
                    return if (oldItem.contact != null && newItem.contact != null) {
                        oldItem.contact.contact.uri == newItem.contact.contact.uri
                    } else {
                        oldItem.connection?.id == newItem.connection?.id
                    }
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = connections[oldItemPosition]
                    val newItem = newConnections[newItemPosition]
                    return if (oldItem.contact != null && newItem.contact != null) {
                        oldItem.contact.contact.uri == newItem.contact.contact.uri
                                && oldItem.contact.displayName == newItem.contact.displayName
                    } else {
                        oldItem.connection?.id == newItem.connection?.id
                                && oldItem.connection?.status == newItem.connection?.status
                    }
                }

            })
            connections = newConnections
            diff.dispatchUpdatesTo(this)
        }
        override fun getItemCount(): Int = connections.size
    }

    override fun onStart() {
        super.onStart()
        disposableBag.add((service.monitorConnections()
            .switchMapSingle { (accountId, connections) ->
                if (connections.isEmpty())
                    Single.just(emptyList())
                else
                    Single.zip(connections
                        .map { (peer, connections) ->
                            contactService.getLoadedContact(accountId, peer).map { Pair(it, connections) }
                        }) { it.map { it as Pair<ContactViewModel, List<AccountService.DeviceConnection>> } }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val adapter = list?.adapter as? ConnectionAdapter
                adapter?.setData(it.map { (contact, connections) ->
                    val list = ArrayList<DeviceConnectionViewModel>(1 + connections.size)
                    list.add(DeviceConnectionViewModel(contact, null))
                    connections.forEach { connection ->
                        list.add(DeviceConnectionViewModel(null, connection))
                    }
                    list
                }.flatten())
            }))
    }

    override fun onStop() {
        super.onStop()
        disposableBag.clear()
    }

}