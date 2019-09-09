/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.navigation;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.adapters.AccountView;
import cx.ring.model.Account;

public class AccountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_ACCOUNT = 0;
    private static final int TYPE_ADD_RING_ACCOUNT = 1;
    private static final int TYPE_ADD_SIP_ACCOUNT = 2;
    private final List<Account> mDataset = new ArrayList<>();
    private OnAccountActionClicked mListener;

    AccountAdapter() {}

    void setOnAccountActionClickedListener(OnAccountActionClicked listener) {
        mListener = listener;
    }

    void replaceAll(List<Account> results) {
        setAccounts(results);
        notifyDataSetChanged();
    }

    private void setAccounts(List<Account> results) {
        mDataset.clear();
        mDataset.addAll(results);
    }

    public void replace(Account account) {
        for (int i = 0; i < mDataset.size(); i++) {
            Account a = mDataset.get(i);
            if (a == account) {
                notifyItemChanged(i);
                break;
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

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        View holderView;
        switch (viewType) {
            case TYPE_ACCOUNT:
                holderView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_account, parent, false);
                viewHolder = new AccountView(holderView, new AccountView.OnAccountActionListener() {
                    @Override
                    public void onAccountSelected(Account account) {
                        mListener.onAccountSelected(account);
                    }

                    @Override
                    public void onAccountEnabled(Account account) {}
                });
                break;
            case TYPE_ADD_SIP_ACCOUNT:
            case TYPE_ADD_RING_ACCOUNT:
                holderView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_menu, parent, false);
                viewHolder = new AddAccountView(holderView, viewType);
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case TYPE_ACCOUNT: {
                ((AccountView) holder).update(mDataset.get(position));
                break;
            }
            case TYPE_ADD_SIP_ACCOUNT:
                ((AddAccountView) holder).icon.setImageResource(R.drawable.baseline_add_24);
                ((AddAccountView) holder).title.setText(R.string.add_sip_account_title);
                break;
            case TYPE_ADD_RING_ACCOUNT:
                ((AddAccountView) holder).icon.setImageResource(R.drawable.baseline_add_24);
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

    interface OnAccountActionClicked {
        void onAccountSelected(Account account);

        void onAddSIPAccountSelected();

        void onAddRINGAccountSelected();
    }

    class AddAccountView extends RecyclerView.ViewHolder implements View.OnClickListener {
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
}