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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import cx.ring.R;
import cx.ring.adapters.AccountView;
import cx.ring.model.Account;

public class AccountsAdapter extends RecyclerView.Adapter<AccountView> {
    static final String TAG = AccountsAdapter.class.getSimpleName();
    private final ArrayList<Account> accounts = new ArrayList<>();
    private final AccountView.OnAccountActionListener mListeners;

    public AccountsAdapter(AccountView.OnAccountActionListener listeners) {
        mListeners = listeners;
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    @NonNull
    @Override
    public AccountView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View holderView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_account_pref, parent, false);
        return new AccountView(holderView, mListeners);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountView entryView, int position) {
        final Account item = accounts.get(position);
        entryView.update(item);
    }

    public void replaceAll(List<Account> results) {
        accounts.clear();
        accounts.addAll(results);
        notifyDataSetChanged();
    }
    public void replaceAccount(Account account) {
        for (int i=0; i<accounts.size(); i++) {
            if (accounts.get(i) == account) {
                notifyItemChanged(i);
                return;
            }
        }
    }

}
