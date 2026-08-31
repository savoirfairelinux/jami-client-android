package net.jami.services

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileTransferInfoTest {
    @Test
    fun successIsAppliedWithoutRetry() {
        val info = FileTransferInfo("path", 10, 10, 0)

        assertTrue(info.isSuccess)
        assertFalse(info.isRetryable)
    }

    @Test
    fun unknownIsTheOnlyRetryableResult() {
        assertTrue(FileTransferInfo(null, 0, 0, 1).isRetryable)
        assertFalse(FileTransferInfo(null, 0, 0, 2).isRetryable)
        assertFalse(FileTransferInfo(null, 0, 0, 3).isRetryable)
    }
}