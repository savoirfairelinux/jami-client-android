package cx.ring.tv.cards;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.leanback.widget.BaseCardView;

import androidx.leanback.R;

public class CardView extends BaseCardView {

    public static final int CARD_TYPE_FLAG_IMAGE_ONLY = 0;
    public static final int CARD_TYPE_FLAG_TITLE = 1;
    public static final int CARD_TYPE_FLAG_CONTENT = 2;
    public static final int CARD_TYPE_FLAG_ICON_RIGHT = 3;
    
    private static final int CARD_HEIGHT = 290;
    private static final String ALPHA = "alpha";

    private ImageView mImageView;
    private ViewGroup mInfoArea;
    private TextView mTitleView;
    private TextView mContentView;
    private ImageView mBadgeImage;
    private boolean mAttachedToWindow;
    private ObjectAnimator mFadeInAnimator;

    @Deprecated
    public CardView(Context context, int themeResId) {
        this(new ContextThemeWrapper(context, themeResId));
    }

    public CardView(Context context) {
        this(context, null);
    }

    public CardView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.imageCardViewStyle);
    }

    public CardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        buildImageCardView(attrs, defStyleAttr, R.style.Widget_Leanback_ImageCardView);
    }

    @SuppressLint("CustomViewStyleable")
    private void buildImageCardView(AttributeSet attrs, int defStyleAttr, int defStyle) {
        // Make sure the ImageCardView is focusable.
        setFocusable(true);
        setFocusableInTouchMode(true);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, CARD_HEIGHT));

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.lb_image_card_view, this);
        TypedArray cardAttrs = getContext().obtainStyledAttributes(attrs, R.styleable.lbImageCardView, defStyleAttr, defStyle);
        ViewCompat.saveAttributeDataForStyleable(this, getContext(), R.styleable.lbImageCardView, attrs, cardAttrs, defStyleAttr, defStyle);
        int cardType = cardAttrs.getInt(R.styleable.lbImageCardView_lbImageCardViewType, CARD_TYPE_FLAG_IMAGE_ONLY);

        boolean hasImageOnly = cardType == CARD_TYPE_FLAG_IMAGE_ONLY;
        boolean hasTitle = (cardType & CARD_TYPE_FLAG_TITLE) == CARD_TYPE_FLAG_TITLE;
        boolean hasContent = (cardType & CARD_TYPE_FLAG_CONTENT) == CARD_TYPE_FLAG_CONTENT;
        boolean hasIconRight = (cardType & CARD_TYPE_FLAG_ICON_RIGHT) == CARD_TYPE_FLAG_ICON_RIGHT;

        mImageView = findViewById(R.id.main_image);
        if (mImageView.getDrawable() == null) {
            mImageView.setVisibility(View.INVISIBLE);
        }

        // Set Object Animator for image view.
        mFadeInAnimator = ObjectAnimator.ofFloat(mImageView, ALPHA, 1f);
        mFadeInAnimator.setDuration(mImageView.getResources().getInteger(android.R.integer.config_shortAnimTime));

        mInfoArea = findViewById(R.id.info_field);

        Typeface mulishBold = ResourcesCompat.getFont(getContext(), cx.ring.R.font.mulish_semibold);
        Typeface mulishRegular = ResourcesCompat.getFont(getContext(), cx.ring.R.font.mulish_regular);

        if (hasImageOnly) {
            removeView(mInfoArea);
            cardAttrs.recycle();
            return;
        }

        if (hasTitle) {
            mTitleView = (TextView) inflater.inflate(R.layout.lb_image_card_view_themed_title, mInfoArea, false);
            mTitleView.setTextSize(12);
            mTitleView.setTypeface(mulishBold);
            mTitleView.setMaxLines(2);
            mTitleView.setEllipsize(TextUtils.TruncateAt.END);
            mInfoArea.addView(mTitleView);
        }

        if (hasContent) {
            mContentView = (TextView) inflater.inflate(R.layout.lb_image_card_view_themed_content, mInfoArea, false);
            mContentView.setTextSize(10);
            mContentView.setTypeface(mulishRegular);
            mContentView.setTextColor(getResources().getColor(cx.ring.R.color.white));
            mInfoArea.addView(mContentView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        }

        if (hasIconRight) {
            int layoutId = R.layout.lb_image_card_view_themed_badge_right;
            mBadgeImage = (ImageView) inflater.inflate(layoutId, mInfoArea, false);
            mInfoArea.addView(mBadgeImage);
        }

        // Set up LayoutParams for children
        if (mBadgeImage != null) {
            RelativeLayout.LayoutParams relativeLayoutParams =
                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (hasTitle) {
                relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                relativeLayoutParams.addRule(RelativeLayout.ALIGN_TOP, mTitleView.getId());
                relativeLayoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, mTitleView.getId());
                relativeLayoutParams.setMargins(0,5,0,0);
            }
            mBadgeImage.setLayoutParams(relativeLayoutParams);
        }

        if (hasTitle && mBadgeImage != null) {
            RelativeLayout.LayoutParams relativeLayoutParams =
                    (RelativeLayout.LayoutParams) mTitleView.getLayoutParams();
            relativeLayoutParams.addRule(RelativeLayout.START_OF, mBadgeImage.getId());
            relativeLayoutParams.addRule(RelativeLayout.LEFT_OF, mBadgeImage.getId());
            mTitleView.setLayoutParams(relativeLayoutParams);
        }

        if (hasContent) {
            RelativeLayout.LayoutParams relativeLayoutParams = (RelativeLayout.LayoutParams) mContentView.getLayoutParams();
            if (!hasTitle) {
                relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            } else {
                relativeLayoutParams.addRule(RelativeLayout.BELOW, mTitleView.getId());
            }
            mContentView.setLayoutParams(relativeLayoutParams);
        }

        cardAttrs.recycle();
    }

    public final ImageView getMainImageView() {
        return mImageView;
    }

    public void setMainImageAdjustViewBounds(boolean adjustViewBounds) {
        if (mImageView != null) {
            mImageView.setAdjustViewBounds(adjustViewBounds);
        }
    }

    public void setMainImage(Drawable drawable) {
        setMainImage(drawable, true);
    }

    public void setMainImage(Drawable drawable, boolean fade) {
        if (mImageView == null) {
            return;
        }

        mImageView.setImageDrawable(drawable);
        if (drawable == null) {
            mFadeInAnimator.cancel();
            mImageView.setAlpha(1f);
            mImageView.setVisibility(View.INVISIBLE);
        } else {
            mImageView.setVisibility(View.VISIBLE);
            if (fade) {
                fadeIn();
            } else {
                mFadeInAnimator.cancel();
                mImageView.setAlpha(1f);
            }
        }
    }

    public void setMainImageDimensions(int width, int height) {
        ViewGroup.LayoutParams lp = mImageView.getLayoutParams();
        lp.width = width;
        lp.height = height;
        mImageView.setLayoutParams(lp);
    }

    public Drawable getMainImage() {
        if (mImageView == null) {
            return null;
        }

        return mImageView.getDrawable();
    }

    public Drawable getInfoAreaBackground() {
        if (mInfoArea != null) {
            return mInfoArea.getBackground();
        }
        return null;
    }

    public void setInfoAreaBackground(Drawable drawable) {
        if (mInfoArea != null) {
            mInfoArea.setBackground(drawable);
        }
    }

    public void setInfoAreaBackgroundColor(@ColorInt int color) {
        if (mInfoArea != null) {
            mInfoArea.setBackgroundColor(color);
        }
    }

    public void setTitleText(CharSequence text) {
        if (mTitleView == null) {
            return;
        }
        mTitleView.setText(text);
    }

    public CharSequence getTitleText() {
        if (mTitleView == null) {
            return null;
        }

        return mTitleView.getText();
    }

    public TextView getTitleTextView() {
        return mTitleView;
    }

    public void setContentText(CharSequence text) {
        if (mContentView == null) {
            return;
        }
        mContentView.setText(text);
    }

    public CharSequence getContentText() {
        if (mContentView == null) {
            return null;
        }

        return mContentView.getText();
    }

    public void setBadgeImage(Drawable drawable) {
        if (mBadgeImage == null) {
            return;
        }
        mBadgeImage.setImageDrawable(drawable);
        if (drawable != null) {
            mBadgeImage.setVisibility(View.VISIBLE);
        } else {
            mBadgeImage.setVisibility(View.GONE);
        }
    }

    public Drawable getBadgeImage() {
        if (mBadgeImage == null) {
            return null;
        }

        return mBadgeImage.getDrawable();
    }

    public void setTitleSingleLine(boolean singleLine) {
        mTitleView.setSingleLine(singleLine);
        mTitleView.setEllipsize(TextUtils.TruncateAt.END);
    }

    private void fadeIn() {
        mImageView.setAlpha(0f);
        if (mAttachedToWindow) {
            mFadeInAnimator.start();
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;
        if (mImageView.getAlpha() == 0) {
            fadeIn();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mAttachedToWindow = false;
        mFadeInAnimator.cancel();
        mImageView.setAlpha(1f);
        super.onDetachedFromWindow();
    }

}
