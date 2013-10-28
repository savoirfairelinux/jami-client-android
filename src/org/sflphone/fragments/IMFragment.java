package org.sflphone.fragments;

import org.sflphone.R;
import org.sflphone.adapters.DiscussArrayAdapter;
import org.sflphone.model.Conference;
import org.sflphone.model.SipMessage;
import org.sflphone.service.ISipService;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class IMFragment extends Fragment {
    static final String TAG = CallListFragment.class.getSimpleName();

    private Callbacks mCallbacks = sDummyCallbacks;

    DiscussArrayAdapter mAdapter;
    ListView list;

    public static final int REQUEST_TRANSFER = 10;
    public static final int REQUEST_CONF = 20;

    @Override
    public void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);

        mAdapter = new DiscussArrayAdapter(getActivity());

    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public ISipService getService() {
            return null;
        }

    };

    /**
     * The Activity calling this fragment has to implement this interface
     * 
     */
    public interface Callbacks {
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
        mCallbacks = sDummyCallbacks;
    }

    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.frag_imessaging, container, false);

        list = (ListView) rootView.findViewById(R.id.message_list);
        list.setAdapter(mAdapter);

        return rootView;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);       
    }

    public void putMessage(SipMessage msg) {
        mAdapter.add(msg);
    }
}
