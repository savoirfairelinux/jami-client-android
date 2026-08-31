package net.jami.model.interaction

import net.jami.model.interaction.Interaction.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

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
    fun daemonInfoCompletesEmptyRegularFile() {
        val file = File.createTempFile("jami-transfer", ".tmp")
        val transfer = newTransfer().apply {
            transferStatus = TransferStatus.FILE_AVAILABLE
        }

        try {
            assertTrue(transfer.applyDaemonInfo(file, 0, 0))
            assertEquals(TransferStatus.TRANSFER_FINISHED, transfer.transferStatus)
        } finally {
            file.delete()
        }
    }

    @Test
    fun daemonInfoUpdatesOngoingTransferWithoutChangingStatus() {
        val transfer = newTransfer().apply {
            transferStatus = TransferStatus.TRANSFER_ONGOING
            bytesProgress = 8
        }

        assertTrue(transfer.applyDaemonInfo(File("updated"), 100, 10))
        assertEquals(TransferStatus.TRANSFER_ONGOING, transfer.transferStatus)
        assertEquals(10, transfer.bytesProgress)
        assertEquals(100, transfer.totalSize)
        assertEquals(File("updated"), transfer.daemonPath)
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

    @Test
    fun interruptedTransferCanResumeAndFinish() {
        val transfer = newTransfer().apply {
            transferStatus = TransferStatus.TRANSFER_UNJOINABLE_PEER
        }

        assertTrue(transfer.canTransitionTo(TransferStatus.TRANSFER_ONGOING))
        assertTrue(transfer.canTransitionTo(TransferStatus.TRANSFER_FINISHED))
    }

    @Test
    fun removedTransferCannotResume() {
        val transfer = newTransfer().apply {
            transferStatus = TransferStatus.FILE_REMOVED
        }

        assertFalse(transfer.canTransitionTo(TransferStatus.TRANSFER_ONGOING))
    }

    @Test
    fun invalidDaemonEventIsAnError() {
        assertEquals(TransferStatus.TRANSFER_ERROR, TransferStatus.fromIntFile(0))
    }

    @Test
    fun publicPathOnlyExposesExistingSymlinkTarget() {
        val directory = Files.createTempDirectory("jami-transfer").toFile()
        val target = File(directory, "target").apply { writeText("data") }
        val link = File(directory, "link")
        val realParent = File(directory, "real-parent").apply { mkdir() }
        val parentLink = File(directory, "parent-link")
        Files.createSymbolicLink(parentLink.toPath(), realParent.toPath())
        val regularThroughParentLink = File(parentLink, "regular").apply { writeText("data") }
        Files.createSymbolicLink(link.toPath(), target.toPath())
        val transfer = newTransfer().apply { daemonPath = link }

        try {
            assertEquals(target.canonicalFile, transfer.publicPath)
            transfer.daemonPath = target
            assertEquals(null, transfer.publicPath)
            transfer.daemonPath = regularThroughParentLink
            assertEquals(null, transfer.publicPath)
        } finally {
            link.delete()
            regularThroughParentLink.delete()
            parentLink.delete()
            realParent.delete()
            target.delete()
            directory.delete()
        }
    }

    @Test
    fun clearingDaemonInfoDropsPathAndProgress() {
        val transfer = newTransfer().apply {
            daemonPath = File("index")
            destination = File("destination")
            bytesProgress = 10
        }

        transfer.clearDaemonInfo()

        assertEquals(null, transfer.daemonPath)
        assertEquals(null, transfer.destination)
        assertEquals(0, transfer.bytesProgress)
    }

    @Test
    fun exactContentRequiresAnExistingRecordedFile() {
        val file = File.createTempFile("jami-transfer", ".tmp")
        val transfer = newTransfer().apply { destination = file }

        try {
            assertTrue(transfer.hasExactContent)
            file.delete()
            assertFalse(transfer.hasExactContent)
        } finally {
            file.delete()
        }
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