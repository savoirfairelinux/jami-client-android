package cx.ring.service

import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.util.Log
import cx.ring.services.NotificationServiceImpl
import net.jami.model.Conference

class CallConnection(val service: ConnectionService, val request: ConnectionRequest) : Connection() {
    var call: Conference? = null

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
        (service.notificationService as NotificationServiceImpl).manageCallNotification(call!!, false)
    }

    companion object {
        private val TAG: String = CallConnection::class.java.simpleName
    }
}