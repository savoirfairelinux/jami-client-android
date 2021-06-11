/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import net.jami.facades.ConversationFacade;
import net.jami.services.ContactService;
import net.jami.smartlist.SmartListViewModel;

import javax.inject.Inject;
import javax.inject.Singleton;

import cx.ring.adapters.SmartListAdapter;
import cx.ring.application.JamiApplication;
import cx.ring.client.ConversationActivity;
import cx.ring.databinding.FragSharewithBinding;
import cx.ring.utils.ConversationPath;
import cx.ring.viewholders.SmartListViewHolder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ShareWithFragment extends Fragment {
    private final static String TAG = ShareWithFragment.class.getSimpleName();

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    @Inject
    @Singleton
    ConversationFacade mConversationFacade;

    @Inject
    @Singleton
    ContactService mContactService;

    private Intent mPendingIntent = null;
    private SmartListAdapter adapter;

    private FragSharewithBinding binding;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ShareWithFragment() {
        JamiApplication.getInstance().getInjectionComponent().inject(this);
    }

    public static ShareWithFragment newInstance() {
        return new ShareWithFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragSharewithBinding.inflate(inflater);

        Context context = binding.getRoot().getContext();
        Activity activity = getActivity();
        if (activity instanceof AppCompatActivity) {
            AppCompatActivity compatActivity = (AppCompatActivity) activity;
            compatActivity.setSupportActionBar(binding.toolbar);
            ActionBar ab = compatActivity.getSupportActionBar();
            if (ab != null)
                ab.setDisplayHomeAsUpEnabled(true);
        }

        if (mPendingIntent != null) {
            String type = mPendingIntent.getType();
            ClipData clip = mPendingIntent.getClipData();
            if (type.startsWith("text/")) {
                binding.previewText.setText(mPendingIntent.getStringExtra(Intent.EXTRA_TEXT));
                binding.previewText.setVisibility(View.VISIBLE);
            } else if (type.startsWith("image/")) {
                Uri data = mPendingIntent.getData();
                if (data == null && clip != null && clip.getItemCount() > 0)
                    data = clip.getItemAt(0).getUri();
                binding.previewImage.setImageURI(data);
                binding.previewImage.setVisibility(View.VISIBLE);
            } else if (type.startsWith("video/")) {
                Uri data = mPendingIntent.getData();
                if (data == null && clip != null && clip.getItemCount() > 0)
                    data = clip.getItemAt(0).getUri();
                try {
                    binding.previewVideo.setVideoURI(data);
                    binding.previewVideo.setVisibility(View.VISIBLE);
                } catch (NullPointerException | InflateException | NumberFormatException e) {
                    Log.e(TAG, e.getMessage());
                }
                binding.previewVideo.setOnCompletionListener(mediaPlayer -> binding.previewVideo.start());
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
                        intent.putExtra(Intent.EXTRA_TEXT, binding.previewText.getText().toString());
                    }
                    intent.putExtras(ConversationPath.toBundle(smartListViewModel.getAccountId(), smartListViewModel.getUri()));
                    intent.setClass(requireActivity(), ConversationActivity.class);
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(SmartListViewModel smartListViewModel) {

            }
        }, mDisposable);
        binding.shareList.setLayoutManager(new LinearLayoutManager(context));
        binding.shareList.setAdapter(adapter);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPendingIntent == null)
            getActivity().finish();
        mDisposable.add(mConversationFacade
                .getCurrentAccountSubject()
                .switchMap(a -> a.getConversationsViewModels(false))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    if (adapter != null)
                        adapter.update(list);
                }));
        if (binding != null && binding.previewVideo.getVisibility() != View.GONE) {
            binding.previewVideo.start();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mDisposable.clear();
    }

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        /*Intent intent = getActivity().getIntent();
        Bundle extra = intent.getExtras();
        if (ConversationPath.fromBundle(extra) != null) {
            intent.setClass(getActivity(), ConversationActivity.class);
            startActivity(intent);
            return;
        }*/
        mPendingIntent = getActivity().getIntent();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPendingIntent = null;
        adapter = null;
    }
}
