package cx.ring.account.pinInput

import androidx.lifecycle.ViewModel

class QrCodePinInputViewModel : PinInputViewModel()

class EditTextPinInputViewModel : PinInputViewModel()

// State of the pin input
enum class PinValidity {
    VALID, ERROR
}

abstract class PinInputViewModel : ViewModel() {

    private lateinit var outputCallback: (String) -> Unit
    private lateinit var resetPin: () -> Unit
    private var pin: String = ""

    fun init(callback: (String) -> Unit, reset: () -> Unit) {
        outputCallback = callback
        resetPin = reset
    }

    fun checkPin(pinToVerify: String): PinValidity {
        // only allow alphanumeric characters in pin format
        val regex = Regex("^[A-Za-z0-9]{8}-[A-Za-z0-9]{8}$")
        // return the pin if it's valid, else reset it and return an error
        return if (regex.matches(pinToVerify)) {
            pin = pinToVerify
            setPin(pinToVerify)
            PinValidity.VALID
        } else {
            resetPin()
            PinValidity.ERROR
        }

    }

    fun emitPinAgain() {
        if (pin != "") outputCallback(pin)
    }

    private fun setPin(pin: String) {
        outputCallback(pin)
    }

}