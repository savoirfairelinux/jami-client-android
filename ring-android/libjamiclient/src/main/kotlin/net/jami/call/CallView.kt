/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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

import net.jami.model.Call.CallStatus
import net.jami.model.Conference.ParticipantInfo
import net.jami.model.Contact
import net.jami.model.Uri
import net.jami.services.HardwareService.AudioState

interface CallView {
    fun displayContactBubble(display: Boolean)
    fun displayPeerVideo(display: Boolean)
    fun displayLocalVideo(display: Boolean)
    fun displayHangupButton(display: Boolean)
    fun displayDialPadKeyboard()
    fun updateAudioState(state: AudioState)
    fun updateTime(duration: Long)
    fun updateCallStatus(callState: CallStatus)
    fun updateBottomSheetButtonStatus(isConference: Boolean, isSpeakerOn: Boolean, isMicrophoneMuted: Boolean, hasMultipleCamera: Boolean, canDial: Boolean, showPluginBtn: Boolean, onGoingCall: Boolean, hasActiveVideo: Boolean)
    fun resetBottomSheetState()
    fun initNormalStateDisplay()
    fun initIncomingCallDisplay(hasVideo: Boolean)
    fun initOutGoingCallDisplay()
    fun resetPreviewVideoSize(previewWidth: Int, previewHeight: Int, rot: Int)
    fun resetVideoSize(videoWidth: Int, videoHeight: Int)
    fun goToConversation(accountId: String, conversationId: Uri)
    fun goToAddContact(contact: Contact)
    fun startAddParticipant(conferenceId: String)
    fun finish()
    fun onUserLeave()
    fun enterPipMode(callId: String)
    fun prepareCall(acceptIncomingCall: Boolean)
    fun handleCallWakelock(isAudioOnly: Boolean)
    fun goToContact(accountId: String, contact: Contact)
    fun displayPluginsButton(): Boolean
    fun updateConfInfo(info: List<ParticipantInfo>)
    fun updateParticipantRecording(contacts: Set<Contact>)
    fun toggleCallMediaHandler(id: String, callId: String, toggle: Boolean)
}