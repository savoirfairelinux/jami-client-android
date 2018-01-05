/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package cx.ring.tv.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.TitleViewAdapter;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import cx.ring.R;

/**
 * Custom title view to be used in {@link android.support.v17.leanback.app.BrowseFragment}.
 */
public class CustomTitleView extends RelativeLayout implements TitleViewAdapter.Provider {
    private final TextView mAliasView;
    private final TextView mTitleView;
    private final ImageView mLogoView;
    private final View mSearchOrbView;

    private final TitleViewAdapter mTitleViewAdapter = new TitleViewAdapter() {
        @Override
        public View getSearchAffordanceView() {
            return mSearchOrbView;
        }

        @Override
        public void setTitle(CharSequence titleText) {
            CustomTitleView.this.setTitle(titleText);
        }

        @Override
        public void setBadgeDrawable(Drawable drawable) {
            //NOOP
        }

        @Override
        public void setOnSearchClickedListener(OnClickListener listener) {
            mSearchOrbView.setOnClickListener(listener);
        }

        @Override
        public void updateComponentsVisibility(int flags) {
            int visibility = (flags & SEARCH_VIEW_VISIBLE) == SEARCH_VIEW_VISIBLE
                    ? View.VISIBLE : View.INVISIBLE;
            mSearchOrbView.setVisibility(visibility);
        }

    };

    public CustomTitleView(Context context) {
        this(context, null);
    }

    public CustomTitleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomTitleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        View root = LayoutInflater.from(context).inflate(R.layout.tv_titleview, this);
        mAliasView = root.findViewById(R.id.account_alias);
        mTitleView = root.findViewById(R.id.title_text);
        mLogoView = root.findViewById(R.id.title_photo_contact);
        mSearchOrbView = root.findViewById(R.id.title_orb);

        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setTitle(CharSequence title) {
        if (title != null) {
            mTitleView.setText(title);
            mTitleView.setVisibility(View.VISIBLE);
            mLogoView.setVisibility(View.VISIBLE);
        }
    }

    public void setAlias(CharSequence alias) {
        if (alias != null) {
            mAliasView.setText(alias);
            mAliasView.setVisibility(VISIBLE);
        }
    }

    public void setCurrentAccountPhoto(byte[] photo) {
        if (photo != null && photo.length > 0) {
            Glide.with(getContext())
                    .load(photo)
                    .error(R.drawable.ic_contact_picture)
                    .into(mLogoView);
        } else {
            Glide.with(getContext())
                    .load(R.drawable.ic_contact_picture)
                    .into(mLogoView);
        }
        mLogoView.setVisibility(View.VISIBLE);
    }

    @Override
    public TitleViewAdapter getTitleViewAdapter() {
        return mTitleViewAdapter;
    }
}