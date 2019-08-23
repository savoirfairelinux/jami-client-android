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

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.model.Account;
import cx.ring.views.AvatarDrawable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

class AccountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_ACCOUNT = 0;
    private static final int TYPE_ADD_RING_ACCOUNT = 1;
    private static final int TYPE_ADD_SIP_ACCOUNT = 2;
    private final List<Account> mDataset = new ArrayList<>();
    private final RingNavigationPresenter mRingNavigationPresenter;
    private OnAccountActionClicked mListener;

    AccountAdapter(RingNavigationPresenter presenter) {
        mRingNavigationPresenter = presenter;
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
                viewHolder = new AccountView(holderView);
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

    class AccountView extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.account_photo)
        ImageView photo;

        @BindView(R.id.account_alias)
        TextView alias;

        @BindView(R.id.account_disabled)
        TextView disabled_flag;

        @BindView(R.id.account_host)
        TextView host;

        @BindView(R.id.error_indicator)
        ImageView error;

        @BindView(R.id.loading_indicator)
        ProgressBar loading;

        private final CompositeDisposable mDisposable = new CompositeDisposable();

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

        public void update(final Account account) {
            final Context context = itemView.getContext();
            mDisposable.clear();
            mDisposable.add(AvatarDrawable.load(context, account)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(avatar -> photo.setImageDrawable(avatar)));

            alias.setText(mRingNavigationPresenter.getAccountAlias(account));
            host.setText(mRingNavigationPresenter.getUri(account, context.getText(R.string.account_type_ip2ip)));
            itemView.setEnabled(account.isEnabled());
            disabled_flag.setVisibility(account.isEnabled() ? View.GONE : View.VISIBLE);
            if (account.isEnabled()) {
                alias.setTextColor(context.getResources().getColor(R.color.text_color_primary));
                if (!account.isActive()) {
                    error.setImageResource(R.drawable.baseline_sync_disabled_24px);
                    error.setColorFilter(Color.BLACK);
                    error.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.GONE);
                } else if (account.isTrying()) {
                    error.setVisibility(View.GONE);
                    loading.setVisibility(View.VISIBLE);
                } else if (account.needsMigration()) {
                    host.setText(R.string.account_update_needed);
                    host.setTextColor(Color.RED);
                    error.setImageResource(R.drawable.ic_warning);
                    error.setColorFilter(Color.RED);
                    error.setVisibility(View.VISIBLE);
                } else if (account.isInError() || !account.isRegistered()) {
                    error.setImageResource(R.drawable.ic_error_white);
                    error.setColorFilter(Color.RED);
                    error.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.GONE);
                } else {
                    error.setVisibility(View.GONE);
                    loading.setVisibility(View.GONE);
                }
            } else {
                alias.setTextColor(context.getResources().getColor(R.color.text_color_secondary));
                error.setVisibility(View.GONE);
                loading.setVisibility(View.GONE);
            }
        }
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