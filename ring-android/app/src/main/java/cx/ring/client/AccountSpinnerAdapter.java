/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import net.jami.model.Account;
import cx.ring.views.AvatarDrawable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class AccountSpinnerAdapter extends ArrayAdapter<Account> {
    private static final String TAG = AccountSpinnerAdapter.class.getSimpleName();
    public static final int TYPE_ACCOUNT = 0;
    public static final int TYPE_CREATE_JAMI = 1;
    public static final int TYPE_CREATE_SIP = 2;
    private final LayoutInflater mInflater;
    private final int logoSize;

    public AccountSpinnerAdapter(@NonNull Context context, List<Account> accounts){
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
            holder.loader.clear();
        }

        holder.binding.logo.setVisibility(View.GONE);
        holder.binding.subtitle.setVisibility(View.GONE);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.binding.title.getLayoutParams();
        params.leftMargin = 0;
        holder.binding.title.setLayoutParams(params);
        if (type == TYPE_ACCOUNT) {
            Account account = getItem(position);
            holder.loader.add(account.getAccountAlias()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(alias -> holder.binding.title.setText(alias)));
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
            holder.loader.clear();
        }

        holder.binding.logo.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams logoParam = holder.binding.logo.getLayoutParams();
        if (type == TYPE_ACCOUNT) {
            Account account = getItem(position);
            CharSequence ip2ipString = rowView.getContext().getString(R.string.account_type_ip2ip);
            holder.loader.add(account.getAccountAlias()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(alias -> {
                        String subtitle = getUri(account, ip2ipString);
                        holder.binding.title.setText(alias);
                        if (alias.equals(subtitle)) {
                            holder.binding.subtitle.setVisibility(View.GONE);
                        } else {
                            holder.binding.subtitle.setVisibility(View.VISIBLE);
                            holder.binding.subtitle.setText(subtitle);
                        }
                    }));
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.binding.title.getLayoutParams();
            params.removeRule(RelativeLayout.CENTER_VERTICAL);
            holder.binding.title.setLayoutParams(params);
            logoParam.width = logoSize;
            logoParam.height = logoSize;
            holder.binding.logo.setLayoutParams(logoParam);
            holder.loader.add(AvatarDrawable.load(getContext(), account)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(avatar -> holder.binding.logo.setImageDrawable(avatar), e -> Log.e(TAG, "Error loading avatar", e)));
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
        final CompositeDisposable loader = new CompositeDisposable();
    }

    private String getUri(Account account, CharSequence defaultNameSip) {
        if (account.isIP2IP()) {
            return defaultNameSip.toString();
        }
        return account.getDisplayUri();
    }

}
