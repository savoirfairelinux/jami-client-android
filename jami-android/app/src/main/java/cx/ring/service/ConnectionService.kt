/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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

import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresApi
import cx.ring.IRemoteService
import cx.ring.service.RemoteControl.Companion.INCOMING_CALL_RECEIVED_EVENT
import cx.ring.service.RemoteControl.Companion.OUTGOING_CALL_REJECTED_EVENT
import cx.ring.services.CallServiceImpl
import cx.ring.services.CallServiceImpl.Companion.CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY
import cx.ring.utils.ConversationPath
import dagger.hilt.android.AndroidEntryPoint
import net.jami.model.Uri
import net.jami.services.CallService
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import net.jami.services.DeviceRuntimeService
import net.jami.services.NotificationService
import javax.inject.Inject

@RequiresApi(CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY)
@AndroidEntryPoint
class ConnectionService : ConnectionService() {
    @Inject
    lateinit var callService: CallService
    @Inject
    lateinit var contactService: ContactService
    @Inject
    lateinit var conversationFacade: ConversationFacade
    @Inject
    lateinit var notificationService: NotificationService
    @Inject
    lateinit var deviceRuntimeService: DeviceRuntimeService

    private val eventListenerList = mutableListOf<IRemoteService.IEventListener>()

    private fun buildConnection(request: ConnectionRequest, showIncomingCallUi: ((CallConnection, CallRequestResult) -> Unit)? = null): CallConnection =
        CallConnection(this, request, showIncomingCallUi).apply {
            val account = request.extras.getString(ConversationPath.KEY_ACCOUNT_ID)
            val contactId = request.extras.getString(ConversationPath.KEY_CONVERSATION_URI)
            if (account != null && contactId != null) {
                try {
                    val profile = conversationFacade.getConversationProfile(account, Uri.fromString(contactId)).blockingGet()
                    Log.w(TAG, "Set connection metadata ${profile.title} ${android.net.Uri.parse(profile.uriTitle)}")
                    setCallerDisplayName(profile.title, TelecomManager.PRESENTATION_ALLOWED)
                    setAddress(android.net.Uri.parse(profile.uriTitle), TelecomManager.PRESENTATION_UNKNOWN)
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting connection metadata", e)
                    setAddress(request.address, TelecomManager.PRESENTATION_UNKNOWN)
                }
            } else
                setAddress(request.address, TelecomManager.PRESENTATION_UNKNOWN)

            audioModeIsVoip = true
            connectionCapabilities = getCapabilities()
            connectionProperties = getProperties()
        }

    override fun onCreateOutgoingConnection(
        account: PhoneAccountHandle?, request: ConnectionRequest
    ): Connection = buildConnection(request).apply {
        (callService as CallServiceImpl).onPlaceCallResult(request.address, request.extras, this)
    }

    override fun onCreateOutgoingConnectionFailed(
        account: PhoneAccountHandle?, request: ConnectionRequest
    ) {
        Log.w(TAG, "onCreateOutgoingConnectionFailed $request")
        notifyEventListeners(OUTGOING_CALL_REJECTED_EVENT)
        (callService as CallServiceImpl).onPlaceCallResult(request.address, request.extras, null)
    }

    override fun onCreateIncomingConnection(account: PhoneAccountHandle?, request: ConnectionRequest): Connection {
        Log.w(TAG, "onCreateIncomingConnection $request")
        return buildConnection(request) { connection, result ->
            notifyEventListeners(INCOMING_CALL_RECEIVED_EVENT)
            (callService as CallServiceImpl).onIncomingCallResult(request.extras, connection, result)
        }
    }

    override fun onCreateIncomingConnectionFailed(account: PhoneAccountHandle?, request: ConnectionRequest?) {
        Log.w(TAG, "onCreateIncomingConnectionFailed $request")
        if (request != null) {
            (callService as CallServiceImpl).onIncomingCallResult(request.extras, null)
        }
    }

    fun registerEventListener(listener: IRemoteService.IEventListener){
        eventListenerList.add(listener)
    }

    fun unregisterEventListener(listener: IRemoteService.IEventListener){
        eventListenerList.remove(listener)
    }

    private fun notifyEventListeners(name: String, data: Map<String, String>? = null) {
        eventListenerList.forEach { listener ->
            listener.onEventReceived(name, data)
        }
    }


    companion object {
        private val TAG: String = ConnectionService::class.java.simpleName
        const val HANDLE_ID = "jami"

        private const val CAPABILITIES = Connection.CAPABILITY_CAN_SEND_RESPONSE_VIA_CONNECTION or
                Connection.CAPABILITY_CAN_PAUSE_VIDEO or
                Connection.CAPABILITY_SUPPORT_HOLD or
                Connection.CAPABILITY_MUTE /*or
                Connection.CAPABILITY_DISCONNECT_FROM_CONFERENCE or
                Connection.CAPABILITY_SEPARATE_FROM_CONFERENCE */
        private const val PROPERTIES = Connection.PROPERTY_SELF_MANAGED

        fun getCapabilities() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            CAPABILITIES or Connection.CAPABILITY_ADD_PARTICIPANT
        } else CAPABILITIES

        fun getProperties() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            PROPERTIES or Connection.PROPERTY_HIGH_DEF_AUDIO
        } else PROPERTIES
    }
}
