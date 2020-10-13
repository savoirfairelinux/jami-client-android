package cx.ring.account;

public class SettingItem {

    private int mTitleRes;
    private int mImageId;

    public SettingItem(int titleRes, int imageId) {
        mTitleRes = titleRes;
        mImageId = imageId;
    }

    public int getTitleRes() {
        return mTitleRes;
    }

    public void setTitleRes(int titleRes) {
        mTitleRes = titleRes;
    }

    public int getImageId() {
        return mImageId;
    }

    public void setImageId(int imageId) {
        mImageId = imageId;
    }

}
