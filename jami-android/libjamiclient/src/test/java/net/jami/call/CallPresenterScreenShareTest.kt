package net.jami.call

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Call
import net.jami.model.Conference
import net.jami.model.Media
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.CallService
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import net.jami.services.DeviceRuntimeService
import net.jami.services.HardwareService
import net.jami.services.NotificationService
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Answers
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class CallPresenterScreenShareTest {
    private val hardware = mock(HardwareService::class.java)
    private val calls = mock(CallService::class.java)
    private val view = mock(CallView::class.java)
    private val call = Call("account", "leg", Uri.fromId("peer"), true)
    private var conference = Conference(call)
    private val data = Any()
    private val projection = Any()
    private val pendingScreenShares = mutableListOf<() -> Unit>()
    private lateinit var presenter: CallPresenter

    @Before
    fun setUp() {
        `when`(hardware.getCameraEvents()).thenReturn(Observable.never())
        `when`(hardware.changeCamera(true)).thenReturn("0")
        `when`(view.getMediaProjection(0, data)).thenReturn(projection)
        call.setMediaList(listOf(Media.DEFAULT_AUDIO))
        val notifications = mock(NotificationService::class.java) { invocation ->
            if (invocation.method.name == "preparePendingScreenshare") {
                pendingScreenShares.add(invocation.getArgument(1))
                null
            } else {
                Answers.RETURNS_DEFAULTS.answer(invocation)
            }
        }
        presenter = CallPresenter(
            mock(AccountService::class.java),
            mock(ContactService::class.java),
            hardware,
            calls,
            mock(DeviceRuntimeService::class.java),
            mock(ConversationFacade::class.java),
            notifications,
            Schedulers.trampoline()
        )
        presenter.bindView(view)
        setConference(conference)
    }

    @Test
    fun stoppingScreenShareInAudioOnlyCallKeepsCameraMuted() {
        startSharing()

        presenter.switchOnOffScreenShare()

        verify(calls).replaceVideoMedia(conference, "camera://0", true)
    }

    @Test
    fun stoppingScreenShareRestoresActiveCamera() {
        setCamera(muted = false)
        startSharing()

        presenter.switchOnOffScreenShare()

        verify(calls).replaceVideoMedia(conference, "camera://0", false)
    }

    @Test
    fun stoppingScreenShareRestoresMutedCamera() {
        setCamera(muted = true)
        startSharing()

        presenter.switchOnOffScreenShare()

        verify(calls).replaceVideoMedia(conference, "camera://0", true)
    }

    @Test
    fun stoppingScreenShareDoesNotEnableDisabledCamera() {
        setCamera(muted = false, enabled = false)
        startSharing()

        presenter.switchOnOffScreenShare()

        verify(calls).replaceVideoMedia(conference, "camera://0", true)
    }

    @Test
    fun missingSavedStateKeepsCameraMuted() {
        setScreenSharingMedia()

        presenter.switchOnOffScreenShare()

        verify(calls).replaceVideoMedia(conference, "camera://0", true)
    }

    @Test
    fun repeatedScreenShareStartPreservesOriginalCameraState() {
        setCamera(muted = false)
        startSharing()
        startSharing()

        presenter.switchOnOffScreenShare()

        verify(calls).replaceVideoMedia(conference, "camera://0", false)
    }

    @Test
    fun cameraStateIsSavedWhenScreenCaptureBecomesReady() {
        setCamera(muted = false)
        assertTrue(presenter.startScreenShare(0, data))
        setCamera(muted = true)
        finishStartingScreenShare()

        presenter.switchOnOffScreenShare()

        verify(calls).replaceVideoMedia(conference, "camera://0", true)
    }

    @Test
    fun failedProjectionDoesNotOverwriteSavedCameraState() {
        setCamera(muted = false)
        startSharing()
        `when`(view.getMediaProjection(0, data)).thenReturn(null)
        assertTrue(presenter.startScreenShare(0, data))
        pendingScreenShares.removeAt(0).invoke()

        presenter.switchOnOffScreenShare()

        verify(calls).replaceVideoMedia(conference, "camera://0", false)
    }

    @Test
    fun hostOnlyConferenceRestoresItsCameraState() {
        conference = Conference(call.account, "hosted").also { hostedConference ->
            hostedConference.hostCall = call
        }
        setConference(conference)
        setCamera(muted = false)
        startSharing()

        presenter.switchOnOffScreenShare()

        verify(calls).replaceVideoMedia(conference, "camera://0", false)
    }

    @Test
    fun requestingScreenCaptureDoesNotSwitchCamera() {
        presenter.switchOnOffScreenShare()

        verify(view).startScreenCapture()
        verify(hardware, never()).changeCamera(true)
    }

    private fun setConference(value: Conference) {
        CallPresenter::class.java.getDeclaredField("mConference").apply {
            isAccessible = true
            set(presenter, value)
        }
    }

    private fun setCamera(muted: Boolean, enabled: Boolean = true) {
        call.setMediaList(listOf(
            Media.DEFAULT_AUDIO,
            Media.DEFAULT_VIDEO.copy(source = "camera://0", isEnabled = enabled, isMuted = muted)
        ))
    }

    private fun setScreenSharingMedia() {
        call.setMediaList(listOf(Media.DEFAULT_AUDIO, Media.DEFAULT_VIDEO.copy(source = "camera://desktop")))
    }

    private fun startSharing() {
        assertTrue(presenter.startScreenShare(0, data))
        finishStartingScreenShare()
    }

    private fun finishStartingScreenShare() {
        pendingScreenShares.removeAt(0).invoke()
        verify(hardware, atLeastOnce()).setPendingScreenShareProjection(projection)
        setScreenSharingMedia()
    }
}