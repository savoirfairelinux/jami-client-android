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
 *
 * Modified by: Loïc Siret <loic.siret@savoirfairelinux.com>
 *
 */
package cx.ring.tv.cards;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * This is a generic example of a custom data object, containing info we might want to keep with
 * each card on the home screen
 */
public class Card {

    private int mLocalImageResource = -1;
    private Drawable mDrawable = null;
    private String mTitle = "";
    private CharSequence mDescription = "";
    /*not used at the moment but will be use in futur*/
    private String mFooterColor = null;
    /*not used at the moment but will be use in futur*/
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

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public Card.Type getType() {
        return mType;
    }

    public void setType(Type type) {
        mType = type;
    }

    public CharSequence getDescription() {
        return mDescription;
    }

    public void setDescription(CharSequence description) {
        mDescription = description;
    }

    public void setLocalImageResource(@DrawableRes int localImageResource) {
        mLocalImageResource = localImageResource;
    }

    public void setDrawable(Drawable bitmapDrawable) {
        mDrawable = bitmapDrawable;
    }

    public Drawable getDrawable(@NonNull Context context) {
        if (mDrawable != null)
            return mDrawable;
        else if (mLocalImageResource != -1)
            return ContextCompat.getDrawable(context, mLocalImageResource);
        else
            return new ColorDrawable(0x00000000);
    }

    /*not used at the moment but will be use in futur*/
    public String getFooterResource() {
        return mFooterResource;
    }

    /*not used at the moment but will be use in futur*/
    public void setFooterResource(String footerResource) {
        mFooterResource = footerResource;
    }

    /*not used at the moment but will be use in futur*/
    public int getFooterColor() {
        if (mFooterColor == null) return -1;
        return Color.parseColor(mFooterColor);
    }

    /*not used at the moment but will be use in futur*/
    public void setFooterColor(String footerColor) {
        mFooterColor = footerColor;
    }

    /*not used at the moment but will be use in futur*/
    public String getFooterLocalImageResourceName() {
        return mFooterResource;
    }

    public enum Type {
        DEFAULT,
        SEARCH_RESULT,
        ACCOUNT_ADD_DEVICE,
        ACCOUNT_EDIT_PROFILE,
        ACCOUNT_SHARE_ACCOUNT,
        ADD_CONTACT,
        CONTACT,
        CONTACT_ONLINE,
        CONTACT_WITH_USERNAME,
        CONTACT_WITH_USERNAME_ONLINE,
        CONTACT_REQUEST,
        CONTACT_REQUEST_WITH_USERNAME
    }

}