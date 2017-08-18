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

package cx.ring.tv.main;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import cx.ring.R;
import cx.ring.model.CallContact;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {
    private static final String TAG = CardPresenter.class.getSimpleName();

    private static int CARD_WIDTH = 313;
    private static int CARD_HEIGHT = 176;

    private static int SIMPLE_CARD_WIDTH = 200;
    private static int SIMPLE_CARD_HEIGHT = 150;

    static class ViewHolder extends Presenter.ViewHolder {
        private ImageCardView mCardView;
        private Drawable mDefaultCardImage;

        public ViewHolder(View view) {
            super(view);
            mCardView = (ImageCardView) view;
            mDefaultCardImage = getCardView().getResources().getDrawable(R.drawable.ic_contact_picture);
        }

        public ImageCardView getCardView() {
            return mCardView;
        }

        public Drawable getDefaultCardImage() {
            return mDefaultCardImage;
        }

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();
        ImageCardView cardView = new ImageCardView(context);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        if (item instanceof CallContact) {
            onBindViewHolderCallContact((ViewHolder) viewHolder, (CallContact)item);
        } else if (item instanceof SimpleCard) {
            onBindViewHolderSimpleCard((ViewHolder) viewHolder, (SimpleCard)item);
        }
    }

    private void onBindViewHolderSimpleCard(ViewHolder viewHolder, SimpleCard card) {
        Bitmap image = card.getImage();
        viewHolder.mCardView.setTitleText(card.getName());
        viewHolder.mCardView.setContentText(card.getDescription());
        viewHolder.mCardView.setMainImageDimensions(SIMPLE_CARD_WIDTH, SIMPLE_CARD_HEIGHT);
        viewHolder.mCardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE);
        viewHolder.mCardView.setMainImage(image != null
                ? new BitmapDrawable(viewHolder.mCardView.getResources(), image)
                : ContextCompat.getDrawable(viewHolder.mCardView.getContext(), card.getImageId()));
    }

    private void onBindViewHolderCallContact(ViewHolder viewHolder, CallContact contact) {
        viewHolder.mCardView.setTitleText(contact.getDisplayName());
        viewHolder.mCardView.setContentText(contact.getIds().get(0));
        viewHolder.mCardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        viewHolder.mCardView.setBackgroundColor(viewHolder.mCardView.getResources().getColor(R.color.color_primary_dark));
        if (contact.getPhoto() == null) {
            viewHolder.mCardView.setMainImage(viewHolder.getDefaultCardImage());
        }
        else {
            viewHolder.mCardView.setMainImage(
                    new BitmapDrawable(viewHolder.mCardView.getResources(), BitmapFactory.decodeByteArray(contact.getPhoto(), 0, contact.getPhoto().length)));
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        // Nothing to do
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
        // Nothing to do
    }

    static class SimpleCard {
        long id;
        int imageId;
        String name;
        String description;
        Bitmap image;

        Uri uri;

        SimpleCard(long id, String name, Bitmap image){
            this.id = id;
            this.name = name;
            this.image = image;
        }

        SimpleCard(long id, String name, int imageId){
            this.id = id;
            this.name = name;
            this.description = "";
            this.imageId = imageId;
        }

        SimpleCard(long id, String name, String description, int imageId){
            this.id = id;
            this.name = name;
            this.description = description;
            this.imageId = imageId;
        }

        SimpleCard(long id, String name, int imageId, Uri uri){
            this(id, name, imageId);
            this.uri = uri;
        }

        public Uri getUri() {
            return uri;
        }

        public void setUri(Uri uri) {
            this.uri = uri;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public int getImageId() {
            return imageId;
        }

        public void setImageId(int imageId) {
            this.image = null;
            this.imageId = imageId;
        }

        public Bitmap getImage() {
            return image;
        }

        public void setImage(Bitmap image) {
            this.image = image;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
