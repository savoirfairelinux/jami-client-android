/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
package cx.ring.account;

import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cx.ring.R;
import cx.ring.model.Account;

public class AccountsAdapter extends BaseAdapter {
    static final String TAG = AccountsAdapter.class.getSimpleName();
    private final ArrayList<Account> accounts = new ArrayList<>();
    private AccountListeners mListeners;

    public AccountsAdapter(AccountListeners listeners) {
        mListeners = listeners;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return accounts.size();
    }

    @Override
    public Account getItem(int pos) {
        return accounts.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return 0;
    }

    @Override
    public View getView(final int pos, View convertView, ViewGroup parent) {
        View rowView = convertView;
        AccountView entryView;

        if (rowView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            rowView = inflater.inflate(R.layout.item_account_pref, parent, false);

            entryView = new AccountView();
            entryView.alias = (TextView) rowView.findViewById(R.id.account_alias);
            entryView.host = (TextView) rowView.findViewById(R.id.account_host);
            entryView.loadingIndicator = rowView.findViewById(R.id.loading_indicator);
            entryView.errorIndicator = (ImageView) rowView.findViewById(R.id.error_indicator);
            entryView.enabled = (CheckBox) rowView.findViewById(R.id.account_checked);
            entryView.errorIndicator.setColorFilter(parent.getContext().getResources().getColor(R.color.error_red));
            entryView.errorIndicator.setVisibility(View.GONE);
            entryView.loadingIndicator.setVisibility(View.GONE);
            rowView.setTag(entryView);
        } else {
            entryView = (AccountView) rowView.getTag();
        }

        final Account item = accounts.get(pos);
        entryView.alias.setText(item.getAlias());
        entryView.host.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.text_color_secondary));

        if (item.isIP2IP()) {
            entryView.host.setText(item.getRegistrationState());
        } else if (item.isSip()) {
            entryView.host.setText(item.getDisplayUri() + " - " + item.getRegistrationState());
        } else {
            entryView.host.setText(item.getDisplayUri());
        }

        entryView.enabled.setChecked(item.isEnabled());
        entryView.enabled.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                item.setEnabled(!item.isEnabled());
                mListeners.onItemClicked(item.getAccountID(), item.getDetails());
            }
        });

        if (item.isEnabled()) {
            if (item.isTrying()) {
                entryView.errorIndicator.setVisibility(View.GONE);
                entryView.loadingIndicator.setVisibility(View.VISIBLE);
            } else if (item.needsMigration()) {
                entryView.host.setText(R.string.account_update_needed);
                entryView.host.setTextColor(Color.RED);
                entryView.errorIndicator.setImageResource(R.drawable.ic_warning);
                entryView.errorIndicator.setColorFilter(Color.RED);
                entryView.errorIndicator.setVisibility(View.VISIBLE);
            } else if (item.isInError()) {
                entryView.errorIndicator.setImageResource(R.drawable.ic_error_white);
                entryView.errorIndicator.setColorFilter(Color.RED);
                entryView.errorIndicator.setVisibility(View.VISIBLE);
                entryView.loadingIndicator.setVisibility(View.GONE);
            } else if (!item.isRegistered()) {
                entryView.errorIndicator.setImageResource(R.drawable.ic_network_disconnect_black_24dp);
                entryView.errorIndicator.setColorFilter(Color.BLACK);
                entryView.errorIndicator.setVisibility(View.VISIBLE);
                entryView.loadingIndicator.setVisibility(View.GONE);
            } else {
                entryView.errorIndicator.setVisibility(View.GONE);
                entryView.loadingIndicator.setVisibility(View.GONE);
            }
        } else {
            entryView.errorIndicator.setVisibility(View.GONE);
            entryView.loadingIndicator.setVisibility(View.GONE);
        }

        return rowView;
    }

    public void replaceAll(List<Account> results) {
        Log.d(TAG, "AccountsAdapter replaceAll " + results.size());
        accounts.clear();
        accounts.addAll(results);
        notifyDataSetChanged();
    }

    public interface AccountListeners {
        void onItemClicked(String accountId, HashMap<String, String> details);
    }

    /**
     * ******************
     * ViewHolder Pattern
     * *******************
     */
    public class AccountView {
        public TextView alias;
        public TextView host;
        public View loadingIndicator;
        public ImageView errorIndicator;
        public CheckBox enabled;
    }
}
