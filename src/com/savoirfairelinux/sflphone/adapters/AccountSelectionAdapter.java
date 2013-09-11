package com.savoirfairelinux.sflphone.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.model.Account;
import com.savoirfairelinux.sflphone.service.ISipService;

public class AccountSelectionAdapter extends BaseAdapter {

    private static final String TAG = AccountSelectionAdapter.class.getSimpleName();

    ArrayList<Account> accounts;
    Context mContext;
    int selectedAccount = -1;

    public AccountSelectionAdapter(Context cont, ArrayList<Account> newList) {
        super();
        accounts = newList;
        mContext = cont;
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
    public View getView(int pos, View convertView, ViewGroup parent) {
        View rowView = convertView;
        AccountView entryView = null;

        if (rowView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            rowView = inflater.inflate(R.layout.item_account, null);

            entryView = new AccountView();
            entryView.alias = (TextView) rowView.findViewById(R.id.account_alias);
            entryView.host = (TextView) rowView.findViewById(R.id.account_host);
            entryView.select = (RadioButton) rowView.findViewById(R.id.account_checked);
            rowView.setTag(entryView);
        } else {
            entryView = (AccountView) rowView.getTag();
        }

        entryView.alias.setText(accounts.get(pos).getAlias());
        entryView.host.setText(accounts.get(pos).getHost() + " - " + accounts.get(pos).getRegistered_state());
        // accManager.displayAccountDetails(accounts.get(pos), entryView);
        if (pos == selectedAccount) {
            entryView.select.setChecked(true);
        }

        return rowView;
    }

    /*********************
     * ViewHolder Pattern
     *********************/
    public class AccountView {
        public TextView alias;
        public TextView host;
        public RadioButton select;
    }

   
    public void setSelectedAccount(int pos) {
        selectedAccount = pos;
    }

    public Account getSelectedAccount() {
        if(selectedAccount == -1){
            return null;
        }
        return accounts.get(selectedAccount);
    }

    public void removeAll() {
        accounts.clear();
        notifyDataSetChanged();

    }

    public void addAll(ArrayList<Account> results) {
        accounts.addAll(results);
        notifyDataSetChanged();
    }

    /**
     * Modify state of specific account
     * @param accountState
     */
    public void updateAccount(Intent accountState) {
        Log.i(TAG,"updateAccount");
        String id = accountState.getStringExtra("Account");
        String newState = accountState.getStringExtra("state");
        accountState.getStringExtra("Account");
        
        for(Account a : accounts){
            if(a.getAccountID().contentEquals(id)){
                a.setRegistered_state(newState);
                notifyDataSetChanged();
                return;
            }
        }
        
    }

}
