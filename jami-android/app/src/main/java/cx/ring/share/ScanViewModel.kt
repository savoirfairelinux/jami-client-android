package cx.ring.share

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.jami.services.HardwareService
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val mHardwareService: HardwareService
): ViewModel() {
    fun cameraPermissionChanged(isGranted: Boolean) {
        if (isGranted && mHardwareService.isVideoAvailable) {
            mHardwareService.initVideo()
                .onErrorComplete()
                .blockingAwait()
        }
    }
}