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

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.model.Account;
import cx.ring.utils.CircleTransform;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.property.FormattedName;

class AccountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Account> mDataset = new ArrayList<>();

    private static final int TYPE_ACCOUNT = 0;
    private static final int TYPE_ADD_RING_ACCOUNT = 1;
    private static final int TYPE_ADD_SIP_ACCOUNT = 2;
    private OnAccountActionClicked mListener;

    public List<Account> getAccounts() {
        return mDataset;
    }

    interface OnAccountActionClicked {
        void onAccountSelected(Account account);

        void onAddSIPAccountSelected();
        void onAddRINGAccountSelected();
    }

    AccountAdapter() {}

    void setOnAccountActionClickedListener(OnAccountActionClicked listener) {
        mListener = listener;
    }

    public void replaceAll(List<Account> results) {
        setAccounts(results);
        notifyDataSetChanged();
    }

    private void setAccounts(List<Account> results) {
        mDataset.clear();
        for (Account account : results) {
            mDataset.add(account);
        }
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
            VCard vcard = VCardUtils.loadLocalProfileFromDisk(context.getFilesDir(), account.getAccountID());
            if (!vcard.getPhotos().isEmpty()) {
                Glide.with(context)
                        .load(vcard.getPhotos().get(0).getData())
                        .placeholder(R.drawable.ic_contact_picture)
                        .crossFade()
                        .transform(new CircleTransform(context))
                        .error(R.drawable.ic_contact_picture)
                        .into(photo);
            } else {
                Glide.with(context)
                        .load(R.drawable.ic_contact_picture)
                        .into(photo);
            }
            String alias = account.getAlias();
            FormattedName name = vcard.getFormattedName();
            if (name != null) {
                String name_value = name.getValue();
                if (!TextUtils.isEmpty(name_value)) {
                    alias = name_value;
                }
            }
            this.alias.setText(alias);
            if (account.isRing()) {
                String username = account.getRegisteredName();
                if (!account.registeringUsername && !TextUtils.isEmpty(username)) {
                    host.setText(username);
                } else {
                    host.setText(account.getUsername());
                }
            } else if (account.isSip() && !account.isIP2IP()) {
                host.setText(account.getUsername() + "@" + account.getHost());
            } else {
                host.setText(R.string.account_type_ip2ip);
            }

            itemView.setEnabled(account.isEnabled());
            disabled_flag.setVisibility(account.isEnabled() ? View.GONE : View.VISIBLE);
            if (account.isEnabled()) {
                this.alias.setTextColor(context.getResources().getColor(R.color.text_color_primary));
                if (account.isTrying()) {
                    error.setVisibility(View.GONE);
                    loading.setVisibility(View.VISIBLE);
                } else if (account.needsMigration()) {
                    host.setText(R.string.account_update_needed);
                    host.setTextColor(Color.RED);
                    error.setImageResource(R.drawable.ic_warning);
                    error.setColorFilter(Color.RED);
                    error.setVisibility(View.VISIBLE);
                } else if (account.isInError()) {
                    error.setImageResource(R.drawable.ic_error_white);
                    error.setColorFilter(Color.RED);
                    error.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.GONE);
                } else if (!account.isRegistered()) {
                    error.setImageResource(R.drawable.ic_network_disconnect_black_24dp);
                    error.setColorFilter(Color.BLACK);
                    error.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.GONE);
                } else {
                    error.setVisibility(View.GONE);
                    loading.setVisibility(View.GONE);
                }
            } else {
                this.alias.setTextColor(context.getResources().getColor(R.color.text_color_secondary));
                error.setVisibility(View.GONE);
                loading.setVisibility(View.GONE);
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

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {
        RecyclerView.ViewHolder viewHolder;
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
            default:
                return null;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        switch (getItemViewType(position)) {
            case TYPE_ACCOUNT: {
                ((AccountView) holder).update(mDataset.get(position));
                break;
            }
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