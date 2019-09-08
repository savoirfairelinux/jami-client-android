/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.model.Account;
import cx.ring.views.AvatarDrawable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class AccountView extends RecyclerView.ViewHolder {

    public interface OnAccountActionListener {
        void onAccountSelected(Account account);
        void onAccountEnabled(Account account);
    }

    private final OnAccountActionListener listener;

    @BindView(R.id.account_photo)
    ImageView photo;

    @BindView(R.id.account_alias)
    TextView alias;

    @BindView(R.id.account_host)
    TextView host;

    @BindView(R.id.error_indicator)
    ImageView error;

    @BindView(R.id.loading_indicator)
    ProgressBar loading;

    private TextView disabled_flag;
    private CheckBox enabled;

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public AccountView(View view, OnAccountActionListener l) {
        super(view);
        listener = l;
        ButterKnife.bind(this, view);
        disabled_flag = view.findViewById(R.id.account_disabled);
        enabled = view.findViewById(R.id.account_checked);
    }

    public void update(final Account account) {
        final Context context = itemView.getContext();
        mDisposable.clear();
        mDisposable.add(AvatarDrawable.load(context, account)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(avatar -> photo.setImageDrawable(avatar)));
        itemView.setOnClickListener(v -> listener.onAccountSelected(account));

        alias.setText(account.getAccountAlias());
        host.setText(account.getDisplayUri(context.getText(R.string.account_type_ip2ip)));
        itemView.setEnabled(account.isEnabled());

        if (disabled_flag != null) {
            disabled_flag.setVisibility(account.isEnabled() ? View.GONE : View.VISIBLE);
        }

        if (enabled != null) {
            enabled.setChecked(account.isEnabled());
            enabled.setOnClickListener(v -> {
                account.setEnabled(!account.isEnabled());
                listener.onAccountEnabled(account);
            });
        }

        if (account.isEnabled()) {
            alias.setTextColor(context.getResources().getColor(R.color.textColorPrimary));
            if (!account.isActive()) {
                error.setImageResource(R.drawable.baseline_sync_disabled_24);
                error.setColorFilter(Color.BLACK);
                error.setVisibility(View.VISIBLE);
                loading.setVisibility(View.GONE);
            } else if (account.isTrying()) {
                error.setVisibility(View.GONE);
                loading.setVisibility(View.VISIBLE);
            } else if (account.needsMigration()) {
                host.setText(R.string.account_update_needed);
                host.setTextColor(Color.RED);
                error.setImageResource(R.drawable.baseline_warning_24);
                error.setColorFilter(Color.RED);
                error.setVisibility(View.VISIBLE);
            } else if (account.isInError() || !account.isRegistered()) {
                error.setImageResource(R.drawable.baseline_error_24);
                error.setColorFilter(Color.RED);
                error.setVisibility(View.VISIBLE);
                loading.setVisibility(View.GONE);
            } else {
                error.setVisibility(View.GONE);
                loading.setVisibility(View.GONE);
            }
        } else {
            alias.setTextColor(context.getResources().getColor(R.color.textColorSecondary));
            error.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
        }
    }
}
