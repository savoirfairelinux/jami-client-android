/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.model.Account;

public class AccountsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    static final String TAG = AccountsAdapter.class.getSimpleName();
    private final ArrayList<Account> accounts = new ArrayList<>();
    private AccountListeners mListeners;

    public AccountsAdapter(AccountListeners listeners) {
        mListeners = listeners;
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    public Account getItem(int position) {
        return accounts.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View holderView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_account_pref, parent, false);
        return new AccountView(holderView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AccountView entryView = (AccountView) holder;
        final Account item = accounts.get(position);
        entryView.alias.setText(item.getAlias());
        entryView.host.setTextColor(ContextCompat.getColor(entryView.itemView.getContext(), R.color.text_color_secondary));

        if (item.isIP2IP()) {
            entryView.host.setText(item.getRegistrationState());
        } else if (item.isSip()) {
            entryView.host.setText(item.getDisplayUri() + " - " + item.getRegistrationState());
        } else {
            entryView.host.setText(item.getDisplayUri());
        }

        entryView.enabled.setChecked(item.isEnabled());
        entryView.enabled.setOnClickListener(v -> {
            item.setEnabled(!item.isEnabled());
            mListeners.onAccountEnabled(item.getAccountID(), item.isEnabled());
        });

        if (item.isEnabled()) {
            if (!item.isActive()) {
                entryView.errorIndicator.setImageResource(R.drawable.baseline_sync_disabled_24px);
                entryView.errorIndicator.setColorFilter(Color.BLACK);
                entryView.errorIndicator.setVisibility(View.VISIBLE);
                entryView.loadingIndicator.setVisibility(View.GONE);
            } else if (item.isTrying()) {
                entryView.errorIndicator.setVisibility(View.GONE);
                entryView.loadingIndicator.setVisibility(View.VISIBLE);
            } else if (item.needsMigration()) {
                entryView.host.setText(R.string.account_update_needed);
                entryView.host.setTextColor(Color.RED);
                entryView.errorIndicator.setImageResource(R.drawable.ic_warning);
                entryView.errorIndicator.setColorFilter(Color.RED);
                entryView.errorIndicator.setVisibility(View.VISIBLE);
            } else if (item.isInError() || !item.isRegistered()) {
                entryView.errorIndicator.setImageResource(R.drawable.ic_error_white);
                entryView.errorIndicator.setColorFilter(Color.RED);
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
    }

    public void replaceAll(List<Account> results) {
        Log.d(TAG, "replaceAll: AccountsAdapter replaceAll " + results.size());
        accounts.clear();
        accounts.addAll(results);
        notifyDataSetChanged();
    }
    public void replaceAccount(Account account) {
        Log.d(TAG, "replaceAccount " + account);
        notifyDataSetChanged();
    }

    public interface AccountListeners {
        void onItemClicked(Account account);
        void onAccountEnabled(String accountID, boolean enabled);
    }

    public class AccountView extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.account_alias)
        public TextView alias;
        @BindView(R.id.account_host)
        public TextView host;
        @BindView(R.id.loading_indicator)
        public View loadingIndicator;
        @BindView(R.id.error_indicator)
        public ImageView errorIndicator;
        @BindView(R.id.account_checked)
        public CheckBox enabled;

        public AccountView(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int data = getAdapterPosition();
            mListeners.onItemClicked(accounts.get(data));
        }

    }
}
