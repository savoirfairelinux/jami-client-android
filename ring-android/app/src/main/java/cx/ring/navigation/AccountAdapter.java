/**
 * Copyright (C) 2016 by Savoir-faire Linux
 * Author : Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.navigation;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.model.Account;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountView> {
    private List<Account> mDataset;

    public class AccountView extends RecyclerView.ViewHolder {

        @BindView(R.id.account_alias)
        TextView alias;

        @BindView(R.id.account_host)
        TextView host;

        @BindView(R.id.error_indicator)
        ImageView error;

        public AccountView(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public AccountAdapter(List<Account> accounts) {
        mDataset = accounts;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public AccountAdapter.AccountView onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false);
        // set the view's size, margins, paddings and layout parameters

        AccountView vh = new AccountView(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(AccountView holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.alias.setText(mDataset.get(position).getAlias());

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}