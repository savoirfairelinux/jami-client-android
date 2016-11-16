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

class AccountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Account> mDataset;

    private static final int TYPE_ACCOUNT = 0;
    private static final int TYPE_ADD_RING_ACCOUNT = 1;
    private static final int TYPE_ADD_SIP_ACCOUNT = 2;
    private OnAccountActionClicked mListener;

    interface OnAccountActionClicked {
        void onAccountSelected(Account account);

        void onAddSIPAccountSelected();
        void onAddRINGAccountSelected();
    }

    AccountAdapter(List<Account> accounts) {
        mDataset = accounts;
    }

    void setOnAccountActionClickedListener(OnAccountActionClicked listener) {
        mListener = listener;
    }

    public void replaceAll(List<Account> results) {
        setAccounts(results);
        notifyDataSetChanged();
    }

    private void setAccounts(List<Account> results) {
        mDataset.clear();
        for (Account acc : results) {
                mDataset.add(acc);
        }
    }

    class AccountView extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.account_alias)
        TextView alias;

        @BindView(R.id.account_host)
        TextView host;

        @BindView(R.id.error_indicator)
        ImageView error;

        AccountView(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }
        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onAccountSelected(mDataset.get(getAdapterPosition()));
            }
        }

    }

    @Override
    public int getItemViewType(int position) {
        if (position == mDataset.size()) {
            return TYPE_ADD_RING_ACCOUNT;
        }
        if (position == mDataset.size() + 1) {
            return TYPE_ADD_SIP_ACCOUNT;
        }
        return TYPE_ACCOUNT;
    }

    public class AddAccountView extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final int viewtype;

        @BindView(R.id.navigation_item_title)
        TextView title;

        @BindView(R.id.navigation_item_icon)
        ImageView icon;

        AddAccountView(View view, int type) {
            super(view);
            viewtype = type;
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }
        @Override
        public void onClick(View v) {
            if (mListener == null) {
                return;
            }
            if (viewtype == TYPE_ADD_RING_ACCOUNT) {
                mListener.onAddRINGAccountSelected();
            } else if (viewtype == TYPE_ADD_SIP_ACCOUNT) {
                mListener.onAddSIPAccountSelected();
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {
        RecyclerView.ViewHolder vh;
        View v;
        switch (viewType) {
            case TYPE_ACCOUNT:
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_account, parent, false);
                vh = new AccountView(v);

                break;
            case TYPE_ADD_SIP_ACCOUNT:
            case TYPE_ADD_RING_ACCOUNT:
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_menu, parent, false);
                vh = new AddAccountView(v, viewType);
                break;
            default:
                return null;
        }

        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        switch (getItemViewType(position)) {
            case TYPE_ACCOUNT:
                Account acc = mDataset.get(position);
                ((AccountView) holder).alias.setText(acc.getAlias());
                if (acc.isRing()) {
                    ((AccountView) holder).host.setText(acc.getUsername());
                } else if (acc.isSip() && !acc.isIP2IP()) {
                    ((AccountView) holder).host.setText(acc.getUsername() + "@" + acc.getHost());
                } else {
                    ((AccountView) holder).host.setText(R.string.account_type_ip2ip);
                }

                ((AccountView) holder).error.setVisibility(acc.isRegistered() ? View.GONE : View.VISIBLE);
                break;
            case TYPE_ADD_SIP_ACCOUNT:
                ((AddAccountView) holder).icon.setImageResource(R.drawable.ic_add_black_24dp);
                ((AddAccountView) holder).title.setText(R.string.add_sip_account_title);
                break;
            case TYPE_ADD_RING_ACCOUNT:
                ((AddAccountView) holder).icon.setImageResource(R.drawable.ic_add_black_24dp);
                ((AddAccountView) holder).title.setText(R.string.add_ring_account_title);
                break;
            default:
                break;
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        // Add two entries for account creation
        return mDataset.size() + 2;
    }
}