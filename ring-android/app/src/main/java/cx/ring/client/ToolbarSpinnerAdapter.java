/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
package cx.ring.client;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import cx.ring.R;
import cx.ring.databinding.ItemToolbarSpinnerBinding;
import cx.ring.model.Account;
import cx.ring.views.AvatarDrawable;
import ezvcard.VCard;
import ezvcard.property.FormattedName;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ToolbarSpinnerAdapter extends ArrayAdapter<Account> {
    private static final String TAG = ToolbarSpinnerAdapter.class.getSimpleName();
    public static final int TYPE_ACCOUNT = 0;
    public static final int TYPE_CREATE_JAMI = 1;
    public static final int TYPE_CREATE_SIP = 2;
    private final LayoutInflater mInflater;
    private final int logoSize;

    public ToolbarSpinnerAdapter(@NonNull Context context, List<Account> accounts){
        super(context, R.layout.item_toolbar_spinner, accounts);
        mInflater = LayoutInflater.from(context);
        logoSize = context.getResources().getDimensionPixelSize(R.dimen.list_medium_icon_size);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        int type = getItemViewType(position);

        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            holder.binding = ItemToolbarSpinnerBinding.inflate(mInflater, parent, false);
            convertView = holder.binding.getRoot();
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            if (holder.loader != null)  {
                holder.loader.dispose();
            }
        }

        holder.binding.logo.setVisibility(View.GONE);
        holder.binding.subtitle.setVisibility(View.GONE);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.binding.title.getLayoutParams();
        params.leftMargin = 0;
        holder.binding.title.setLayoutParams(params);
        if (type == TYPE_ACCOUNT) {
            Account account = getItem(position);
            holder.binding.title.setText(getAccountAlias(account));
        }

        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        int type = getItemViewType(position);
        ViewHolder holder;
        View rowView = convertView;
        if (rowView == null) {
            holder = new ViewHolder();
            holder.binding = ItemToolbarSpinnerBinding.inflate(mInflater, parent, false);
            rowView = holder.binding.getRoot();
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
            if (holder.loader != null)  {
                holder.loader.dispose();
            }
        }

        holder.binding.logo.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams logoParam = holder.binding.logo.getLayoutParams();
        if (type == TYPE_ACCOUNT) {
            Account account = getItem(position);
            holder.binding.subtitle.setVisibility(View.VISIBLE);
            holder.binding.title.setText(getAccountAlias(account));
            holder.binding.subtitle.setText(getUri(account, rowView.getContext().getString(R.string.account_type_ip2ip)));
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.binding.title.getLayoutParams();
            params.removeRule(RelativeLayout.CENTER_VERTICAL);
            holder.binding.title.setLayoutParams(params);
            logoParam.width = logoSize;
            logoParam.height = logoSize;
            holder.binding.logo.setLayoutParams(logoParam);
            holder.loader = AvatarDrawable.load(getContext(), account)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(avatar -> holder.binding.logo.setImageDrawable(avatar), e -> Log.e(TAG, "Error loading avatar", e));
        } else {
            if (type == TYPE_CREATE_JAMI)
                holder.binding.title.setText(R.string.add_ring_account_title);
            else
                holder.binding.title.setText(R.string.add_sip_account_title);
            holder.binding.subtitle.setVisibility(View.GONE);
            holder.binding.logo.setImageResource(R.drawable.baseline_add_24);
            logoParam.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            logoParam.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.binding.logo.setLayoutParams(logoParam);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.binding.title.getLayoutParams();
            params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            holder.binding.title.setLayoutParams(params);
        }

        return rowView;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == super.getCount()) {
            return TYPE_CREATE_JAMI;
        }
        if (position == super.getCount() + 1) {
            return TYPE_CREATE_SIP;
        }
        return TYPE_ACCOUNT;
    }

    @Override
    public int getCount() {
        return super.getCount() + 2;
    }

    private static class ViewHolder {
        ItemToolbarSpinnerBinding binding;
        Disposable loader;
    }

    private String getAccountAlias(Account account) {
        if (account == null) {
            Log.e(TAG, "Not able to get account alias");
            return null;
        }
        String alias = getAlias(account);
        return (alias == null) ? account.getAlias() : alias;
    }

    private String getAlias(Account account) {
        if (account == null) {
            Log.e(TAG, "Not able to get alias");
            return null;
        }
        VCard vcard = account.getProfile();
        if (vcard != null) {
            FormattedName name = vcard.getFormattedName();
            if (name != null) {
                String name_value = name.getValue();
                if (name_value != null && !name_value.isEmpty()) {
                    return name_value;
                }
            }
        }
        return null;
    }

    private String getUri(Account account, CharSequence defaultNameSip) {
        if (account.isIP2IP()) {
            return defaultNameSip.toString();
        }
        return account.getDisplayUri();
    }

}
