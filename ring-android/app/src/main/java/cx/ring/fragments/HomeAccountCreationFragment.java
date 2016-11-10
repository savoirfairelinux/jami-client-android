package cx.ring.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.client.AccountWizard;

/**
 * Created by abonnet on 16-11-11.
 */

public class HomeAccountCreationFragment extends Fragment {

    @BindView(R.id.ring_add_account)
    Button mLinkButton;

    @BindView(R.id.ring_create_btn)
    Button mCreateButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.frag_acc_home_create, parent, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @OnClick(R.id.ring_add_account)
    public void linkAccountClicked(){
        AccountWizard accountWizard = (AccountWizard) getActivity();
        accountWizard.newAccount(true);
    }

    @OnClick(R.id.ring_create_btn)
    public void createAccountClicked(){
        AccountWizard accountWizard = (AccountWizard) getActivity();
        accountWizard.newAccount(false);
    }
}
