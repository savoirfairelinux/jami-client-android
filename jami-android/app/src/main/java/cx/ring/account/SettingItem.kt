package cx.ring.account

import androidx.annotation.StringRes
import androidx.annotation.DrawableRes

class SettingItem(
    @StringRes val titleRes: Int,
    @DrawableRes val imageId: Int,
    private val mOnClick: Runnable
) {
    fun onClick() {
        mOnClick.run()
    }
}