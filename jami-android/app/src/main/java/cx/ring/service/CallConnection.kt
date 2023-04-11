package cx.ring.service

import android.os.Build
import android.telecom.CallAudioState
import android.telecom.Connection
import android.util.Log
import androidx.annotation.RequiresApi
import cx.ring.services.NotificationServiceImpl
import net.jami.model.Conference

@RequiresApi(Build.VERSION_CODES.O)
class CallConnection(val service: ConnectionService, val conference: Conference) : Connection() {
    override fun onAbort() {
        Log.w(TAG, "onAbort")
    }

    override fun onAnswer() {
        Log.w(TAG, "onAnswer")
    }

    override fun onReject() {
        Log.w(TAG, "onReject")
    }

    override fun onHold() {
        Log.w(TAG, "onHold")
    }

    override fun onPlayDtmfTone(c: Char) {
        Log.w(TAG, "onPlayDtmfTone $c")
    }

    override fun onCallAudioStateChanged(state: CallAudioState?) {
        Log.w(TAG, "onCallAudioStateChanged: $state")
    }

    override fun onShowIncomingCallUi() {
        Log.w(TAG, "onShowIncomingCallUi")
        (service.notificationService as NotificationServiceImpl).manageCallNotification(conference, false)
    }

    companion object {
        private val TAG: String = CallConnection::class.java.simpleName
    }
}