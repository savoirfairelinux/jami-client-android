/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.account

import net.jami.model.Account
import net.jami.model.Profile
import java.io.File

interface JamiAccountSummaryView {
    fun showExportingProgressDialog()
    fun showPasswordProgressDialog()
    fun accountChanged(account: Account, profile: Profile)
    fun showNetworkError()
    fun showPasswordError()
    fun showGenericError()
    fun showPIN(pin: String)
    fun passwordChangeEnded(ok: Boolean)
    fun displayCompleteArchive(dest: File)
    fun gotToImageCapture()
    fun askCameraPermission()
    fun goToGallery()
    fun askGalleryPermission()
    fun updateUserView(account: Account, profile: Profile)
    fun goToMedia(accountId: String)
    fun goToSystem(accountId: String)
    fun goToAdvanced(accountId: String)
    fun goToAccount(accountId: String)
    fun setSwitchStatus(account: Account)
    fun showRevokingProgressDialog()
    fun deviceRevocationEnded(device: String, status: Int)
    fun updateDeviceList(devices: Map<String, String>, currentDeviceId: String)
}