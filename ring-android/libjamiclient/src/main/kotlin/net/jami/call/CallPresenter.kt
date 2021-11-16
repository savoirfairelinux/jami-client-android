/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.call

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.daemon.JamiService
import net.jami.services.ConversationFacade
import net.jami.model.*
import net.jami.model.Call.CallStatus
import net.jami.model.Conference.ParticipantInfo
import net.jami.model.Uri.Companion.fromString
import net.jami.mvp.RootPresenter
import net.jami.services.*
import net.jami.services.HardwareService.AudioState
import net.jami.services.HardwareService.VideoEvent
import net.jami.utils.Log
import net.jami.utils.StringUtils.toNumber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class CallPresenter @Inject constructor(
    private val mAccountService: AccountService,
    private val mContactService: ContactService,
    private val mHardwareService: HardwareService,
    private val mCallService: CallService,
    private val mDeviceRuntimeService: DeviceRuntimeService,
    private val mConversationFacade: ConversationFacade,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<CallView>() {
    private var mConference: Conference? = null
    private val mPendingCalls: MutableList<ParticipantInfo> = ArrayList()
    private val mPendingSubject: Subject<List<ParticipantInfo>> = BehaviorSubject.createDefault(mPendingCalls)
    var mOnGoingCall = false
        private set
    private var permissionChanged = false
    private var incomingIsFullIntent = true
    private var callInitialized = false
    private var videoWidth = -1
    private var videoHeight = -1
    private var previewWidth = -1
    private var previewHeight = -1
    private var currentSurfaceId: String? = null
    private var currentPluginSurfaceId: String? = null
    private var timeUpdateTask: Disposable? = null
    val isSpeakerphoneOn: Boolean
        get() = mHardwareService.isSpeakerphoneOn
    var isMicrophoneMuted: Boolean = false
    var wantVideo = false
    var videoIsMuted = false
        private set
    var isVideoActive: Boolean = false
        get() = mConference?.hasActiveVideo() == true

    fun cameraPermissionChanged(isGranted: Boolean) {
        if (isGranted && mHardwareService.isVideoAvailable) {
            mHardwareService.initVideo()
                .onErrorComplete()
                .blockingAwait()
            permissionChanged = true
        }
    }

    fun audioPermissionChanged(isGranted: Boolean) {
        if (isGranted && mHardwareService.hasMicrophone()) {
            mCallService.restartAudioLayer()
        }
    }

    override fun bindView(view: CallView) {
        super.bindView(view)
        mCompositeDisposable.add(mHardwareService.getVideoEvents()
            .observeOn(mUiScheduler)
            .subscribe { event: VideoEvent -> onVideoEvent(event) })
        mCompositeDisposable.add(mHardwareService.audioState
            .observeOn(mUiScheduler)
            .subscribe { state: AudioState -> this.view?.updateAudioState(state) })
    }

    fun initOutGoing(accountId: String?, conversationUri: Uri?, contactUri: String?, hasVideo: Boolean) {
        Log.e(TAG, "initOutGoing")
        var pHasVideo = hasVideo
        if (accountId == null || contactUri == null) {
            Log.e(TAG, "initOutGoing: null account or contact")
            hangupCall()
            return
        }
        if (!mHardwareService.hasCamera()) {
            pHasVideo = false
        }
        //getView().blockScreenRotation();
        val callObservable = mCallService
            .placeCall(accountId, conversationUri, fromString(toNumber(contactUri)!!), pHasVideo)
            .flatMapObservable { call: Call -> mCallService.getConfUpdates(call) }
            .share()
        mCompositeDisposable.add(callObservable
            .observeOn(mUiScheduler)
            .subscribe({ conference ->
                Log.w(TAG, "DEBUG subscribe confUpdate(conference)")
                confUpdate(conference)
            }) { e: Throwable ->
                hangupCall()
                Log.e(TAG, "Error with initOutgoing: " + e.message, e)
            })
        showConference(callObservable)
    }

    /**
     * Returns to or starts an incoming call
     *
     * @param confId         the call id
     * @param actionViewOnly true if only returning to call or if using full screen intent
     */
    fun initIncomingCall(confId: String, actionViewOnly: Boolean) {
        // if the call is incoming through a full intent, this allows the incoming call to display
        incomingIsFullIntent = actionViewOnly
        val callObservable = mCallService.getConfUpdates(confId)
            .observeOn(mUiScheduler)
            .share()

        // Handles the case where the call has been accepted, emits a single so as to only check for permissions and start the call once

        if (!actionViewOnly) {
            mCompositeDisposable.add(callObservable
                .firstOrError()
                .subscribe({ call: Conference ->
                    confUpdate(call)
                    callInitialized = true
                    view!!.prepareCall(true)
                }) { e: Throwable ->
                    hangupCall()
                    Log.e(TAG, "Error with initIncoming, preparing call flow :", e)
                })
        }

        // Handles retrieving call updates. Items emitted are only used if call is already in process or if user is returning to a call.
        mCompositeDisposable.add(callObservable
            .subscribe({ call: Conference ->
                if (callInitialized || actionViewOnly) {
                    confUpdate(call)
                }
            }) { e: Throwable ->
                hangupCall()
                Log.e(TAG, "Error with initIncoming, action view flow: ", e)
            })
        showConference(callObservable)
    }

    private fun showConference(conference: Observable<Conference>){
        val conference = conference.distinctUntilChanged()
        mCompositeDisposable.add(conference
            .switchMap { obj: Conference -> Observable.combineLatest(obj.participantInfo, mPendingSubject) { participants, pending ->
                val p = if (participants.isEmpty() && !obj.isConference) {
                    listOf(ParticipantInfo(obj.call, obj.call!!.contact!!, emptyMap()))
                } else {
                    participants
                }
                if (pending.isEmpty()) p else p + pending
            }
            }
            .observeOn(mUiScheduler)
            .subscribe({ info: List<ParticipantInfo> -> view?.updateConfInfo(info) })
            { e: Throwable -> Log.e(TAG, "Error with initIncoming, action view flow: ", e) })
        mCompositeDisposable.add(conference
            .switchMap { obj: Conference -> obj.participantRecording }
            .observeOn(mUiScheduler)
            .subscribe({ contacts: Set<Contact> -> view?.updateParticipantRecording(contacts) })
            { e: Throwable -> Log.e(TAG, "Error with initIncoming, action view flow: ", e) })
    }

    /**
     * Get all the call details in order to display each elements correctly
     * */
    private fun prepareBottomSheetButtonsStatus(conference: Conference) {
        //boolean hasContact = mSipCall != null && null != mSipCall.getContact() && mSipCall.getContact().isUnknown();
        val canDial = mOnGoingCall
        val displayPluginsButton = view!!.displayPluginsButton()
        val showPluginBtn = displayPluginsButton && mOnGoingCall
        val hasActiveVideo = conference.hasActiveVideo()
        val hasMultipleCamera = mHardwareService.cameraCount > 1 && mOnGoingCall && hasActiveVideo

        view?.updateBottomSheetButtonStatus(isSpeakerphoneOn, conference.isAudioMuted, hasMultipleCamera, canDial, showPluginBtn, mOnGoingCall, hasActiveVideo)
    }

    fun chatClick() {
        val conference = mConference ?: return
        if (conference.participants.isEmpty()) return
        val firstCall = conference.participants[0]
        val c = firstCall.conversation
        if (c is Conversation) {
            view?.goToConversation(c.accountId, c.uri)
        } else if (firstCall.contact != null) {
            view?.goToConversation(firstCall.account!!, firstCall.contact!!.conversationUri.blockingFirst())
        }
    }

    fun speakerClick(checked: Boolean) {
        mHardwareService.toggleSpeakerphone(checked)
    }

    /**
     * Mute the local microphone
     * this function is used by the main panel control
     * */
    fun muteMicrophoneToggled(checked: Boolean) {
        val conference = mConference ?: return
        mCallService.setLocalMediaMuted(conference.accountId, conference.id, CallService.MEDIA_TYPE_AUDIO, checked)
    }

    fun switchVideoInputClick() {
        val conference = mConference ?: return
        mHardwareService.switchInput(conference.accountId, conference.id)
    }

    fun switchOnOffCamera() {
        val conference = mConference ?: return
        videoIsMuted = !videoIsMuted
        mCallService.requestVideoMedia(conference, !videoIsMuted)
    }

    fun configurationChanged(rotation: Int) {
        mHardwareService.setDeviceOrientation(rotation)
    }

    fun dialpadClick() {
        view?.displayDialPadKeyboard()
    }

    fun acceptCall(hasVideo: Boolean) {
        mConference?.let { mCallService.accept(it.accountId, it.id, hasVideo) }
    }

    fun hangupCall() {
        mConference?.let { conference ->
            if (conference.isConference)
                mCallService.hangUpConference(conference.accountId, conference.id)
            else
                mCallService.hangUp(conference.accountId, conference.id)
        }
        for (participant in mPendingCalls) {
            val call = participant.call ?: continue
            mCallService.hangUp(call.account!!, call.daemonIdString!!)
        }
        finish()
    }

    fun refuseCall() {
        mConference?.let { mCallService.refuse(it.accountId, it.id) }
        finish()
    }

    fun videoSurfaceCreated(holder: Any) {
        val conference = mConference ?: return
        val newId = conference.id
        if (newId != currentSurfaceId) {
            currentSurfaceId?.let { id ->
                mHardwareService.removeVideoSurface(id)
            }
            currentSurfaceId = newId
        }
        mHardwareService.addVideoSurface(conference.id, holder)
    }

    private fun videoSurfaceUpdateId(newId: String) {
        if (newId != currentSurfaceId) {
            currentSurfaceId?.let { oldId ->
                mHardwareService.updateVideoSurfaceId(oldId, newId)
            }
            currentSurfaceId = newId
        }
    }

    fun pluginSurfaceCreated(holder: Any) {
        val conference = mConference ?: return
        val newId = conference.pluginId
        if (newId != currentPluginSurfaceId) {
            currentPluginSurfaceId?.let { id ->
                mHardwareService.removeVideoSurface(id)
            }
            currentPluginSurfaceId = newId
        }
        mHardwareService.addVideoSurface(conference.pluginId, holder)
        view?.displayContactBubble(false)
    }

    private fun pluginSurfaceUpdateId(newId: String) {
        if (newId != currentPluginSurfaceId) {
            currentPluginSurfaceId?.let { oldId ->
                mHardwareService.updateVideoSurfaceId(oldId, newId)
            }
            currentPluginSurfaceId = newId
        }
    }

    fun previewVideoSurfaceCreated(holder: Any) {
        mHardwareService.addPreviewVideoSurface(holder, mConference)
        //mHardwareService.startCapture(null);
    }

    fun videoSurfaceDestroyed() {
        currentSurfaceId?.let { id ->
            mHardwareService.removeVideoSurface(id)
            currentSurfaceId = null
        }
    }

    fun pluginSurfaceDestroyed() {
        currentPluginSurfaceId?.let { id ->
            mHardwareService.removeVideoSurface(id)
            currentPluginSurfaceId = null
        }
    }

    fun previewVideoSurfaceDestroyed() {
        mHardwareService.removePreviewVideoSurface()
        mHardwareService.endCapture()
    }

    fun uiVisibilityChanged(displayed: Boolean) {
        Log.w(TAG, "uiVisibilityChanged $mOnGoingCall $displayed")
        view?.displayHangupButton(mOnGoingCall && displayed)
    }

    private fun finish() {
        timeUpdateTask?.let { task ->
            if (!task.isDisposed)
                task.dispose()
            timeUpdateTask = null
        }
        mConference = null
        val view = view
        view?.finish()
    }

    private fun confUpdate(call: Conference) {
        mConference = call
        val status = call.state
        if (status === CallStatus.HOLD) {
            if (call.isSimpleCall) mCallService.unhold(call.accountId, call.id) else JamiService.addMainParticipant(call.accountId, call.id)
        }
        val hasVideo = call.hasVideo()
        val hasActiveVideo = call.hasActiveVideo()
        videoIsMuted = !hasActiveVideo
        val view = view ?: return
        if (call.isOnGoing) {
            mOnGoingCall = true
            view.initNormalStateDisplay()
            prepareBottomSheetButtonsStatus(call)
            if (hasVideo) {
                mHardwareService.setPreviewSettings()
                mHardwareService.updatePreviewVideoSurface(call)
                videoSurfaceUpdateId(call.id)
                pluginSurfaceUpdateId(call.pluginId)
                view.displayLocalVideo(hasActiveVideo && mDeviceRuntimeService.hasVideoPermission())
                if (permissionChanged) {
                    mHardwareService.switchInput(call.accountId, call.id, permissionChanged)
                    permissionChanged = false
                }
            }
            if (mHardwareService.hasInput(call.id)) {
                view.displayPeerVideo(true)
            }
            timeUpdateTask?.dispose()
            timeUpdateTask = mUiScheduler.schedulePeriodicallyDirect({ updateTime() }, 0, 1, TimeUnit.SECONDS)
        } else if (call.isRinging) {
            val scall = call.call!!
            view.handleCallWakelock(!hasVideo)
            if (scall.isIncoming) {
                if (mAccountService.getAccount(scall.account)?.isAutoanswerEnabled == true) {
                    Log.w(TAG, "Accept because of autoanswer")
                    mCallService.accept(scall.account!!, scall.daemonIdString!!, wantVideo)
                    // only display the incoming call screen if the notification is a full screen intent
                } else if (incomingIsFullIntent) {
                    view.initIncomingCallDisplay(hasVideo)
                }
            } else {
                mOnGoingCall = false
                view.updateCallStatus(scall.callStatus)
                view.initOutGoingCallDisplay()
            }
        } else {
            finish()
        }
    }

    fun maximizeParticipant(info: ParticipantInfo?) {
        val conference = mConference ?: return
        val contact = info?.contact
        val toMaximize = if (conference.maximizedParticipant == contact) null else info
        conference.maximizedParticipant = contact
        if (toMaximize != null) {
            mCallService.setConfMaximizedParticipant(conference.accountId, conference.id, toMaximize.contact.uri)
        } else {
            mCallService.setConfGridLayout(conference.accountId, conference.id)
        }
    }

    private fun updateTime() {
        val conference = mConference ?: return
        val view = view ?: return
        if (conference.isOnGoing) {
            val start = conference.timestampStart
            if (start != Long.MAX_VALUE) {
                view.updateTime((System.currentTimeMillis() - start) / 1000)
            } else {
                view.updateTime(-1)
            }
        }
    }

    private fun onVideoEvent(event: VideoEvent) {
        Log.w(TAG, "DEBUG onVideoEvent  $event")
        val view = view ?: return
        val conference = mConference
        if (event.callId == null) {
            if (event.start) {
                view.displayLocalVideo(true)
            }
            if (event.started) {
                previewWidth = event.w
                previewHeight = event.h
                view.resetPreviewVideoSize(previewWidth, previewHeight, event.rot)
            }
        } else if (conference != null && conference.id == event.callId) {
            if (event.start) {
                view.displayPeerVideo(true)
            } else if (event.started) {
                videoWidth = event.w
                videoHeight = event.h
                view.resetVideoSize(videoWidth, videoHeight)
            } else {
                view.displayPeerVideo(false)
            }
        }
    }

    fun positiveButtonClicked() {
        val conference = mConference ?: return
        if (conference.isRinging && conference.isIncoming) {
            acceptCall(true)
        } else {
            hangupCall()
        }
    }

    fun negativeButtonClicked() {
        val conference = mConference ?: return
        if (conference.isRinging && conference.isIncoming) {
            refuseCall()
        } else {
            hangupCall()
        }
    }

    fun toggleButtonClicked() {
        val conference = mConference ?: return
        if (!(conference.isRinging && conference.isIncoming)) {
            hangupCall()
        }
    }

    fun requestPipMode() {
        val conference = mConference ?: return
        if (conference.isOnGoing && conference.hasVideo()) {
            view?.enterPipMode(conference.id)
        }
    }

    fun toggleCallMediaHandler(id: String, toggle: Boolean) {
        val conference = mConference ?: return
        if (conference.isOnGoing && conference.hasVideo()) {
            view?.toggleCallMediaHandler(id, conference.id, toggle)
        }
    }

    fun sendDtmf(s: CharSequence) {
        mCallService.playDtmf(s.toString())
    }

    fun addConferenceParticipant(accountId: String, uri: Uri) {
        val conference = mConference ?: return
        mCompositeDisposable.add(mConversationFacade.startConversation(accountId, uri)
            .subscribe { conversation: Conversation ->
                val conf = conversation.currentCall
                if (conf == null) {
                    val pendingObserver: Observer<Call> = object : Observer<Call> {
                        private var call: ParticipantInfo? = null
                        override fun onSubscribe(d: Disposable) {}
                        override fun onNext(sipCall: Call) {
                            if (call == null) {
                                call = ParticipantInfo(sipCall, sipCall.contact ?: conversation.contact!!, emptyMap(), pending = true)
                                    .apply { mPendingCalls.add(this) }
                            }
                            mPendingSubject.onNext(mPendingCalls)
                        }

                        override fun onError(e: Throwable) {}
                        override fun onComplete() {
                            if (call != null) {
                                mPendingCalls.remove(call)
                                mPendingSubject.onNext(mPendingCalls)
                                call = null
                            }
                        }
                    }
                    val contactUri = if (uri.isSwarm) conversation.contact!!.uri else uri

                    // Place new call, join to conference when answered
                    val newCall = mCallService.placeCallObservable(accountId, null, contactUri, wantVideo)
                        .doOnEach(pendingObserver)
                        .filter(Call::isOnGoing)
                        .firstElement()
                        .delay(1, TimeUnit.SECONDS)
                        .doOnEvent { v: Call?, e: Throwable? -> pendingObserver.onComplete() }
                    mCompositeDisposable.add(newCall.subscribe { call: Call ->
                        val id = conference.id
                        if (conference.isConference) {
                            mCallService.addParticipant(call.account!!, call.daemonIdString!!, conference.accountId, id)
                        } else {
                            mCallService.joinParticipant(conference.accountId, id, call.account!!, call.daemonIdString!!).subscribe()
                        }
                    })
                } else if (conf !== conference) {
                    if (conference.isConference) {
                        if (conf.isConference)
                            mCallService.joinConference(conference.accountId, conference.id, conf.accountId, conf.id)
                        else
                            mCallService.addParticipant(conf.accountId, conf.id, conference.accountId, conference.id)
                    } else {
                        if (conf.isConference)
                            mCallService.addParticipant(conference.accountId, conference.id, conf.accountId, conf.id)
                        else
                            mCallService.joinParticipant(conference.accountId, conference.id, conf.accountId, conf.id).subscribe()
                    }
                }
            })
    }

    fun startAddParticipant() {
        view!!.startAddParticipant(mConference!!.id)
    }

    fun hangupParticipant(info: ParticipantInfo) {
        if (info.call != null)
            mCallService.hangUp(info.call.account!!, info.call.daemonIdString!!)
        else
            mCallService.hangupParticipant(mConference!!.accountId, mConference!!.id, info.contact.primaryNumber)
    }

    /**
    * Mute a participant when in conference mode
     * this fonction is used by the recycler view for each participant
     * it allow user to mute when they are moderator
    * */
    fun muteParticipant(info: ParticipantInfo, mute: Boolean) {
        mCallService.muteParticipant(mConference!!.accountId, mConference!!.id, info.contact.primaryNumber, mute)
    }

    fun openParticipantContact(info: ParticipantInfo) {
        val call = info.call ?: mConference?.firstCall ?: return
        view?.goToContact(call.account!!, info.contact)
    }

    fun stopCapture() {
        mHardwareService.stopCapture()
    }

    fun startScreenShare(mediaProjection: Any?): Boolean {
        val conference = mConference ?: return false
        mHardwareService.switchInput(conference.accountId, conference.id, false, mediaProjection)
        return true//mHardwareService.startScreenShare(mediaProjection)
    }

    fun stopScreenShare() {
        val conference = mConference ?: return
        mHardwareService.switchInput(conference.accountId, conference.id, true)
    }

    fun isMaximized(info: ParticipantInfo): Boolean {
        return mConference?.maximizedParticipant == info.contact
    }

    fun startPlugin(mediaHandlerId: String?) {
        mHardwareService.startMediaHandler(mediaHandlerId)
        mConference?.let { conference -> mHardwareService.switchInput(conference.accountId, conference.id, mHardwareService.isPreviewFromFrontCamera) }
    }

    fun stopPlugin() {
        mHardwareService.stopMediaHandler()
        mConference?.let { conference -> mHardwareService.switchInput(conference.accountId, conference.id, mHardwareService.isPreviewFromFrontCamera) }
    }

    companion object {
        val TAG = CallPresenter::class.simpleName!!
    }
}