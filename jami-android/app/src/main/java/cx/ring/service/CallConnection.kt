/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.service

import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.DisconnectCause
import android.telecom.VideoProfile
import android.util.Log
import androidx.annotation.RequiresApi
import cx.ring.services.CallServiceImpl.Companion.CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.model.Call

enum class CallRequestResult {
    ACCEPTED,
    ACCEPTED_VIDEO,
    REJECTED,
    IGNORED,
    SHOW_UI
}

/**
 * Implements a Connection from the Android Telecom API.
 */
@RequiresApi(CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY)
class CallConnection(
    val service: ConnectionService,
    val request: ConnectionRequest,
    private val showIncomingCallUi: ((CallConnection, CallRequestResult) -> Unit)?
) : Connection() {

    private val audioStateSubject: Subject<CallAudioState> = BehaviorSubject.create()
    private val wantedAudioStateSubject: Subject<List<Int>> = BehaviorSubject.create()

    val audioState: Observable<CallAudioState>
        get() = audioStateSubject

    var call: Call? = null
        set(value) {
            field = value
            disposable.clear()
            if (value != null) {
                if (value.callStatus == Call.CallStatus.RINGING)
                    setRinging()
                disposable.add(service.callService.callsUpdates
                    .filter { it === value }
                    .subscribe { call ->
                        val status = call.callStatus
                        // Set the HOLD capability if the call is current
                        connectionCapabilities = if (status == Call.CallStatus.CURRENT) {
                            connectionCapabilities or CAPABILITY_HOLD
                        } else {
                            connectionCapabilities and CAPABILITY_HOLD.inv()
                        }
                        if (status == Call.CallStatus.CURRENT)
                            callAudioState?.let { audioStateSubject.onNext(it) }
                        // Update call status
                        when (status) {
                            Call.CallStatus.RINGING -> if (call.isIncoming) setRinging() else setDialing()
                            Call.CallStatus.CURRENT -> setActive()
                            Call.CallStatus.HOLD -> setOnHold()
                            Call.CallStatus.INACTIVE -> setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                            Call.CallStatus.FAILURE -> setDisconnected(DisconnectCause(DisconnectCause.ERROR))
                            Call.CallStatus.HUNGUP -> setDisconnected(DisconnectCause(DisconnectCause.REMOTE))
                            Call.CallStatus.OVER -> dispose()
                            else -> {}
                        }
                    })
                disposable.add(Observable
                    .combineLatest(audioStateSubject, wantedAudioStateSubject) { a, w -> Pair(a, w) }
                    .subscribe { (audioState, wantedList) ->
                        val supported = audioState.supportedRouteMask
                        wantedList.firstOrNull { it and supported != 0 }?.let {
                            setAudioRoute(it)
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
        val call = call ?: return
        service.callService.hangUp(call.account!!, call.daemonIdString!!)
    }

    override fun onAnswer(videoState: Int) {
        Log.w(TAG, "onAnswer $videoState")
        showIncomingCallUi?.invoke(this, if (videoState == VideoProfile.STATE_BIDIRECTIONAL) CallRequestResult.ACCEPTED_VIDEO else CallRequestResult.ACCEPTED)
    }

    override fun onReject() {
        Log.w(TAG, "onReject")
        showIncomingCallUi?.invoke(this, CallRequestResult.REJECTED)
    }

    override fun onHold() {
        Log.w(TAG, "onHold")
        val call = call ?: return
        service.callService.hold(call.account!!, call.daemonIdString!!)
    }

    override fun onUnhold() {
        Log.w(TAG, "onUnhold")
        val call = call ?: return
        service.callService.unhold(call.account!!, call.daemonIdString!!)
    }

    override fun onSilence() {
        Log.w(TAG, "onSilence")
    }

    override fun onDisconnect() {
        Log.w(TAG, "onDisconnect")
        val call = call ?: return
        service.callService.hangUp(call.account!!, call.daemonIdString!!)
    }

    override fun onPlayDtmfTone(c: Char) {
        Log.w(TAG, "onPlayDtmfTone $c")
        service.callService.playDtmf(c.toString())
    }

    override fun onCallAudioStateChanged(state: CallAudioState) {
        Log.w(TAG, "onCallAudioStateChanged: $state")
        audioStateSubject.onNext(state)
    }

    override fun onShowIncomingCallUi() {
        Log.w(TAG, "onShowIncomingCallUi")
        showIncomingCallUi?.invoke(this, CallRequestResult.SHOW_UI)
    }

    fun setWantedAudioState(wanted: List<Int>) {
        wantedAudioStateSubject.onNext(wanted)
    }

    companion object {
        private val TAG: String = CallConnection::class.java.simpleName

        /** Default route list for audio calls */
        val ROUTE_LIST_DEFAULT = listOf(
            CallAudioState.ROUTE_BLUETOOTH,
            CallAudioState.ROUTE_WIRED_HEADSET,
            CallAudioState.ROUTE_WIRED_OR_EARPIECE,
            CallAudioState.ROUTE_SPEAKER
        )
        /** Default route list for ringtone and video calls */
        val ROUTE_LIST_SPEAKER_IMPLICIT = listOf(
            CallAudioState.ROUTE_BLUETOOTH,
            CallAudioState.ROUTE_WIRED_HEADSET,
            CallAudioState.ROUTE_SPEAKER,
            CallAudioState.ROUTE_WIRED_OR_EARPIECE
        )
        /** Route list when the user selects the speaker explicitly */
        val ROUTE_LIST_SPEAKER_EXPLICIT = listOf(
            CallAudioState.ROUTE_SPEAKER,
            CallAudioState.ROUTE_BLUETOOTH,
            CallAudioState.ROUTE_WIRED_HEADSET,
            CallAudioState.ROUTE_WIRED_OR_EARPIECE
        )
    }
}