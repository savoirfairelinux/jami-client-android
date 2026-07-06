package net.jami.model.interaction

import net.jami.model.interaction.Interaction.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DataTransferTest {
    @Test
    fun daemonInfoCompletesAvailableTransfer() {
        val file = File.createTempFile("jami-transfer", ".tmp").apply { writeText("data") }
        val transfer = newTransfer().apply {
            transferStatus = TransferStatus.FILE_AVAILABLE
        }

        try {
            assertTrue(transfer.applyDaemonInfo(file, file.length(), file.length()))
            assertEquals(file, transfer.daemonPath)
            assertEquals(file.length(), transfer.totalSize)
            assertEquals(file.length(), transfer.bytesProgress)
            assertEquals(TransferStatus.TRANSFER_FINISHED, transfer.transferStatus)
        } finally {
            file.delete()
        }
    }

    @Test
    fun daemonInfoDoesNotDowngradeOngoingTransfer() {
        val transfer = newTransfer().apply {
            transferStatus = TransferStatus.TRANSFER_ONGOING
            bytesProgress = 8
        }

        assertFalse(transfer.applyDaemonInfo(File("ignored"), 100, 10))
        assertEquals(TransferStatus.TRANSFER_ONGOING, transfer.transferStatus)
        assertEquals(8, transfer.bytesProgress)
        assertEquals(0, transfer.totalSize)
        assertEquals(null, transfer.daemonPath)
    }

    @Test
    fun daemonInfoDoesNotRestoreRemovedTransfer() {
        val transfer = newTransfer().apply {
            transferStatus = TransferStatus.FILE_REMOVED
        }

        assertFalse(transfer.applyDaemonInfo(File("ignored"), 100, 100))
        assertEquals(TransferStatus.FILE_REMOVED, transfer.transferStatus)
    }

    @Test
    fun terminalTransferCannotReturnToOngoing() {
        val transfer = newTransfer().apply {
            transferStatus = TransferStatus.TRANSFER_FINISHED
        }

        assertFalse(transfer.canTransitionTo(TransferStatus.TRANSFER_ONGOING))
        assertTrue(transfer.canTransitionTo(TransferStatus.TRANSFER_FINISHED))
    }

    @Test
    fun ongoingTransferCanFinish() {
        val transfer = newTransfer().apply {
            transferStatus = TransferStatus.TRANSFER_ONGOING
        }

        assertTrue(transfer.canTransitionTo(TransferStatus.TRANSFER_FINISHED))
    }

    private fun newTransfer() = DataTransfer(
        "file-id",
        "account-id",
        "peer-uri",
        "file.txt",
        false,
        0,
        0,
        0
    )
}