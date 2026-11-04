package cx.ring.adapters

import android.app.Application
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import cx.ring.R
import cx.ring.fragments.ConversationFragment
import cx.ring.tv.conversation.TvConversationAdapter
import cx.ring.tv.conversation.TvConversationFragment
import cx.ring.viewholders.ConversationViewHolder
import io.reactivex.rxjava3.core.Observable
import net.jami.conversation.ConversationPresenter
import net.jami.model.Contact
import net.jami.model.Uri
import net.jami.model.interaction.DataTransfer
import net.jami.model.interaction.Interaction
import net.jami.model.interaction.Interaction.InteractionType
import net.jami.model.interaction.Interaction.TransferStatus
import net.jami.services.LogService
import net.jami.utils.Log
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MessageGroupingTest {
    private val contact = Contact(Uri.fromId("a".repeat(40)))
    private val hour = 3_600_000L
    private val start = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 4, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private lateinit var context: Context
    private lateinit var presenter: ConversationPresenter
    private lateinit var adapter: ConversationAdapter
    private lateinit var tvAdapter: TvConversationAdapter
    private val holders = mutableListOf<ConversationViewHolder>()

    @Before
    fun setUp() {
        Log.injectLogService(mock(LogService::class.java))
        context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.AppTheme)
        presenter = mock(ConversationPresenter::class.java, RETURNS_DEEP_STUBS)
        `when`(presenter.isGroup()).thenReturn(true)
        `when`(presenter.contactService.observeContact("account", contact, false))
            .thenReturn(Observable.never())
        val fragment = mock(ConversationFragment::class.java)
        `when`(fragment.resources).thenReturn(context.resources)
        `when`(fragment.requireContext()).thenReturn(context)
        adapter = ConversationAdapter(fragment, presenter).apply { showLinkPreviews = false }
        val tvFragment = mock(TvConversationFragment::class.java)
        `when`(tvFragment.resources).thenReturn(context.resources)
        `when`(tvFragment.requireContext()).thenReturn(context)
        tvAdapter = TvConversationAdapter(tvFragment, presenter)
    }

    @After
    fun tearDown() {
        holders.forEach { it.compositeDisposable.clear() }
    }

    @Test
    fun gapIsInclusiveAndMeasuredBetweenNeighbors() {
        assertEquals(hour, MessageGrouping.MAX_GAP_MILLIS)
        val first = message()
        for (gap in listOf(10 * 60_000L, 45 * 60_000L, hour)) {
            assertFalse(MessageGrouping.isSeqBreak(first, message(gap = gap)))
        }
        assertTrue(MessageGrouping.isSeqBreak(first, message(gap = hour + 1)))
        assertSequences(
            listOf(message(), message(gap = hour), message(gap = 2 * hour)),
            listOf("FIRST", "MIDDLE", "LAST")
        )
    }

    @Test
    fun textsCallsAndTransfersCanShareGroups() {
        val types = listOf(InteractionType.TEXT, InteractionType.CALL, InteractionType.DATA_TRANSFER)
        for (firstType in types) {
            for (secondType in types) {
                assertFalse(
                    "$firstType -> $secondType",
                    MessageGrouping.isSeqBreak(message(firstType), message(secondType, 45 * 60_000L))
                )
            }
        }
        assertSequences(
            listOf(message(), transfer("image.png"), transfer("document.pdf", gap = 2_000)),
            listOf("FIRST", "MIDDLE", "LAST")
        )
    }

    @Test
    fun transferStatusAndFilenameDoNotCreateGroupBreaks() {
        for (status in TransferStatus.entries) {
            val transfer = transfer("\uD83D\uDE00", status)
            assertFalse(MessageGrouping.isAlwaysSingleMsg(transfer))
            assertFalse(MessageGrouping.isSeqBreak(message(), transfer))
            assertFalse(MessageGrouping.isSeqBreak(transfer, message(gap = 2_000)))
        }
    }

    @Test
    fun senderDirectionEmojiAndSystemEventsStillBreakGroups() {
        val first = message()
        assertTrue(MessageGrouping.isSeqBreak(first, message().apply { isIncoming = false }))
        assertTrue(MessageGrouping.isSeqBreak(first, message().apply {
            contact = Contact(Uri.fromId("b".repeat(40)))
        }))
        for (type in listOf(InteractionType.CONTACT, InteractionType.COLLAB_DOC)) {
            val event = message(type)
            assertTrue(MessageGrouping.isAlwaysSingleMsg(event))
            assertTrue(MessageGrouping.isSeqBreak(first, event))
            assertTrue(MessageGrouping.isSeqBreak(event, first))
        }
        val emoji = message().apply { body = "\uD83D\uDE00" }
        assertTrue(MessageGrouping.isAlwaysSingleMsg(emoji))
        assertTrue(MessageGrouping.isSeqBreak(first, emoji))
        assertTrue(MessageGrouping.isSeqBreak(emoji, first))
    }

    @Test
    fun repliesStartGroupsAndCanBeFollowedByTransfers() {
        val reply = message(gap = 1_000).apply { replyToId = "replied-message" }
        assertTrue(MessageGrouping.isSeqBreak(message(), reply))
        assertFalse(MessageGrouping.isSeqBreak(reply, transfer("image.png", gap = 2_000)))
        assertSequences(
            listOf(message(), reply, transfer("image.png", gap = 2_000)),
            listOf("SINGLE", "FIRST", "LAST")
        )
    }

    @Test
    fun gapOverOneHourBreaksOnBothSidesOfBoundary() {
        assertSequences(
            listOf(
                message(),
                transfer("image.png"),
                transfer("document.pdf", gap = hour + 1_001),
                message(gap = hour + 2_000)
            ),
            listOf("FIRST", "LAST", "FIRST", "LAST")
        )
    }

    @Test
    fun hiddenInteractionsDoNotSplitGroups() {
        val messages = listOf(
            message(InteractionType.INVALID),
            message(),
            message(InteractionType.INVALID),
            transfer("image.png"),
            message(InteractionType.INVALID),
            message(gap = 2_000)
        )
        adapter.updateDataset(messages.toMutableList())
        tvAdapter.updateDataset(messages)
        for ((position, expected) in listOf(1 to "FIRST", 3 to "MIDDLE", 5 to "LAST")) {
            assertEquals(expected, sequence(adapter, position))
            assertEquals(expected, sequence(tvAdapter, position))
        }
    }

    @Test
    fun localDateBoundaryStillSplitsMainConversationGroups() {
        val midnight = Calendar.getInstance().apply {
            timeInMillis = start
            set(Calendar.HOUR_OF_DAY, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val messages = mutableListOf(
            message().apply { timestamp = midnight - 60_000 },
            transfer("image.png").apply { timestamp = midnight }
        )
        adapter.updateDataset(messages)
        assertEquals("SINGLE", sequence(adapter, 0))
        assertEquals("SINGLE", sequence(adapter, 1, separatorShown = true))
    }

    @Test
    fun fileAndImageSenderDetailsFollowGroupBoundariesAndResetOnRebind() {
        for (name in listOf("document.pdf", "image.png")) {
            val file = transfer(
                name,
                if (name.endsWith(".pdf")) TransferStatus.FILE_AVAILABLE else TransferStatus.TRANSFER_FINISHED
            )
            `when`(presenter.deviceRuntimeService.getConversationPath(file))
                .thenReturn(File(context.cacheDir, name))
            adapter.updateDataset(mutableListOf(file))
            val holder = bind(0)
            val avatar = holder.mAvatar!!
            assertEquals(View.VISIBLE, holder.mPeerDisplayName!!.visibility)
            assertEquals(View.VISIBLE, avatar.visibility)

            adapter.updateDataset(mutableListOf(message(), file, message(gap = 2_000)))
            adapter.onBindViewHolder(holder, 1)
            assertEquals(View.GONE, holder.mPeerDisplayName!!.visibility)
            assertEquals(View.INVISIBLE, avatar.visibility)
            val layout = holder.mAnswerLayout ?: holder.mLayout!!
            assertEquals(0, (layout.layoutParams as MarginLayoutParams).topMargin)

            adapter.updateDataset(mutableListOf(file, message(gap = 2_000)))
            adapter.onBindViewHolder(holder, 0)
            assertEquals(View.VISIBLE, holder.mPeerDisplayName!!.visibility)
            assertEquals(View.INVISIBLE, avatar.visibility)

            adapter.updateDataset(mutableListOf(message(), file))
            adapter.onBindViewHolder(holder, 1)
            assertEquals(View.GONE, holder.mPeerDisplayName!!.visibility)
            assertEquals(View.VISIBLE, avatar.visibility)

            file.timestamp = start + hour + 1
            adapter.onBindViewHolder(holder, 1)
            assertEquals(View.VISIBLE, holder.mPeerDisplayName!!.visibility)
            assertEquals(
                context.resources.getDimensionPixelSize(R.dimen.conversation_message_separation),
                (layout.layoutParams as MarginLayoutParams).topMargin
            )

            `when`(presenter.isGroup()).thenReturn(false)
            adapter.onBindViewHolder(holder, 1)
            assertEquals(View.GONE, holder.mPeerDisplayName!!.visibility)
            assertEquals(View.GONE, avatar.visibility)
            `when`(presenter.isGroup()).thenReturn(true)
        }
    }

    @Test
    fun tvFileAndImageSenderDetailsFollowTextGroupBoundaries() {
        for (name in listOf("document.pdf", "image.png")) {
            val file = transfer(
                name,
                if (name.endsWith(".pdf")) TransferStatus.FILE_AVAILABLE else TransferStatus.TRANSFER_FINISHED
            )
            `when`(presenter.deviceRuntimeService.getConversationPath(file))
                .thenReturn(File(context.cacheDir, name))
            tvAdapter.updateDataset(listOf(file))
            val holder = tvAdapter.onCreateViewHolder(
                FrameLayout(context), tvAdapter.getItemViewType(0)
            ).also { holders.add(it) }
            tvAdapter.onBindViewHolder(holder, 0)
            val avatar = holder.mAvatar!!
            val peerName = holder.mPeerDisplayName
            if (name.endsWith(".pdf")) assertNotNull(peerName) else assertNull(peerName)
            peerName?.let { assertEquals(View.VISIBLE, it.visibility) }
            assertEquals(View.VISIBLE, avatar.visibility)

            tvAdapter.updateDataset(listOf(message(), file, message(gap = 2_000)))
            tvAdapter.onBindViewHolder(holder, 1)
            peerName?.let { assertEquals(View.GONE, it.visibility) }
            assertEquals(View.INVISIBLE, avatar.visibility)

            tvAdapter.updateDataset(listOf(message(), file))
            tvAdapter.onBindViewHolder(holder, 1)
            peerName?.let { assertEquals(View.VISIBLE, it.visibility) }
            assertEquals(View.VISIBLE, avatar.visibility)
        }
    }

    private fun message(type: InteractionType = InteractionType.TEXT, gap: Long = 0) =
        Interaction().apply {
            this.type = type
            account = "account"
            contact = this@MessageGroupingTest.contact
            isIncoming = true
            body = "message"
            timestamp = start + gap
        }

    private fun transfer(
        name: String,
        status: TransferStatus = TransferStatus.TRANSFER_FINISHED,
        gap: Long = 1_000,
    ) = DataTransfer(message(InteractionType.DATA_TRANSFER, gap)).apply {
        body = name
        transferStatus = status
    }

    private fun assertSequences(messages: List<Interaction>, expected: List<String>) {
        adapter.updateDataset(messages.toMutableList())
        tvAdapter.updateDataset(messages)
        for (position in messages.indices) {
            assertEquals("main position $position", expected[position], sequence(adapter, position))
            assertEquals("TV position $position", expected[position], sequence(tvAdapter, position))
        }
    }

    private fun sequence(adapter: Any, position: Int, separatorShown: Boolean = false): String {
        val method = adapter.javaClass.getDeclaredMethod(
            "getMsgSequencing", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType
        ).apply { isAccessible = true }
        return (method.invoke(adapter, position, separatorShown) as Enum<*>).name
    }

    private fun bind(position: Int): ConversationViewHolder =
        adapter.onCreateViewHolder(FrameLayout(context), adapter.getItemViewType(position)).also {
            holders.add(it)
            adapter.onBindViewHolder(it, position)
        }
}
