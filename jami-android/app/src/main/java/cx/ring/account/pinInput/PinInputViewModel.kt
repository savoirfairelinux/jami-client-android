package cx.ring.account.pinInput

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.jami.services.HardwareService
import javax.inject.Inject

@HiltViewModel
class QrCodePinInputViewModel @Inject constructor(
    private val mHardwareService: HardwareService
) : PinInputViewModel() {
    fun cameraPermissionChanged(isGranted: Boolean) {
        if (isGranted && mHardwareService.isVideoAvailable) {
            mHardwareService.initVideo()
                .onErrorComplete()
                .blockingAwait()
        }
    }
}

class EditTextPinInputViewModel : PinInputViewModel()

// State of the pin input
enum class PinValidity {
    VALID, ERROR
}

abstract class PinInputViewModel : ViewModel() {
    // validPinCallback is used to store the valid pin we entered in a tab
    // and restore it when we return to that tab -> enables the connect button
    private lateinit var validPinCallback: (String) -> Unit

    // resetPinCallback is used to reset the pin when switching tabs -> disables the connect button
    private lateinit var resetPinCallback: () -> Unit
    private var pin: String = ""

    fun init(callback: (String) -> Unit, reset: () -> Unit) {
        validPinCallback = callback
        resetPinCallback = reset
    }

    /**
     * Will be called by the view when the pin is entered.
     * If the pin is valid, validPinCallback will be called with the pin as parameter.
     * else resetPinCallback will be called.
     * @param pinToVerify the pin to verify
     * @return the validity of the pin
     */
    fun checkPin(pinToVerify: String): PinValidity {
        // only allow alphanumeric characters in pin format
        val regex = Regex("^[A-Za-z0-9]{8}-[A-Za-z0-9]{8}$")
        // return the pin if it's valid, else reset it and return an error
        return if (regex.matches(pinToVerify)) {
            pin = pinToVerify
            validPinCallback(pin)
            PinValidity.VALID
        } else {
            pin = ""
            resetPinCallback()
            PinValidity.ERROR
        }
    }

    fun emitPinAgain() {
        if (pin != "") validPinCallback(pin)
    }

}