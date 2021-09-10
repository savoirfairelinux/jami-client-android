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
    private val mPendingCalls: MutableList<Call> = ArrayList()
    private val mPendingSubject: Subject<List<Call>> = BehaviorSubject.createDefault(mPendingCalls)
    private var mOnGoingCall = false
    var isAudioOnly = true
        private set
    private var permissionChanged = false
    var isPipMode = false
        private set
    private var incomingIsFullIntent = true
    private var callInitialized = false
    private var videoWidth = -1
    private var videoHeight = -1
    private var previewWidth = -1
    private var previewHeight = -1
    private var currentSurfaceId: String? = null
    private var currentPluginSurfaceId: String? = null
    private var timeUpdateTask: Disposable? = null

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

    override fun unbindView() {
        if (!isAudioOnly) {
            mHardwareService.endCapture()
        }
        super.unbindView()
    }

    override fun bindView(view: CallView) {
        super.bindView(view)
        /*mCompositeDisposable.add(mAccountService.getRegisteredNames()
                .observeOn(mUiScheduler)
                .subscribe(r -> {
                    if (mSipCall != null && mSipCall.getContact() != null) {
                        getView().updateContactBubble(mSipCall.getContact());
                    }
                }));*/
        mCompositeDisposable.add(mHardwareService.getVideoEvents()
            .observeOn(mUiScheduler)
            .subscribe { event: VideoEvent -> onVideoEvent(event) })
        mCompositeDisposable.add(mHardwareService.audioState
            .observeOn(mUiScheduler)
            .subscribe { state: AudioState -> this.view?.updateAudioState(state) })

        /*mCompositeDisposable.add(mHardwareService
                .getBluetoothEvents()
                .subscribe(event -> {
                    if (!event.connected && mSipCall == null) {
                        hangupCall();
                    }
                }));*/
    }

    fun initOutGoing(accountId: String?, conversationUri: Uri?, contactUri: String?, audioOnly: Boolean) {
        Log.e(TAG, "initOutGoing")
        var audioOnly = audioOnly
        if (accountId == null || contactUri == null) {
            Log.e(TAG, "initOutGoing: null account or contact")
            hangupCall()
            return
        }
        if (!mHardwareService.hasCamera()) {
            audioOnly = true
        }
        //getView().blockScreenRotation();
        val callObservable = mCallService
            .placeCall(accountId, conversationUri, fromString(toNumber(contactUri)!!), audioOnly)
            //.map(mCallService::getConference)
            .flatMapObservable { call: Call -> mCallService.getConfUpdates(call) }
            .share()
        mCompositeDisposable.add(callObservable
            .observeOn(mUiScheduler)
            .subscribe({ conference: Conference ->
                contactUpdate(conference)
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
        //getView().blockScreenRotation();

        // if the call is incoming through a full intent, this allows the incoming call to display
        incomingIsFullIntent = actionViewOnly
        val callObservable = mCallService.getConfUpdates(confId)
            .observeOn(mUiScheduler)
            .share()

        // Handles the case where the call has been accepted, emits a single so as to only check for permissions and start the call once
        mCompositeDisposable.add(callObservable
            .firstOrError()
            .subscribe({ call: Conference ->
                if (!actionViewOnly) {
                    contactUpdate(call)
                    confUpdate(call)
                    callInitialized = true
                    view!!.prepareCall(true)
                }
            }) { e: Throwable ->
                hangupCall()
                Log.e(TAG, "Error with initIncoming, preparing call flow :", e)
            })

        // Handles retrieving call updates. Items emitted are only used if call is already in process or if user is returning to a call.
        mCompositeDisposable.add(callObservable
            .subscribe({ call: Conference ->
                if (callInitialized || actionViewOnly) {
                    contactUpdate(call)
                    confUpdate(call)
                }
            }) { e: Throwable ->
                hangupCall()
                Log.e(TAG, "Error with initIncoming, action view flow: ", e)
            })
        showConference(callObservable)
    }

    private fun showConference(conference: Observable<Conference>) {
        var conference = conference
        conference = conference.distinctUntilChanged()
        mCompositeDisposable.add(conference
            .switchMap { obj: Conference -> obj.participantInfo }
            .observeOn(mUiScheduler)
            .subscribe({ info: List<ParticipantInfo> -> view?.updateConfInfo(info) })
            { e: Throwable -> Log.e(TAG, "Error with initIncoming, action view flow: ", e) })
        mCompositeDisposable.add(conference
            .switchMap { obj: Conference -> obj.participantRecording }
            .observeOn(mUiScheduler)
            .subscribe({ contacts: Set<Contact> -> view?.updateParticipantRecording(contacts) })
            { e: Throwable -> Log.e(TAG, "Error with initIncoming, action view flow: ", e) })
    }

    fun prepareOptionMenu() {
        val isSpeakerOn: Boolean = mHardwareService.isSpeakerphoneOn
        //boolean hasContact = mSipCall != null && null != mSipCall.getContact() && mSipCall.getContact().isUnknown();
        val canDial = mOnGoingCall && mConference != null
        // get the preferences
        val displayPluginsButton = view!!.displayPluginsButton()
        val showPluginBtn = displayPluginsButton && mOnGoingCall && mConference != null
        val hasMultipleCamera = mHardwareService.cameraCount > 1 && mOnGoingCall && !isAudioOnly
        view?.initMenu(isSpeakerOn, hasMultipleCamera, canDial, showPluginBtn, mOnGoingCall)
    }

    fun chatClick() {
        if (mConference == null || mConference!!.participants.isEmpty()) {
            return
        }
        val firstCall = mConference!!.participants[0] ?: return
        val c = firstCall.conversation
        if (c is Conversation) {
            view?.goToConversation(c.accountId, c.uri)
        } else if (firstCall.contact != null) {
            view?.goToConversation(firstCall.account!!, firstCall.contact!!.conversationUri.blockingFirst())
        }
    }

    val isSpeakerphoneOn: Boolean
        get() = mHardwareService.isSpeakerphoneOn

    fun speakerClick(checked: Boolean) {
        mHardwareService.toggleSpeakerphone(checked)
    }

    fun muteMicrophoneToggled(checked: Boolean) {
        mCallService.setLocalMediaMuted(mConference!!.id, CallService.MEDIA_TYPE_AUDIO, checked)
    }

    val isMicrophoneMuted: Boolean
        get() = mCallService.isCaptureMuted

    fun switchVideoInputClick() {
        val conference = mConference ?: return
        mHardwareService.switchInput(conference.id, false)
        view?.switchCameraIcon(mHardwareService.isPreviewFromFrontCamera)
    }

    fun configurationChanged(rotation: Int) {
        mHardwareService.setDeviceOrientation(rotation)
    }

    fun dialpadClick() {
        view?.displayDialPadKeyboard()
    }

    fun acceptCall() {
        mConference?.let { mCallService.accept(it.id) }
    }

    fun hangupCall() {
        mConference?.let { conference ->
            if (conference.isConference)
                mCallService.hangUpConference(conference.id)
            else
                mCallService.hangUp(conference.id)
        }
        for (call in mPendingCalls) {
            mCallService.hangUp(call.daemonIdString!!)
        }
        finish()
    }

    fun refuseCall() {
        mConference?.let { mCallService.refuse(it.id) }
        finish()
    }

    fun videoSurfaceCreated(holder: Any) {
        if (mConference == null) {
            return
        }
        val newId = mConference!!.id
        if (newId != currentSurfaceId) {
            currentSurfaceId?.let { id ->
                mHardwareService.removeVideoSurface(id)
            }
            currentSurfaceId = newId
        }
        mHardwareService.addVideoSurface(mConference!!.id, holder)
        view?.displayContactBubble(false)
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
        if (mConference == null) {
            return
        }
        val newId = mConference!!.pluginId
        if (newId != currentPluginSurfaceId) {
            currentPluginSurfaceId?.let { id ->
                mHardwareService.removeVideoSurface(id)
            }
            currentPluginSurfaceId = newId
        }
        mHardwareService.addVideoSurface(mConference!!.pluginId, holder)
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

    fun displayChanged() {
        mHardwareService.switchInput(mConference!!.id, false)
    }

    fun layoutChanged() {
        //getView().resetVideoSize(videoWidth, videoHeight, previewWidth, previewHeight);
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

    private var contactDisposable: Disposable? = null
    private fun contactUpdate(conference: Conference) {
        if (mConference !== conference) {
            mConference = conference
            contactDisposable?.apply { dispose() }
            if (conference.participants.isEmpty()) return

            // Updates of participant (and  pending participant) list
            val callsObservable = mPendingSubject
                .map<List<Call>> { pendingList: List<Call> ->
                    Log.w(TAG, "mPendingSubject onNext " + pendingList.size + " " + conference.participants.size)
                    if (pendingList.isEmpty()) return@map conference.participants
                    val newList: MutableList<Call> = ArrayList(conference.participants.size + pendingList.size)
                    newList.addAll(conference.participants)
                    newList.addAll(pendingList)
                    newList
                }

            // Updates of individual contacts
            val contactsObservable = callsObservable.flatMapSingle { calls: List<Call> ->
                Observable.fromIterable(calls)
                    .map { call: Call -> mContactService.observeContact(call.account!!, call.contact!!, false)
                            .map { call } }
                    .toList(calls.size)
            }

            // Combined updates of contacts as participant list updates
            val contactUpdates = contactsObservable
                .switchMap { list: List<Observable<Call>> -> Observable.combineLatest(list) { objects: Array<Any> ->
                    Log.w(TAG, "flatMapObservable " + objects.size)
                    val calls = ArrayList<Call>(objects.size)
                    for (call in objects) calls.add(call as Call)
                    calls
                } }
                .filter { list: List<Call> -> list.isNotEmpty() }
            contactDisposable = contactUpdates
                .observeOn(mUiScheduler)
                .subscribe({ cs: List<Call> -> view?.updateContactBubble(cs) })
                { e: Throwable -> Log.e(TAG, "Error updating contact data", e) }
                .apply { mCompositeDisposable.add(this) }
        }
        mPendingSubject.onNext(mPendingCalls)
    }

    private fun confUpdate(call: Conference) {
        Log.w(TAG, "confUpdate " + call.id + " " + call.state)
        val status = call.state
        if (status === CallStatus.HOLD) {
            if (call.isSimpleCall) mCallService.unhold(call.id) else JamiService.addMainParticipant(call.id)
        }
        isAudioOnly = !call.hasVideo()
        val view = view ?: return
        view.updateMenu()
        if (call.isOnGoing) {
            mOnGoingCall = true
            view.initNormalStateDisplay(isAudioOnly, isMicrophoneMuted)
            view.updateMenu()
            if (!isAudioOnly) {
                mHardwareService.setPreviewSettings()
                mHardwareService.updatePreviewVideoSurface(call)
                videoSurfaceUpdateId(call.id)
                pluginSurfaceUpdateId(call.pluginId)
                view.displayVideoSurface(true, mDeviceRuntimeService.hasVideoPermission())
                if (permissionChanged) {
                    mHardwareService.switchInput(mConference!!.id, permissionChanged)
                    permissionChanged = false
                }
            }
            timeUpdateTask?.dispose()
            timeUpdateTask = mUiScheduler.schedulePeriodicallyDirect({ updateTime() }, 0, 1, TimeUnit.SECONDS)
        } else if (call.isRinging) {
            val scall = call.call!!
            view.handleCallWakelock(isAudioOnly)
            if (scall.isIncoming) {
                if (mAccountService.getAccount(scall.account!!)!!.isAutoanswerEnabled) {
                    Log.w(TAG, "Accept because of autoanswer")
                    mCallService.accept(scall.daemonIdString!!)
                    // only display the incoming call screen if the notification is a full screen intent
                } else if (incomingIsFullIntent) {
                    view.initIncomingCallDisplay()
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
        var info = info
        val contact = info?.contact
        if (mConference!!.maximizedParticipant == contact) info = null
        mConference!!.maximizedParticipant = contact
        if (info != null) {
            mCallService.setConfMaximizedParticipant(mConference!!.id, info.contact.uri)
        } else {
            mCallService.setConfGridLayout(mConference!!.id)
        }
    }

    private fun updateTime() {
        val view = view
        if (view != null && mConference != null) {
            if (mConference!!.isOnGoing) {
                val start = mConference!!.timestampStart
                if (start != Long.MAX_VALUE) {
                    view.updateTime((System.currentTimeMillis() - start) / 1000)
                } else {
                    view.updateTime(-1)
                }
            }
        }
    }

    private fun onVideoEvent(event: VideoEvent) {
        Log.d(TAG, "VIDEO_EVENT: " + event.start + " " + event.callId + " " + event.w + "x" + event.h)
        val view = view ?: return
        if (event.start) {
            view.displayVideoSurface(true, !isPipMode && mDeviceRuntimeService.hasVideoPermission())
        } else if (mConference != null && mConference!!.id == event.callId) {
            view.displayVideoSurface(event.started, event.started && !isPipMode && mDeviceRuntimeService.hasVideoPermission())
            if (event.started) {
                videoWidth = event.w
                videoHeight = event.h
                view.resetVideoSize(videoWidth, videoHeight)
            }
        } else if (event.callId == null) {
            if (event.started) {
                previewWidth = event.w
                previewHeight = event.h
                view.resetPreviewVideoSize(previewWidth, previewHeight, event.rot)
            }
        }
        if (mConference != null && mConference!!.pluginId == event.callId) {
            if (event.started) {
                previewWidth = event.w
                previewHeight = event.h
                view.resetPluginPreviewVideoSize(previewWidth, previewHeight, event.rot)
            }
        }
        /*if (event.started || event.start) {
            getView().resetVideoSize(videoWidth, videoHeight, previewWidth, previewHeight);
        }*/
    }

    fun positiveButtonClicked() {
        if (mConference!!.isRinging && mConference!!.isIncoming) {
            acceptCall()
        } else {
            hangupCall()
        }
    }

    fun negativeButtonClicked() {
        if (mConference!!.isRinging && mConference!!.isIncoming) {
            refuseCall()
        } else {
            hangupCall()
        }
    }

    fun toggleButtonClicked() {
        if (mConference != null && !(mConference!!.isRinging && mConference!!.isIncoming)) {
            hangupCall()
        }
    }

    fun requestPipMode() {
        if (mConference != null && mConference!!.isOnGoing && mConference!!.hasVideo()) {
            view!!.enterPipMode(mConference!!.id)
        }
    }

    fun pipModeChanged(pip: Boolean) {
        isPipMode = pip
        if (pip) {
            view!!.displayHangupButton(false)
            view!!.displayPreviewSurface(false)
            view!!.displayVideoSurface(true, false)
        } else {
            view!!.displayPreviewSurface(true)
            view!!.displayVideoSurface(true, mDeviceRuntimeService.hasVideoPermission())
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
        mCompositeDisposable.add(mConversationFacade.startConversation(accountId, uri)
            .subscribe { conversation: Conversation ->
                val confs: List<Conference> = conversation.currentCalls
                if (confs.isEmpty()) {
                    val pendingObserver: Observer<Call> = object : Observer<Call> {
                        private var call: Call? = null
                        override fun onSubscribe(d: Disposable) {}
                        override fun onNext(sipCall: Call) {
                            if (call == null) {
                                call = sipCall
                                mPendingCalls.add(sipCall)
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
                    val newCall = mCallService.placeCallObservable(accountId, null, contactUri, isAudioOnly)
                        .doOnEach(pendingObserver)
                        .filter(Call::isOnGoing)
                        .firstElement()
                        .delay(1, TimeUnit.SECONDS)
                        .doOnEvent { v: Call, e: Throwable -> pendingObserver.onComplete() }
                    mCompositeDisposable.add(newCall.subscribe { call: Call ->
                        val id = mConference!!.id
                        if (mConference!!.isConference) {
                            mCallService.addParticipant(call.daemonIdString!!, id)
                        } else {
                            mCallService.joinParticipant(id, call.daemonIdString!!).subscribe()
                        }
                    })
                } else {
                    // Selected contact already in call or conference, join it to current conference
                    val selectedConf = confs[0]
                    if (selectedConf !== mConference) {
                        if (mConference!!.isConference) {
                            if (selectedConf.isConference)
                                mCallService.joinConference(mConference!!.id, selectedConf.id)
                            else
                                mCallService.addParticipant(selectedConf.id, mConference!!.id)
                        } else {
                            if (selectedConf.isConference)
                                mCallService.addParticipant(mConference!!.id, selectedConf.id)
                            else
                                mCallService.joinParticipant(mConference!!.id, selectedConf.id).subscribe()
                        }
                    }
                }
            })
    }

    fun startAddParticipant() {
        view!!.startAddParticipant(mConference!!.id)
    }

    fun hangupParticipant(info: ParticipantInfo) {
        if (info.call != null)
            mCallService.hangUp(info.call.daemonIdString!!)
        else
            mCallService.hangupParticipant(mConference!!.id, info.contact.primaryNumber)
    }

    fun muteParticipant(info: ParticipantInfo, mute: Boolean) {
        mCallService.muteParticipant(mConference!!.id, info.contact.primaryNumber, mute)
    }

    fun openParticipantContact(info: ParticipantInfo) {
        val call = info.call ?: mConference?.firstCall ?: return
        view?.goToContact(call.account!!, info.contact)
    }

    fun stopCapture() {
        mHardwareService.stopCapture()
    }

    fun startScreenShare(mediaProjection: Any?): Boolean {
        return mHardwareService.startScreenShare(mediaProjection)
    }

    fun stopScreenShare() {
        mHardwareService.stopScreenShare()
    }

    fun isMaximized(info: ParticipantInfo): Boolean {
        return mConference?.maximizedParticipant == info.contact
    }

    fun startPlugin(mediaHandlerId: String?) {
        mHardwareService.startMediaHandler(mediaHandlerId)
        mConference?.let { conference -> mHardwareService.switchInput(conference.id, mHardwareService.isPreviewFromFrontCamera) }
    }

    fun stopPlugin() {
        mHardwareService.stopMediaHandler()
        mConference?.let { conference -> mHardwareService.switchInput(conference.id, mHardwareService.isPreviewFromFrontCamera) }
    }

    companion object {
        val TAG = CallPresenter::class.simpleName!!
    }
}