package cx.ring.account.linkDevice

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import cx.ring.R
import cx.ring.fragments.QRCodeFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.jami.utils.Log

// the different states of the QR code scanner
enum class State {
    INIT, VALID, ERROR
}
class QrToPinViewModel: ViewModel(){

    private val _event = MutableLiveData<State>()
    val event: LiveData<State> = _event

    companion object {
        private val TAG = QrToPinViewModel::class.simpleName!!
    }
    // callback check if the scanned QR code is valid with the regex pattern following
    val callback: BarcodeCallback = BarcodeCallback { result ->
        Log.w(TAG, "BarcodeCallback: $result")
        if (result.text != null) {
            val regex = Regex("^.{8}-.{8}$")
            if (regex.matches(result.text)) {
                _event.value = State.VALID
            } else {
                _event.value = State.ERROR
            }
        }
    }

}