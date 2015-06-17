package cx.ring.fragments;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import cx.ring.R;
import cx.ring.client.HomeActivity;

public class AboutFragment extends Fragment {

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity)getActivity()).setToolbarState(false, R.string.menu_item_about);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_about, parent, false);

        TextView title = (TextView) inflatedView.findViewById(R.id.app_name);
        try {
            String versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            title.setText(getString(R.string.app_name) + " - " + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView link = (TextView) inflatedView.findViewById(R.id.web_site);
        String linkText = "<a href='http://ring.cx'>" + getResources().getString(R.string.web_site) + "</a>";
        link.setText(Html.fromHtml(linkText));
        link.setMovementMethod(LinkMovementMethod.getInstance());

        return inflatedView;
    }

}
