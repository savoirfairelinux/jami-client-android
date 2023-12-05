/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package cx.ring.client

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import cx.ring.R
import cx.ring.databinding.ItemToolbarSpinnerBinding
import cx.ring.utils.DeviceUtils
import cx.ring.views.AvatarDrawable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Account
import net.jami.model.Profile
import net.jami.services.AccountService
import net.jami.services.ConversationFacade


/**
 * Adapter allowing to select account user want to use.
 */
class AccountAdapter(
    context: Context,
    accounts: List<Account>,
    val disposable: CompositeDisposable,
    var mAccountService: AccountService,
    var mConversationFacade: ConversationFacade,
) : ArrayAdapter<Account>(context, R.layout.item_toolbar_spinner, accounts) {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val ip2ipString = context.getString(R.string.account_type_ip2ip)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        // Try to recycle the view
        val holder: ViewHolder
        var view = convertView
        if (view != null) {
            holder = view.tag as ViewHolder
            holder.loader.clear()
            holder.binding.logo.setImageDrawable(null)
            holder.binding.title.text = null
            holder.binding.subtitle.text = null
        } else { // Create a new view
            holder = ViewHolder(
                ItemToolbarSpinnerBinding.inflate(mInflater, parent, false),
                disposable
            )
            view = holder.binding.root
            view.setTag(holder)
        }

        // Items can be:
        // - available accounts (type=TYPE_ACCOUNT)
        // - a button to create a new account (type=TYPE_CREATE_ACCOUNT)
        val type = getItemViewType(position)
        if (type == TYPE_ACCOUNT) {
            val account = getItem(position)!!

            // Subscribe to account profile changes to update:
            // - avatar
            // - name (will display the jami if name is not setup)
            holder.loader.add(mAccountService.getObservableAccountProfile(account.accountId)
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe({ profile ->
                    val subtitle = getUri(account, ip2ipString)
                    holder.binding.logo.setImageDrawable(
                        AvatarDrawable.build(
                            holder.binding.root.context,
                            profile.first,
                            profile.second,
                            true,
                            profile.first.isRegistered
                        )
                    )
                    holder.binding.title.text = getTitle(profile.first, profile.second)
                    if (holder.binding.title.text == subtitle) {
                        holder.binding.subtitle.visibility = View.GONE
                    } else {
                        holder.binding.subtitle.visibility = View.VISIBLE
                        holder.binding.subtitle.text = subtitle
                    }
                }) { e: Throwable -> Log.e(TAG, "Error loading avatar", e) })
        } else {
            holder.binding.invitationBadge.visibility = View.GONE
            holder.binding.title.setText(
                if (type == TYPE_CREATE_ACCOUNT) R.string.add_ring_account_title
                else R.string.add_sip_account_title
            )
            holder.binding.logo.setImageResource(R.drawable.baseline_add_24)
            holder.binding.subtitle.visibility = View.GONE
        }
        return view
    }

    override fun getItemViewType(position: Int): Int {
        if (position == super.getCount()) {
            return TYPE_CREATE_ACCOUNT
        }
        return TYPE_ACCOUNT
    }

    override fun getCount(): Int = super.getCount() + 1

    private fun getTitle(account: Account, profile: Profile): String =
        profile.displayName.orEmpty().ifEmpty {
            account.registeredName.ifEmpty {
                account.alias.orEmpty().ifEmpty {
                    if (account.isSip) context.getString(R.string.sip_account)
                    else context.getString(R.string.ring_account)
                }
            }
        }

    private class ViewHolder(val binding: ItemToolbarSpinnerBinding, parentDisposable: CompositeDisposable) {
        val loader = CompositeDisposable().apply { parentDisposable.add(this) }
    }

    private fun getUri(account: Account, defaultNameSip: CharSequence): String =
        if (account.isIP2IP) defaultNameSip.toString() else account.displayUri!!

    companion object {
        private val TAG = AccountAdapter::class.simpleName!!
        const val TYPE_ACCOUNT = 0
        const val TYPE_CREATE_ACCOUNT = 1
    }

}