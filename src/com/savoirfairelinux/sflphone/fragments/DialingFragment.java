package com.savoirfairelinux.sflphone.fragments;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.account.AccountSelectionSpinner;
import com.savoirfairelinux.sflphone.client.receiver.CallListReceiver;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.views.ClearableEditText;

public class DialingFragment extends Fragment {

    private static final String TAG = HistoryFragment.class.getSimpleName();
    public static final String ARG_SECTION_NUMBER = "section_number";
    private boolean isReady;
    private ISipService service;

    ClearableEditText textField;
    private AccountSelectionSpinner mAccountSelectionSpinner;
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onCallDialed(String account, String to) {
        }

        @Override
        public ISipService getService() {
            // TODO Auto-generated method stub
            return null;
        }
    };

    /**
     * The Activity calling this fragment has to implement this interface
     * 
     */
    public interface Callbacks {
        void onCallDialed(String account, String to);

        public ISipService getService();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // mAdapter = new HistoryAdapter(getActivity(),new ArrayList<HashMap<String, String>>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_dialing, parent, false);

        mAccountSelectionSpinner = (AccountSelectionSpinner) inflatedView.findViewById(R.id.account_selection_button);

        textField = (ClearableEditText) inflatedView.findViewById(R.id.textField);
        ((ImageButton) inflatedView.findViewById(R.id.buttonCall)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                
                String accountID = mAccountSelectionSpinner.getAccount();
                String to = textField.getText().toString();
                mCallbacks.onCallDialed(accountID, to);
            }
        });

        ((Button) inflatedView.findViewById(R.id.alphabetic_keyboard)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                textField.setInputType(EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                InputMethodManager lManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE); 
                lManager.showSoftInput(textField.getEdit_text(), 0);
            }
        });

        ((Button) inflatedView.findViewById(R.id.numeric_keyboard)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                textField.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                InputMethodManager lManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE); 
                lManager.showSoftInput(textField.getEdit_text(), 0);
            }
        });

        isReady = true;
        if (mCallbacks.getService() != null) {

            onServiceSipBinded(mCallbacks.getService());
        }
        return inflatedView;
    }

    @Override
    public void onStart() {
        super.onStart();

    }


    public String getSelectedAccount() {
        return mAccountSelectionSpinner.getAccount();
    }

    /**
     * Called by activity to pass a reference to sipservice to Fragment.
     * 
     * @param isip
     */
    public void onServiceSipBinded(ISipService isip) {

        if (isReady) {
            service = isip;
            ArrayList<String> accountList;
            try {
                accountList = (ArrayList<String>) mCallbacks.getService().getAccountList();
                Log.w(TAG, "SIP service binded accounts " + accountList.size());
                mAccountSelectionSpinner.populate(mCallbacks.getService(), accountList);
            } catch (RemoteException e) {
                Log.i(TAG, e.toString());
            }
        }

    }

}
