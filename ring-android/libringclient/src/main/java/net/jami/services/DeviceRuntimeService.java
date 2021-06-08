/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.services;

import net.jami.model.Conversation;
import net.jami.model.DataTransfer;

import java.io.File;

public abstract class DeviceRuntimeService implements DaemonService.SystemInfoCallbacks {

    public abstract void loadNativeLibrary();

    public abstract File provideFilesDir();
    public abstract File getCacheDir();

    public abstract File getFilePath(String name);
    public abstract File getConversationPath(String conversationId, String name);
    public abstract File getConversationPath(String accountId, String conversationId, String name);

    public File getConversationPath(DataTransfer interaction) {
        return interaction.getConversationId() == null
                ? getConversationPath(interaction.getConversation().getParticipant(), interaction.getStoragePath())
                : interaction.getPublicPath();
    }
    public File getNewConversationPath(String accountId, String conversationId, String name) {
        int prefix = 0;
        File destPath;
        do {
            String fileName = prefix == 0 ? name : prefix + '_' + name;
            destPath = getConversationPath(accountId, conversationId, fileName);
            prefix++;
        } while (destPath.exists());
        return destPath;
    }

    public abstract File getTemporaryPath(String conversationId, String name);
    public abstract File getConversationDir(String conversationId);

    public abstract String getPushToken();

    public abstract boolean isConnectedMobile();

    public abstract boolean isConnectedEthernet();

    public abstract boolean isConnectedWifi();

    public abstract boolean isConnectedBluetooth();

    public abstract boolean hasVideoPermission();

    public abstract boolean hasAudioPermission();

    public abstract boolean hasContactPermission();

    public abstract boolean hasCallLogPermission();

    public abstract boolean hasGalleryPermission();

    public abstract boolean hasWriteExternalStoragePermission();

    public abstract String getProfileName();

    public abstract boolean hardLinkOrCopy(File source, File dest);
}
