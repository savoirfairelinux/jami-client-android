package cx.ring.account.QrToPin

import com.journeyapps.barcodescanner.BarcodeCallback
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.jami.utils.Log

// the different states of the QR code scanner
enum class View {
    INIT, VALID, ERROR
}

class QrToPinViewModel : ViewModel() {

    private val _event = MutableLiveData<View>()
    val event: LiveData<View> = _event

    companion object {
        private val TAG = QrToPinViewModel::class.simpleName!!
    }

    // callback check if the scanned QR code is valid with the regex pattern following
    val callback: BarcodeCallback = BarcodeCallback { result ->
        Log.w(TAG, "BarcodeCallback: $result")
        if (result.text != null) {
            val regex = Regex("^.{8}-.{8}$")
            if (regex.matches(result.text)) {
                _event.value = View.VALID
            } else {
                _event.value = View.ERROR
            }
        }
    }

}