/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

package cx.ring.account;

import java.io.File;
import java.util.Map;

import cx.ring.model.Account;

public interface RingAccountSummaryView {

    void showExportingProgressDialog();

    void showRevokingProgressDialog();

    void showPasswordProgressDialog();

    void accountChanged(final Account account);

    void showNetworkError();

    void showPasswordError();

    void showGenericError();

    void showPIN(String pin);

    void updateDeviceList(Map<String, String> devices, String currentDeviceId);

    void deviceRevocationEnded(String device, int status);
    void passwordChangeEnded(boolean ok);

    void displayCompleteArchive(File dest);

    void gotToImageCapture();

    void askCameraPermission();

    void goToGallery();

    void askGalleryPermission();
}
