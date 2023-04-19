package cx.ring.service

import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.DisconnectCause
import android.telecom.VideoProfile
import android.util.Log
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Call
import net.jami.model.Conference

class CallConnection(
    val service: ConnectionService,
    val request: ConnectionRequest,
    private val showIncomingCallUi: ((CallConnection) -> Unit)?
) : Connection() {
    var call: Conference? = null
        set(value) {
            field = value
            disposable.clear()
            if (value != null) {
                val call = value.call
                if (call?.callStatus == Call.CallStatus.RINGING)
                    setRinging()
                disposable.add(service.callService.callsUpdates
                    .filter { it === call }
                    .distinctUntilChanged { p, n -> p.callStatus == n.callStatus }
                    .subscribe {
                        val status = it.callStatus
                        Log.w(TAG, "CallConnection state change $status")
                        // Set the HOLD capability if the call is current
                        connectionCapabilities = if (status == Call.CallStatus.CURRENT) {
                            connectionCapabilities or CAPABILITY_HOLD
                        } else {
                            connectionCapabilities and CAPABILITY_HOLD.inv()
                        }
                        // Update call status
                        when (status) {
                            Call.CallStatus.RINGING -> if (it.isIncoming) setRinging() else setDialing()
                            Call.CallStatus.CURRENT -> setActive()
                            Call.CallStatus.HOLD -> setOnHold()
                            Call.CallStatus.INACTIVE -> setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                            Call.CallStatus.FAILURE -> setDisconnected(DisconnectCause(DisconnectCause.ERROR))
                            Call.CallStatus.HUNGUP -> setDisconnected(DisconnectCause(DisconnectCause.REMOTE))
                            Call.CallStatus.OVER -> dispose()
                            else -> {}
                        }
                    })
            }
        }
    val disposable = CompositeDisposable()

    fun dispose() {
        disposable.dispose()
        destroy()
    }

    override fun onAbort() {
        Log.w(TAG, "onAbort")
        service.callService.hangUpAny(call!!.accountId, call!!.id)
    }

    override fun onAnswer(videoState: Int) {
        Log.w(TAG, "onAnswer")
        service.callService.accept(call!!.accountId, call!!.id, videoState == VideoProfile.STATE_BIDIRECTIONAL)
    }

    override fun onReject() {
        Log.w(TAG, "onReject")
        service.callService.refuse(call!!.accountId, call!!.id)
    }

    override fun onHold() {
        Log.w(TAG, "onHold")
        service.callService.holdCallOrConference(call!!)
    }

    override fun onUnhold() {
        Log.w(TAG, "onUnhold")
        service.callService.unholdCallOrConference(call!!)
    }

    override fun onSilence() {
        Log.w(TAG, "onSilence")
    }

    override fun onDisconnect() {
        Log.w(TAG, "onDisconnect")
        service.callService.hangUpAny(call!!.accountId, call!!.id)
    }

    override fun onPlayDtmfTone(c: Char) {
        Log.w(TAG, "onPlayDtmfTone $c")
        service.callService.playDtmf(c.toString())
    }

    override fun onCallAudioStateChanged(state: CallAudioState?) {
        Log.w(TAG, "onCallAudioStateChanged: $state")
    }

    override fun onShowIncomingCallUi() {
        Log.w(TAG, "onShowIncomingCallUi")
        showIncomingCallUi?.invoke(this)
    }

    companion object {
        private val TAG: String = CallConnection::class.java.simpleName
    }
}