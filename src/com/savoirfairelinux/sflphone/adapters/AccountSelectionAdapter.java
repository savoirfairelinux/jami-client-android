package com.savoirfairelinux.sflphone.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.account.AccountDetailBasic;
import com.savoirfairelinux.sflphone.service.ISipService;

public class AccountSelectionAdapter extends BaseAdapter {

    private static final String TAG = AccountSelectionAdapter.class.getSimpleName();

    ArrayList<String> accountIDs;
    Context mContext;
    AccountManager accManager;
    ISipService service;
    int selectedAccount = 0;

    public AccountSelectionAdapter(Context cont, ISipService s, ArrayList<String> newList) {
        super();
        accountIDs = newList;
        mContext = cont;
        service = s;
        accManager = new AccountManager(mContext);
    }

    @Override
    public int getCount() {
        return accountIDs.size();
    }

    @Override
    public String getItem(int pos) {
        return accountIDs.get(pos);
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

        accManager.displayAccountDetails(accountIDs.get(pos), entryView);
        if(pos == selectedAccount){
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

    /**
     * Asynchronous account details retriever
     */
    public class AccountManager {

        // private static final String TAG = ImageManager.class.getSimpleName();

        private HashMap<String, HashMap<String, String>> accountMap = new HashMap<String, HashMap<String, String>>();
        private AccountQueue accountQueue = new AccountQueue();
        private Thread accountLoaderThread = new Thread(new AccountQueueManager());


        public AccountManager(Context context) {
            accountLoaderThread.setPriority(Thread.NORM_PRIORITY - 1);

        }

        public void displayAccountDetails(String id, AccountView account) {

            if (accountMap.containsKey(id)) {

                HashMap<String, String> details = accountMap.get(id);
                account.alias.setText(details.get(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS));
                account.host.setText(details.get(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME));

            } else {
                queueAccount(id, account);
            }
        }

        private void queueAccount(String id, AccountView row) {
            // This ImageView might have been used for other images, so we clear
            // the queue of old tasks before starting.
            accountQueue.Clean(row);
            AccountRef p = new AccountRef(id, row);

            synchronized (accountQueue.accountRefsStack) {
                accountQueue.accountRefsStack.push(p);
                accountQueue.accountRefsStack.notifyAll();
            }

            // Start thread if it's not started yet
            if (accountLoaderThread.getState() == Thread.State.NEW) {
                accountLoaderThread.start();
            }
        }

        /** Classes **/

        private class AccountRef {
            public String accountID;
            public AccountView row;

            public AccountRef(String u, AccountView i) {
                accountID = u;
                row = i;
            }
        }

        private class AccountQueue {
            private Stack<AccountRef> accountRefsStack = new Stack<AccountRef>();

            // removes all instances of this account
            public void Clean(AccountView view) {

                for (int i = 0; i < accountRefsStack.size();) {
                    if (accountRefsStack.get(i).row == view)
                        accountRefsStack.remove(i);
                    else
                        ++i;
                }
            }
        }

        private class AccountQueueManager implements Runnable {
            @Override
            public void run() {
                try {
                    while (true) {
                        // Thread waits until there are accountsID in the queue to be retrieved
                        if (accountQueue.accountRefsStack.size() == 0) {
                            synchronized (accountQueue.accountRefsStack) {
                                accountQueue.accountRefsStack.wait();
                            }
                        }

                        // When we have accounts to load
                        if (accountQueue.accountRefsStack.size() != 0) {
                            AccountRef accountToLoad;

                            synchronized (accountQueue.accountRefsStack) {
                                accountToLoad = accountQueue.accountRefsStack.pop();
                            }

                            HashMap<String, String> details = (HashMap<String, String>) service.getAccountDetails(accountToLoad.accountID);
                            accountMap.put(accountToLoad.accountID, details);
                            AccountDisplayer accDisplayer = new AccountDisplayer(details, accountToLoad.row);
                            Activity a = (Activity) mContext;

                            a.runOnUiThread(accDisplayer);

                        }

                        if (Thread.interrupted())
                            break;
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, e.toString());
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }

        // Used to display details in the UI thread
        private class AccountDisplayer implements Runnable {
            HashMap<String, String> displayDetails;
            AccountView displayRow;

            public AccountDisplayer(HashMap<String, String> details, AccountView row) {
                displayRow = row;
                displayDetails = details;
            }

            public void run() {
                displayRow.alias.setText(displayDetails.get(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS));
                displayRow.host.setText(displayDetails.get(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME));
            }
        }

        public void removeFromCache(Uri uri) {
            if (accountMap.containsKey(uri)) {
                accountMap.remove(uri);
            }
        }
    }

    public void setSelectedAccount(int pos) {
       selectedAccount = pos;
    }

}
