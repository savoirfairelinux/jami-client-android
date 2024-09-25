/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
package net.jami.qrcode

import androidx.annotation.IntDef
import net.jami.mvp.RootPresenter

interface QRCodeView

class QRCodePresenter : RootPresenter<QRCodeView>() {

    companion object {
        private val TAG = this::class.java.simpleName

        const val MODE_SCAN = 1
        const val MODE_SHARE = 2

        @IntDef(flag = true, value = [MODE_SCAN, MODE_SHARE])
        @Retention(AnnotationRetention.SOURCE)
        annotation class QRCodeMode
    }
}