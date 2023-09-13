package cx.ring.account.qrToPin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.journeyapps.barcodescanner.BarcodeCallback

// the different states of the QR code scanner
enum class QrResultState {
    INIT, VALID, ERROR
}

class QrToPinViewModel : ViewModel() {

    private val _event = MutableLiveData<QrResultState>()
    val event: LiveData<QrResultState> = _event

    companion object {
        private val TAG = QrToPinViewModel::class.simpleName!!
    }

    // callback check if the scanned QR code is valid with the regex pattern following
    val callback: BarcodeCallback = BarcodeCallback { result ->
        if (result.text != null) {
            // only allow alphanumeric characters
            val regex = Regex("^[A-Za-z0-9]{8}-[A-Za-z0-9]{8}$")
            if (regex.matches(result.text)) {
                _event.value = QrResultState.VALID
            } else {
                _event.value = QrResultState.ERROR
            }
        }
    }

}