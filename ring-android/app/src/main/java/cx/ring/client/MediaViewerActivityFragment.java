package cx.ring.client;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.load.resource.bitmap.CenterInside;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.adapters.ConversationAdapter;
import cx.ring.utils.GlideApp;
import cx.ring.utils.GlideOptions;

/**
 * A placeholder fragment containing a simple view.
 */
public class MediaViewerActivityFragment extends Fragment {
    private final static String TAG = MediaViewerActivityFragment.class.getSimpleName();

    private Uri mUri = null;

    protected ImageView mImage;

    private final GlideOptions PICTURE_OPTIONS = new GlideOptions().transform(new CenterInside());

    public MediaViewerActivityFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_media_viewer, container, false);
        mImage = view.findViewById(R.id.image);
        showImage();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Activity a = getActivity();
        if (a == null)
            return;
        Intent i = a.getIntent();
        Uri data = i.getData();
        Log.w(TAG, "onStart(): " + i + " " + data);

        mUri = data;
        showImage();
    }

    private void showImage() {
        if (mUri == null) {
            Log.w(TAG, "showImage(): null URI");
            return;
        }
        Activity a = getActivity();
        if (a == null) {
            Log.w(TAG, "showImage(): null Activity");
            return;
        }
        if (mImage == null) {
            Log.w(TAG, "showImage(): null image view");
            return;
        }
        GlideApp.with(a)
                .load(mUri)
                .apply(PICTURE_OPTIONS)
                .into(mImage);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
}
