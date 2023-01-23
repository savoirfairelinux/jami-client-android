/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.client

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import cx.ring.R
import cx.ring.databinding.ItemToolbarSelectedBinding
import cx.ring.databinding.ItemToolbarSpinnerBinding
import cx.ring.utils.DeviceUtils
import cx.ring.views.AvatarDrawable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Account
import net.jami.model.Profile
import net.jami.services.AccountService
import net.jami.services.ConversationFacade


class AccountSpinnerAdapter(context: Context, accounts: List<Account>, val disposable: CompositeDisposable,
                            var mAccountService: AccountService, var mConversationFacade: ConversationFacade) :
    ArrayAdapter<Account>(context, R.layout.item_toolbar_spinner, accounts) {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val logoSize: Int = context.resources.getDimensionPixelSize(R.dimen.list_medium_icon_size)

    private fun getTitle(account: Account, profile: Profile): String =
        profile.displayName.orEmpty().ifEmpty {
            account.registeredName.ifEmpty {
                account.alias.orEmpty().ifEmpty {
                    context.getString(R.string.ring_account)
                }
            }
        }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val type = getItemViewType(position)
        val holder: ViewHolderHeader
        if (view == null) {
            holder = ViewHolderHeader(ItemToolbarSelectedBinding.inflate(mInflater, parent, false), disposable)
            view = holder.binding.root
            view.setTag(holder)
        } else {
            holder = view.tag as ViewHolderHeader
            holder.loader.clear()
        }
        if (type == TYPE_ACCOUNT) {
            val account = getItem(position)!!
            holder.loader.add(mAccountService.getObservableAccountProfile(account.accountId)
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe({ profile ->
                    holder.binding.logo.setImageDrawable(AvatarDrawable.build(holder.binding.root.context, profile.first, profile.second, true, profile.first.isRegistered))
                    holder.binding.title.text = getTitle(profile.first, profile.second)
                }){ e: Throwable -> Log.e(TAG, "Error loading avatar", e) })
        }
        return view
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val type = getItemViewType(position)
        val holder: ViewHolder
        var rowView = convertView
        if (rowView == null) {
            holder = ViewHolder(ItemToolbarSpinnerBinding.inflate(mInflater, parent, false), disposable)
            rowView = holder.binding.root
            rowView.setTag(holder)
        } else {
            holder = rowView.tag as ViewHolder
            holder.loader.clear()
        }
        holder.binding.logo.visibility = View.VISIBLE
        val logoParam = holder.binding.logo.layoutParams
        val badgeDrawable = BadgeDrawable.create(context).apply {
            badgeGravity = BadgeDrawable.BOTTOM_START
        }
        if (type == TYPE_ACCOUNT) {
            val account = getItem(position)!!
            val ip2ipString = rowView.context.getString(R.string.account_type_ip2ip)
            val params = holder.binding.title.layoutParams as RelativeLayout.LayoutParams
            params.removeRule(RelativeLayout.CENTER_VERTICAL)
            holder.binding.title.layoutParams = params
            logoParam.width = logoSize
            logoParam.height = logoSize
            holder.binding.logo.layoutParams = logoParam
            holder.loader.add(mAccountService.getObservableAccountProfile(account.accountId)
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe({ profile ->
                    val subtitle = getUri(account, ip2ipString)
                    holder.binding.logo.setImageDrawable(AvatarDrawable.build(holder.binding.root.context, profile.first, profile.second, true, profile.first.isRegistered))
                    holder.binding.title.text = getTitle(profile.first, profile.second)
                    if (holder.binding.title.text == subtitle) {
                        holder.binding.subtitle.visibility = View.GONE
                    } else {
                        holder.binding.subtitle.visibility = View.VISIBLE
                        holder.binding.subtitle.text = subtitle
                    }
                }){ e: Throwable -> Log.e(TAG, "Error loading avatar", e) })
            holder.loader.add(mConversationFacade.getAccountSubject(account.accountId)
                .flatMapObservable { acc -> acc.unreadConversations }
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe { count ->
                    if (count == 0) {
                        BadgeUtils.detachBadgeDrawable(badgeDrawable, holder.binding.badgeAnchor)
                        holder.binding.badgeAnchor.visibility = View.GONE
                    } else {
                        badgeDrawable.number = count
                        holder.binding.badgeAnchor.visibility = View.VISIBLE
                        BadgeUtils.attachBadgeDrawable(badgeDrawable, holder.binding.badgeAnchor)

                    }
                })
        } else {
            holder.binding.title.setText(
                if (type == TYPE_CREATE_ACCOUNT) R.string.add_ring_account_title else R.string.add_sip_account_title)
            holder.binding.subtitle.visibility = View.GONE
            holder.binding.logo.setImageResource(R.drawable.baseline_add_24)
            logoParam.width = ViewGroup.LayoutParams.WRAP_CONTENT
            logoParam.height = ViewGroup.LayoutParams.WRAP_CONTENT
            holder.binding.logo.layoutParams = logoParam
            val params = holder.binding.title.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
            holder.binding.title.layoutParams = params
            holder.binding.badgeAnchor.visibility = View.GONE
            BadgeUtils.attachBadgeDrawable(badgeDrawable, holder.binding.badgeAnchor)
        }
        return rowView
    }

    override fun getItemViewType(position: Int): Int {
        if (position == super.getCount()) {
            return TYPE_CREATE_ACCOUNT
        }
        return TYPE_ACCOUNT
    }

    override fun getCount(): Int = super.getCount() + 1

    private class ViewHolder(val binding: ItemToolbarSpinnerBinding, parentDisposable: CompositeDisposable) {
        val loader = CompositeDisposable().apply { parentDisposable.add(this) }
    }

    private class ViewHolderHeader(val binding: ItemToolbarSelectedBinding, parentDisposable: CompositeDisposable) {
        val loader = CompositeDisposable().apply { parentDisposable.add(this) }
    }

    private fun getUri(account: Account, defaultNameSip: CharSequence): String =
        if (account.isIP2IP) defaultNameSip.toString() else account.displayUri!!

    companion object {
        private val TAG = AccountSpinnerAdapter::class.simpleName!!
        const val TYPE_ACCOUNT = 0
        const val TYPE_CREATE_ACCOUNT = 1
    }

}