package com.savoirfairelinux.sflphone.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.MenuAdapter;
import com.savoirfairelinux.sflphone.client.SFLPhoneHomeActivity;
import com.savoirfairelinux.sflphone.client.SFLPhonePreferenceActivity;

public class MenuFragment extends Fragment {

    private static final String TAG = MenuFragment.class.getSimpleName();
    public static final String ARG_SECTION_NUMBER = "section_number";

    MenuAdapter mAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new MenuAdapter(getActivity());

        String[] categories = getResources().getStringArray(R.array.menu_categories);
        ArrayAdapter<String> paramAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_menu, getResources().getStringArray(
                R.array.menu_items_param));
        ArrayAdapter<String> helpAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_menu, getResources().getStringArray(
                R.array.menu_items_help));

        // Add Sections

        mAdapter.addSection(categories[0], paramAdapter);
        mAdapter.addSection(categories[1], helpAdapter);

    }

    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_menu, parent, false);

        ((ListView) inflatedView.findViewById(R.id.listView)).setAdapter(mAdapter);
        ((ListView) inflatedView.findViewById(R.id.listView)).setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
               if(pos == 1 || pos == 2 || pos == 3){
                   Intent launchPreferencesIntent = new Intent().setClass(getActivity(), SFLPhonePreferenceActivity.class);
                   startActivityForResult(launchPreferencesIntent, SFLPhoneHomeActivity.REQUEST_CODE_PREFERENCES);
               }
                
            }
        });
        return inflatedView;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

}
