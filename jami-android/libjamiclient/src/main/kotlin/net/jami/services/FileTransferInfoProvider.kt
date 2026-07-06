package net.jami.services

import net.jami.daemon.JamiService

data class FileTransferInfo(
    val path: String?,
    val total: Long,
    val progress: Long
)

fun interface FileTransferInfoProvider {
    fun get(accountId: String, conversationId: String, fileId: String): FileTransferInfo
}

object JamiFileTransferInfoProvider : FileTransferInfoProvider {
    override fun get(accountId: String, conversationId: String, fileId: String): FileTransferInfo {
        val paths = arrayOfNulls<String>(1)
        val totals = LongArray(1)
        val progress = LongArray(1)
        JamiService.fileTransferInfo(accountId, conversationId, fileId, paths, totals, progress)
        return FileTransferInfo(paths[0], totals[0], progress[0])
    }
}