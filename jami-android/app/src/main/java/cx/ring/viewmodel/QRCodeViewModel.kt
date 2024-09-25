package cx.ring.viewmodel

import androidx.annotation.IntDef
import androidx.lifecycle.ViewModel

const val MODE_SCAN = 1
const val MODE_SHARE = 2

@IntDef(flag = true, value = [MODE_SCAN, MODE_SHARE])
@Retention(AnnotationRetention.SOURCE)
annotation class QRCodeMode

class QRCodeViewModel(
    @QRCodeMode private val mode: Int,
) : ViewModel() {

    fun getMode() = mode
}