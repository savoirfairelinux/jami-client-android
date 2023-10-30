/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package cx.ring.services

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import cx.ring.application.JamiApplication
import cx.ring.service.CallConnection
import cx.ring.service.CallRequestResult
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.SingleSubject
import net.jami.model.Call
import net.jami.model.Media
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.CallService
import net.jami.services.ContactService
import net.jami.services.DeviceRuntimeService
import net.jami.services.NotificationService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService

class CallServiceImpl(val mContext: Context, executor: ScheduledExecutorService,
                      contactService: ContactService,
                      accountService: AccountService,
                      deviceRuntimeService: DeviceRuntimeService
): CallService(executor, contactService, accountService, deviceRuntimeService) {

    private val pendingCallRequests = ConcurrentHashMap<String, SingleSubject<SystemCall>>()
    private val incomingCallRequests = ConcurrentHashMap<String, Pair<Call, SingleSubject<SystemCall>>>()

    class AndroidCall(val connection: CallConnection?): SystemCall(connection != null) {
        override fun setCall(call: Call) {
            if (Build.VERSION.SDK_INT >= CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY) {
                this.connection?.call = call
                call.setSystemConnection(this)
            } else {
                call.setSystemConnection(null)
            }
        }

    }

    override fun requestPlaceCall(accountId: String, conversationUri: Uri?, contactUri: String, hasVideo: Boolean): Single<SystemCall> {
        // Use the Android Telecom API to implement requestPlaceCall if available

        // Disabled because doesn't seem well integrated. GitLab: #1337.
        if(DeviceUtils.isTv(mContext)) return CALL_ALLOWED

        if (Build.VERSION.SDK_INT >= CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY) {
            mContext.getSystemService<TelecomManager>()?.let { telecomService ->
                val accountHandle = JamiApplication.instance!!.androidPhoneAccountHandle ?: return CALL_ALLOWED

                // Disabled because of a bug on Lenovo Tab P12 Pro (Android 12) where
                // isOutgoingCallPermitted() is always returning false. GitLab: #1288.
                // Less optimal but still functional.
                /* // Dismiss the call immediately if disallowed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!telecomService.isOutgoingCallPermitted(accountHandle))
                    return CALL_DISALLOWED
            }*/

                // Build call parameters
                val params = Bundle().apply {
                    putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle)
                    putBundle(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, Bundle().apply {
                        putString(ConversationPath.KEY_ACCOUNT_ID, accountId)
                        putString(ConversationPath.KEY_CONVERSATION_URI, contactUri)
                        if (conversationUri != null)
                            putString(ConversationPath.KEY_CONVERSATION_URI, conversationUri.uri)
                    })
                    putInt(
                        TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                        if (hasVideo) VideoProfile.STATE_BIDIRECTIONAL
                        else VideoProfile.STATE_AUDIO_ONLY
                    )
                }

                // Build contact' Android URI
                val callUri = android.net.Uri.parse(contactUri)
                val key = "$accountId/$callUri"
                val subject = SingleSubject.create<SystemCall>()

                // Place call request
                pendingCallRequests[key] = subject
                try {
                    Log.w(TAG, "Telecom API: new outgoing call request for $callUri")
                    telecomService.placeCall(callUri, params)
                    return subject
                } catch (e: SecurityException) {
                    pendingCallRequests.remove(key)
                    Log.e(TAG, "Can't use the Telecom API to place call", e)
                }
            }
        }
        // Fallback to allowing the call
        return CALL_ALLOWED
    }

    @RequiresApi(CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY)
    fun onPlaceCallResult(uri: android.net.Uri, extras: Bundle, result: CallConnection?) {
        val accountId = extras.getString(ConversationPath.KEY_ACCOUNT_ID) ?: return
        Log.w(TAG, "Telecom API: outgoing call request for $uri has result $result")
        val call = pendingCallRequests.remove("$accountId/$uri")
        if (call != null)
            call.onSuccess(AndroidCall(result))
        else result?.dispose()
    }

    override fun requestIncomingCall(call: Call): Single<SystemCall> {
        // Use the Android Telecom API if available

        // Disabled because doesn't seem well integrated. GitLab: #1337.
        if(DeviceUtils.isTv(mContext)) return CALL_ALLOWED

        if (Build.VERSION.SDK_INT >= CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY) {
            mContext.getSystemService<TelecomManager>()?.let { telecomService ->
                val accountHandle = JamiApplication.instance!!.androidPhoneAccountHandle ?: return CALL_ALLOWED
                val extras = Bundle()
                if (call.hasActiveMedia(Media.MediaType.MEDIA_TYPE_VIDEO))
                    extras.putInt(
                        TelecomManager.EXTRA_INCOMING_VIDEO_STATE,
                        VideoProfile.STATE_BIDIRECTIONAL
                    )
                extras.putString(ConversationPath.KEY_ACCOUNT_ID, call.account)
                extras.putString(NotificationService.KEY_CALL_ID, call.daemonIdString)
                extras.putString(
                    ConversationPath.KEY_CONVERSATION_URI,
                    call.contact?.uri?.rawUriString
                )

                val key = call.daemonIdString!!
                val subject = SingleSubject.create<SystemCall>()

                // Place call request
                incomingCallRequests[key] = Pair(call, subject)
                try {
                    Log.w(TAG, "Telecom API: new incoming call request for $key")
                    telecomService.addNewIncomingCall(accountHandle, extras)
                    return subject
                } catch (e: SecurityException) {
                    incomingCallRequests.remove(key)
                    Log.e(TAG, "Can't use the Telecom API to place call", e)
                }
            }
        }
        // Fallback to allowing the call
        return CALL_ALLOWED
    }

    @RequiresApi(CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY)
    fun onIncomingCallResult(extras: Bundle, connection: CallConnection?, result: CallRequestResult = CallRequestResult.REJECTED) {
        val accountId = extras.getString(ConversationPath.KEY_ACCOUNT_ID) ?: return
        val callId = extras.getString(NotificationService.KEY_CALL_ID) ?: return
        Log.w(TAG, "Telecom API: incoming call request for $callId has result $connection")
        val call = incomingCallRequests.remove(callId)?.second
        if (call == null) {
            Log.e(TAG, "Telecom API: incoming call request for $callId has no pending request")
            connection?.dispose()
            return
        }
        call.onSuccess(if (connection != null && result != CallRequestResult.REJECTED)
            AndroidCall(connection)
        else
            SystemCall(false))

        if (connection == null || result == CallRequestResult.REJECTED)
            refuse(accountId, callId)
        else if (result == CallRequestResult.ACCEPTED || result == CallRequestResult.ACCEPTED_VIDEO)
            accept(accountId, callId, result == CallRequestResult.ACCEPTED_VIDEO)
        if (result == CallRequestResult.REJECTED)
            connection?.dispose()
    }

    companion object {
        const val CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY : Int = Build.VERSION_CODES.P
    }
}