/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
package net.jami.services

import net.jami.services.DaemonService.SystemInfoCallbacks
import net.jami.model.DataTransfer
import java.io.File

abstract class DeviceRuntimeService : SystemInfoCallbacks {
    abstract fun loadNativeLibrary()
    abstract fun provideFilesDir(): File
    abstract val cacheDir: File
    abstract fun getFilePath(name: String): File
    abstract fun getConversationPath(accountId: String, conversationId: String, name: String): File
    fun getConversationPath(interaction: DataTransfer): File? =
        if (interaction.conversationId == null)
            getConversationPath(interaction.account!!, interaction.conversation!!.participant!!, interaction.storagePath)
        else interaction.publicPath

    fun getNewConversationPath(accountId: String, conversationId: String, name: String): File {
        var prefix = 0
        var destPath: File
        do {
            val fileName = if (prefix == 0) name else "${prefix}_$name"
            destPath = getConversationPath(accountId, conversationId, fileName)
            prefix++
        } while (destPath.exists())
        return destPath
    }

    abstract fun getTemporaryPath(conversationId: String, name: String): File
    abstract fun getConversationDir(conversationId: String): File?
    abstract val pushToken: String
    abstract val pushPlatform: String

    abstract val isConnectedMobile: Boolean
    abstract val isConnectedEthernet: Boolean
    abstract val isConnectedWifi: Boolean
    abstract val isConnectedBluetooth: Boolean
    abstract fun hasVideoPermission(): Boolean
    abstract fun hasAudioPermission(): Boolean
    abstract fun hasContactPermission(): Boolean
    abstract fun hasCallLogPermission(): Boolean
    abstract fun hasGalleryPermission(): Boolean
    abstract val profileName: String?
    abstract fun hardLinkOrCopy(source: File, dest: File): Boolean
}
