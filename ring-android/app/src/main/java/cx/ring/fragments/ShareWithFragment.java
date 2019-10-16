/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.fragments;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import javax.inject.Inject;
import javax.inject.Singleton;

import cx.ring.R;
import cx.ring.adapters.SmartListAdapter;
import cx.ring.application.JamiApplication;
import cx.ring.client.ConversationActivity;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.services.ContactService;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.utils.ConversationPath;
import cx.ring.viewholders.SmartListViewHolder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class ShareWithFragment extends Fragment {
    private final static String TAG = ShareWithFragment.class.getSimpleName();

    private CompositeDisposable mDisposable = new CompositeDisposable();

    @Inject
    @Singleton
    ConversationFacade mConversationFacade;

    @Inject
    @Singleton
    ContactService mContactService;

    private Intent mPendingIntent = null;
    private SmartListAdapter adapter;

    private TextView previewText;
    private ImageView previewImage;
    private VideoView previewVideo;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ShareWithFragment() {
        JamiApplication.getInstance().getRingInjectionComponent().inject(this);
    }

    public static ShareWithFragment newInstance() {
        return new ShareWithFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_sharewith, container, false);
        RecyclerView list = view.findViewById(R.id.shareList);
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        previewText = view.findViewById(R.id.previewText);
        previewImage = view.findViewById(R.id.previewImage);
        previewVideo = view.findViewById(R.id.previewVideo);

        Context context = view.getContext();
        Activity activity = getActivity();
        if (activity instanceof AppCompatActivity) {
            AppCompatActivity compatActivity = (AppCompatActivity) activity;
            compatActivity.setSupportActionBar(toolbar);
            ActionBar ab = compatActivity.getSupportActionBar();
            if (ab != null)
                ab.setDisplayHomeAsUpEnabled(true);
        }

        if (mPendingIntent != null) {
            String type = mPendingIntent.getType();
            ClipData clip = mPendingIntent.getClipData();
            if (type.startsWith("text/")) {
                previewText.setText(mPendingIntent.getStringExtra(Intent.EXTRA_TEXT));
                previewText.setVisibility(View.VISIBLE);
            } else if (type.startsWith("image/")) {
                Uri data = mPendingIntent.getData();
                if (data == null && clip != null && clip.getItemCount() > 0)
                    data = clip.getItemAt(0).getUri();
                previewImage.setImageURI(data);
                previewImage.setVisibility(View.VISIBLE);
            } else if (type.startsWith("video/")) {
                Uri data = mPendingIntent.getData();
                if (data == null && clip != null && clip.getItemCount() > 0)
                    data = clip.getItemAt(0).getUri();
                try {
                    previewVideo.setVideoURI(data);
                    previewVideo.setVisibility(View.VISIBLE);
                } catch (NullPointerException | InflateException | NumberFormatException e) {
                    Log.e(TAG, e.getMessage());
                }
                previewVideo.setOnCompletionListener(mediaPlayer -> previewVideo.start());
            }
        }

        adapter = new SmartListAdapter(null, new SmartListViewHolder.SmartListListeners() {
            @Override
            public void onItemClick(SmartListViewModel smartListViewModel) {
                if (mPendingIntent != null) {
                    Intent intent = mPendingIntent;
                    mPendingIntent = null;
                    String type = intent.getType();
                    if (type != null && type.startsWith("text/")) {
                        intent.putExtra(Intent.EXTRA_TEXT, previewText.getText().toString());
                    }
                    intent.putExtra(ConversationFragment.KEY_ACCOUNT_ID, smartListViewModel.getAccountId());
                    intent.putExtra(ConversationFragment.KEY_CONTACT_RING_ID, smartListViewModel.getContact().getPrimaryNumber());
                    intent.setClass(requireActivity(), ConversationActivity.class);
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(SmartListViewModel smartListViewModel) {

            }
        });
        list.setLayoutManager(new LinearLayoutManager(context));
        list.setAdapter(adapter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPendingIntent == null)
            getActivity().finish();
        mDisposable.add(mConversationFacade
                .getCurrentAccountSubject()
                .switchMap(Account::getConversationsViewModels)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    if (adapter != null)
                        adapter.update(list);
                }));
        if (previewVideo != null && previewVideo.getVisibility() != View.GONE) {
            previewVideo.start();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mDisposable.clear();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getActivity().getIntent();
        Bundle extra = intent.getExtras();
        if (extra != null) {
            if (ConversationPath.fromBundle(extra) != null) {
                intent.setClass(getActivity(), ConversationActivity.class);
                startActivity(intent);
                return;
            }
        }
        mPendingIntent = intent;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPendingIntent = null;
        adapter = null;
    }
}
