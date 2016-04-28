/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.client;

import android.app.Fragment;

import com.google.zxing.integration.android.IntentIntegrator;
import com.journeyapps.barcodescanner.CaptureActivity;

import cx.ring.R;

public class QRCodeScannerActivity extends CaptureActivity {

    /**
     * Starts a QR Code scanner and passes the receiver to the engine to notify it with the result.
     *
     * @param receiver the Fragment to notify with the result of the scan.
     */
    public static void startQRCodeScanWithFragmentReceiver(Fragment receiver) {
        if (null != receiver) {
            IntentIntegrator integrator = IntentIntegrator.forFragment(receiver);
            configureIntentIntegrator(integrator,
                    receiver.getString(R.string.scan_qr_account_message));
        }
    }

    /**
     * Configures the QR Code scanner with invariable parameters
     *
     * @param intentIntegrator the IntentIntegrator to configure
     * @param promptString     the text to display in the prompt of the QR Code scanner overlay
     */
    private static void configureIntentIntegrator(IntentIntegrator intentIntegrator,
                                                  String promptString) {
        if (null == intentIntegrator) {
            return;
        }
        intentIntegrator.setPrompt(promptString);
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        intentIntegrator.setCaptureActivity(QRCodeScannerActivity.class);
        intentIntegrator.setBarcodeImageEnabled(true);
        intentIntegrator.setOrientationLocked(false);
        intentIntegrator.initiateScan();
    }
}
