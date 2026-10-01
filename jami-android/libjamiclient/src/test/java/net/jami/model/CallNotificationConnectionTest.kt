package net.jami.model

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import net.jami.services.CallService
import net.jami.services.LogService
import net.jami.utils.Log
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CallNotificationConnectionTest {
    @Before
    fun setUp() {
        Log.injectLogService(object : LogService {
            override fun e(tag: String, message: String) {}
            override fun d(tag: String, message: String) {}
            override fun w(tag: String, message: String) {}
            override fun i(tag: String, message: String) {}
            override fun e(tag: String, message: String, e: Throwable) {}
            override fun d(tag: String, message: String, e: Throwable) {}
            override fun w(tag: String, message: String, e: Throwable) {}
            override fun i(tag: String, message: String, e: Throwable) {}
        })
    }

    @Test
    fun connectedCallWithoutTelecomConnectionCanShowNotification() {
        val call = Call("account", "leg", Uri.fromId("peer"), true)
        call.setCallState(Call.CallStatus.CURRENT)

        call.notificationSystemConnection.test()
            .assertValue(CallService.CALL_ALLOWED_VAL)
            .assertComplete()
    }

    @Test
    fun connectedConferenceLegDoesNotBlockLaterCallEvents() {
        val conferenceLeg = Call("account", "conferenceLeg", Uri.fromId("peer"), true)
        conferenceLeg.setCallState(Call.CallStatus.CURRENT)
        val nextCall = Call("account", "nextCall", Uri.fromId("other"), true)
        nextCall.setCallState(Call.CallStatus.RINGING)
        nextCall.setSystemConnection(CallService.SystemCall(true))
        val processed = mutableListOf<String>()

        Observable.just(conferenceLeg, nextCall)
            .concatMapCompletable { call ->
                call.notificationSystemConnection.flatMapCompletable {
                    Completable.fromAction { processed.add(call.id!!) }
                }
            }
            .test()
            .assertComplete()

        assertEquals(listOf("conferenceLeg", "nextCall"), processed)
    }

    @Test
    fun ringingCallWaitsForItsTelecomConnection() {
        val call = Call("account", "leg", Uri.fromId("peer"), true)
        call.setCallState(Call.CallStatus.RINGING)
        val result = call.notificationSystemConnection.test()
        val connection = CallService.SystemCall(true)

        result.assertNoValues().assertNotComplete()
        call.setSystemConnection(connection)
        result.assertValue(connection).assertComplete()
    }

    @Test
    fun connectedCallUsesItsExistingTelecomConnection() {
        val call = Call("account", "leg", Uri.fromId("peer"), true)
        val connection = CallService.SystemCall(true)
        call.setSystemConnection(connection)
        call.setCallState(Call.CallStatus.CURRENT)

        call.notificationSystemConnection.test()
            .assertValue(connection)
            .assertComplete()
    }
}
