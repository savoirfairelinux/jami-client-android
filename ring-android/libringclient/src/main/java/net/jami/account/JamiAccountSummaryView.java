/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.jami.account;

import java.io.File;
import java.util.Map;

import net.jami.model.Account;

public interface JamiAccountSummaryView {

    void showExportingProgressDialog();

    void showPasswordProgressDialog();

    void accountChanged(final Account account);

    void showNetworkError();

    void showPasswordError();

    void showGenericError();

    void showPIN(String pin);

    void passwordChangeEnded(boolean ok);

    void displayCompleteArchive(File dest);

    void gotToImageCapture();

    void askCameraPermission();

    void goToGallery();

    void askGalleryPermission();

    void updateUserView(Account account);

    void goToMedia(String accountId);

    void goToSystem(String accountId);

    void goToAdvanced(String accountId);

    void goToAccount(String accountId);

    void setSwitchStatus(Account account);

    void showRevokingProgressDialog();

    void deviceRevocationEnded(String device, int status);

    void updateDeviceList(Map<String, String> devices, String currentDeviceId);

}
