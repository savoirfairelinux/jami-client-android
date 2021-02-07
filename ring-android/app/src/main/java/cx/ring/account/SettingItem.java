package cx.ring.account;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public class SettingItem {
    private final int mTitleRes;
    private final int mImageId;
    private final Runnable mOnClick;

    public SettingItem(@StringRes int titleRes, @DrawableRes int imageId, @NonNull Runnable onClick) {
        mTitleRes = titleRes;
        mImageId = imageId;
        mOnClick = onClick;
    }

    public @StringRes int getTitleRes() {
        return mTitleRes;
    }

    public @DrawableRes int getImageId() {
        return mImageId;
    }

    public void onClick() {
        mOnClick.run();
    }
}
