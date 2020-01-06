/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.model.Account;
import cx.ring.services.AccountService;
import cx.ring.views.AvatarDrawable;
import ezvcard.VCard;
import ezvcard.property.FormattedName;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ToolbarSpinnerAdapter extends ArrayAdapter<Account> {

    public static final int TYPE_ACCOUNT = 0;
    public static final int TYPE_CREATE_JAMI = 1;
    public static final int TYPE_CREATE_SIP = 2;

    private static final String TAG = ToolbarSpinnerAdapter.class.getSimpleName();

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    @Inject
    AccountService mAccountService;

    private Context mContext;
    private List<Account> mAccounts;

    public ToolbarSpinnerAdapter(@NonNull Context context, int resource, List<Account> accounts){
        super(context, resource, accounts);

        mContext = context;
        mAccounts = accounts;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return selectedView(convertView, position);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return dropDownView(convertView, position);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mAccounts.size()) {
            return TYPE_CREATE_JAMI;
        }
        if (position == mAccounts.size() + 1) {
            return TYPE_CREATE_SIP;
        }

        return TYPE_ACCOUNT;
    }

    @Override
    public int getCount() {
        return mAccounts.size() + 2;
    }

    private View selectedView(View convertView , int position){

        int type = getItemViewType(position);

        ViewHolder holder ;
        View rowView = convertView;
        if (rowView == null) {

            holder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.item_toolbar_spinner, null, false);

            holder.title = rowView.findViewById(R.id.title);
            holder.subTitle = rowView.findViewById(R.id.subtitle);
            holder.logo = rowView.findViewById(R.id.logo);
            holder.logo.setVisibility(View.GONE);
            holder.subTitle.setVisibility(View.GONE);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.title.getLayoutParams();
            params.leftMargin = 0;
            holder.title.setLayoutParams(params);

            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        if (type == TYPE_ACCOUNT) {
            Account account = getItem(position);

            holder.title.setText(getAccountAlias(account));
        }

        return rowView;
    }

    private View dropDownView(View convertView , int position){

        int type = getItemViewType(position);

        ViewHolder holder ;
        View rowView = convertView;
        if (rowView == null) {

            holder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.item_toolbar_spinner, null, false);

            holder.title = rowView.findViewById(R.id.title);
            holder.subTitle = rowView.findViewById(R.id.subtitle);
            holder.logo = rowView.findViewById(R.id.logo);
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        if (type == TYPE_ACCOUNT) {
            Account account = getItem(position);

            holder.title.setText(getAccountAlias(account));
            holder.subTitle.setText(getUri(account, mContext.getString(R.string.account_type_ip2ip)));

            mDisposable.add(AvatarDrawable.load(mContext, account)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(avatar -> holder.logo.setImageDrawable(avatar), e -> Log.e(TAG, "Error loading avatar", e)));
        } else {
            if (type == TYPE_CREATE_JAMI)
                holder.title.setText(R.string.add_ring_account_title);
            else
                holder.title.setText(R.string.add_sip_account_title);
            holder.subTitle.setVisibility(View.GONE);
            holder.logo.setImageResource(R.drawable.baseline_add_24);

            ViewGroup.LayoutParams logoParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            holder.logo.setLayoutParams(logoParam);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.title.getLayoutParams();
            params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            holder.title.setLayoutParams(params);
        }

        return rowView;
    }

    private static class ViewHolder {
        TextView title;
        TextView subTitle;
        ImageView logo;
    }

    private String getAccountAlias(Account account) {
        if (account == null) {
            cx.ring.utils.Log.e(TAG, "Not able to get account alias");
            return null;
        }
        String alias = getAlias(account);
        return (alias == null) ? account.getAlias() : alias;
    }

    private String getAlias(Account account) {
        if (account == null) {
            cx.ring.utils.Log.e(TAG, "Not able to get alias");
            return null;
        }
        VCard vcard = account.getProfile();
        if (vcard != null) {
            FormattedName name = vcard.getFormattedName();
            if (name != null) {
                String name_value = name.getValue();
                if (name_value != null && !name_value.isEmpty()) {
                    return name_value;
                }
            }
        }
        return null;
    }

    private String getUri(Account account, CharSequence defaultNameSip) {
        if (account.isIP2IP()) {
            return defaultNameSip.toString();
        }
        return account.getDisplayUri();
    }

}
