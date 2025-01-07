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
package net.jami.scan

import net.jami.mvp.RootPresenter
import net.jami.services.HardwareService
import javax.inject.Inject

interface ScanView {
    fun moveToConversation(conversation: String)
}

class ScanPresenter @Inject constructor(
    private val mHardwareService: HardwareService,
) : RootPresenter<ScanView>() {

    fun onBarcodeScanned(barcodeResult: String) {
        view?.moveToConversation(barcodeResult)
    }

    fun cameraPermissionChanged(isGranted: Boolean) {
        if (isGranted && mHardwareService.isVideoAvailable) {
            mHardwareService.initVideo()
                .onErrorComplete()
                .blockingAwait()
        }
    }
}