/*
 * Copyright (C) 2014 The Android Open Source Project
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

package cx.ring.tv.cards;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * This is a generic example of a custom data object, containing info we might want to keep with
 * each card on the home screen
 */
public class Card {

    private String mTitle = "";
    private String mDescription = "";
    private String mExtraText = "";
    private String mImageUrl;
    private String mFooterColor = null;
    private String mSelectedColor = null;
    private String mLocalImageResource = null;
    private String mFooterResource = null;
    private Card.Type mType;
    private long mId;
    private int mWidth;
    private int mHeight;

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getLocalImageResource() {
        return mLocalImageResource;
    }

    public void setLocalImageResource(String localImageResource) {
        mLocalImageResource = localImageResource;
    }

    public String getFooterResource() {
        return mFooterResource;
    }

    public void setFooterResource(String footerResource) {
        mFooterResource = footerResource;
    }

    public void setType(Type type) {
        mType = type;
    }

    public void setId(long id) {
        mId = id;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public long getId() {
        return mId;
    }

    public Card.Type getType() {
        return mType;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getExtraText() {
        return mExtraText;
    }

    public void setExtraText(String extraText) {
        mExtraText = extraText;
    }

    public int getFooterColor() {
        if (mFooterColor == null) return -1;
        return Color.parseColor(mFooterColor);
    }

    public void setFooterColor(String footerColor) {
        mFooterColor = footerColor;
    }

    public int getSelectedColor() {
        if (mSelectedColor == null) return -1;
        return Color.parseColor(mSelectedColor);
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setSelectedColor(String selectedColor) {
        mSelectedColor = selectedColor;
    }

    public void setImageUrl(String imageUrl) {
        mImageUrl = imageUrl;
    }

    public URI getImageURI() {
        if (getImageUrl() == null) return null;
        try {
            return new URI(getImageUrl());
        } catch (URISyntaxException e) {
            Log.d("URI exception: ", getImageUrl());
            return null;
        }
    }

    public int getLocalImageResourceId(Context context) {
        return context.getResources().getIdentifier(getLocalImageResourceName(), "drawable",
                                                    context.getPackageName());
    }

    public String getLocalImageResourceName() {
        return mLocalImageResource;
    }

    public String getFooterLocalImageResourceName() {
        return mFooterResource;
    }

    public enum Type {
        VERSION,
        DEFAULT,
        SEARCH_RESULT,
        CONTRIBUTOR,
        LICENCES,
    }

}