package cx.ring.utils

import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.HashMap


object QRCodeLoaderUtils {

    private val mCompositeDisposable = CompositeDisposable()
    private val mUiScheduler = AndroidSchedulers.mainThread()

    fun loadQRCodeData(
        contactUri: String,
        foregroundColor: Int,
        backgroundColor: Int,
        onQRCodeDataLoaded: (QRCodeUtils.QRCodeData) -> Unit
    ) {
        mCompositeDisposable.add(
            Maybe.fromCallable {
                QRCodeUtils.encodeStringAsQRCodeData(
                    contactUri,
                    foregroundColor,
                    backgroundColor
                )
            }
                .subscribeOn(Schedulers.io())
                .observeOn(mUiScheduler)
                .subscribe(
                    { qrCodeData -> onQRCodeDataLoaded(qrCodeData) },
                    { error -> error.printStackTrace() }
                )
        )
    }

    // Utilisez cette méthode pour libérer les ressources quand elles ne sont plus nécessaires
    fun clearDisposables() {
        mCompositeDisposable.clear()
    }
}

// Todo: Delete for production.
object QRCodeUtils {
    private val TAG = QRCodeUtils::class.simpleName!!
    private const val QRCODE_IMAGE_SIZE = 256
    private const val QRCODE_IMAGE_PADDING = 1

    /**
     * @param input uri to be displayed
     * @return the resulting data
     */
    fun encodeStringAsQRCodeData(
        input: String?,
        foregroundColor: Int,
        backgroundColor: Int
    ): QRCodeData? {
        if (input == null || input.isEmpty()) {
            return null
        }
        val qrWriter = QRCodeWriter()
        val qrImageMatrix: BitMatrix
        try {
            val hints = HashMap<EncodeHintType, Int?>()
            hints[EncodeHintType.MARGIN] = QRCODE_IMAGE_PADDING
            qrImageMatrix = qrWriter.encode(
                input,
                BarcodeFormat.QR_CODE,
                QRCODE_IMAGE_SIZE,
                QRCODE_IMAGE_SIZE,
                hints
            )
        } catch (e: WriterException) {
            Log.e(TAG, "Error while encoding QR", e)
            return null
        }
        val qrImageWidth = qrImageMatrix.getWidth()
        val qrImageHeight = qrImageMatrix.getHeight()
        val pixels = IntArray(qrImageWidth * qrImageHeight)
        for (row in 0 until qrImageHeight) {
            val offset = row * qrImageWidth
            for (column in 0 until qrImageWidth) {
                pixels[offset + column] =
                    if (qrImageMatrix[column, row]) foregroundColor else backgroundColor
            }
        }
        return QRCodeData(pixels, qrImageWidth, qrImageHeight)
    }

    class QRCodeData(val data: IntArray, val width: Int, val height: Int)
}