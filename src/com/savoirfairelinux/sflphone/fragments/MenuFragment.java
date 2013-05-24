package com.savoirfairelinux.sflphone.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.MenuAdapter;

public class MenuFragment extends Fragment{
    
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
        
         mAdapter = new MenuAdapter(getActivity(),getResources().getStringArray(R.array.menu_categories));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_menu, parent, false);

        ((ListView) inflatedView.findViewById(R.id.listView)).setAdapter(mAdapter);
        return inflatedView;
    }

    @Override
    public void onStart() {
        super.onStart();

    }


}
