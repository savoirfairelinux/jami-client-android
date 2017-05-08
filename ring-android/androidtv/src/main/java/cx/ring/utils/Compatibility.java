/*
 *  Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Regis Montoya <r3gis.3R@gmail.com>
 *  Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package cx.ring.utils;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaRecorder.AudioSource;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

@SuppressWarnings("deprecation")
public final class Compatibility {

    private Compatibility() {
    }

    private static final String THIS_FILE = "Compat";

    public static int getApiLevel() {
        return android.os.Build.VERSION.SDK_INT;
    }

    public static boolean isCompatible(int apiLevel) {
        return android.os.Build.VERSION.SDK_INT >= apiLevel;
    }

    /**
     * Get the stream id for in call track. Can differ on some devices. Current device for which it's different :
     * 
     * @return
     */
    public static int getInCallStream(boolean requestBluetooth) {
        /* Archos 5IT */
        if (android.os.Build.BRAND.equalsIgnoreCase("archos") && android.os.Build.DEVICE.equalsIgnoreCase("g7a")) {
            // Since archos has no voice call capabilities, voice call stream is
            // not implemented
            // So we have to choose the good stream tag, which is by default
            // falled back to music
            return AudioManager.STREAM_MUSIC;
        }
        if (requestBluetooth) {
            return 6; /* STREAM_BLUETOOTH_SCO -- Thx @Stefan for the contrib */
        }

        // return AudioManager.STREAM_MUSIC;
        return AudioManager.STREAM_VOICE_CALL;
    }

    public static boolean shouldUseRoutingApi() {
        Log.d(THIS_FILE, "Current device " + android.os.Build.BRAND + " - " + android.os.Build.DEVICE);

        // HTC evo 4G
        if (android.os.Build.PRODUCT.equalsIgnoreCase("htc_supersonic")) {
            return true;
        }

        // ZTE joe
        if (android.os.Build.DEVICE.equalsIgnoreCase("joe")) {
            return true;
        }

        // Samsung GT-S5830
        return android.os.Build.DEVICE.toUpperCase().startsWith("GT-S");
    }

    public static boolean shouldUseModeApi() {

        // ZTE blade et joe
        if (android.os.Build.DEVICE.equalsIgnoreCase("blade") || android.os.Build.DEVICE.equalsIgnoreCase("joe")) {
            return true;
        }
        // Samsung GT-S5360 GT-S5830 GT-S6102 ... probably all..
        if (android.os.Build.DEVICE.toUpperCase().startsWith("GT-") || android.os.Build.PRODUCT.toUpperCase().startsWith("GT-")
                || android.os.Build.DEVICE.toUpperCase().startsWith("YP-")) {
            return true;
        }

        // HTC evo 4G
        if (android.os.Build.PRODUCT.equalsIgnoreCase("htc_supersonic")) {
            return true;
        }
        // LG P500, Optimus V
        if (android.os.Build.DEVICE.toLowerCase().startsWith("thunder")) {
            return true;
        }
        // LG-E720(b)
        if (android.os.Build.MODEL.toUpperCase().startsWith("LG-E720") && !Compatibility.isCompatible(9)) {
            return true;
        }
        // LG-LS840
        if (android.os.Build.DEVICE.toLowerCase().startsWith("cayman")) {
            return true;
        }

        // Huawei
        if (android.os.Build.DEVICE.equalsIgnoreCase("U8150") || android.os.Build.DEVICE.equalsIgnoreCase("U8110")
                || android.os.Build.DEVICE.equalsIgnoreCase("U8120") || android.os.Build.DEVICE.equalsIgnoreCase("U8100")
                || android.os.Build.PRODUCT.equalsIgnoreCase("U8655")) {
            return true;
        }

        // Moto defy mini
        if (android.os.Build.MODEL.equalsIgnoreCase("XT320")) {
            return true;
        }

        // Alcatel
        if (android.os.Build.DEVICE.toUpperCase().startsWith("ONE_TOUCH_993D")) {
            return true;
        }

        // N4
        return android.os.Build.DEVICE.toUpperCase().startsWith("MAKO");

    }

    public static String guessInCallMode() {
        // New api for 2.3.3 is not available on galaxy S II :(
        if (!isCompatible(11) && android.os.Build.DEVICE.toUpperCase().startsWith("GT-I9100")) {
            return Integer.toString(AudioManager.MODE_NORMAL);
        }

        if (android.os.Build.BRAND.equalsIgnoreCase("sdg") || isCompatible(10)) {
            // Note that in APIs this is only available from level 11.
            return "3";
        }
        if (android.os.Build.DEVICE.equalsIgnoreCase("blade")) {
            return Integer.toString(AudioManager.MODE_IN_CALL);
        }

        if (!isCompatible(5)) {
            return Integer.toString(AudioManager.MODE_IN_CALL);
        }

        return Integer.toString(AudioManager.MODE_NORMAL);
    }

    public static String getDefaultMicroSource() {
        // Except for galaxy S II :(
        if (!isCompatible(11) && android.os.Build.DEVICE.toUpperCase().startsWith("GT-I9100")) {
            return Integer.toString(AudioSource.MIC);
        }

        if (isCompatible(10)) {
            // Note that in APIs this is only available from level 11.
            // VOICE_COMMUNICATION
            return Integer.toString(0x7);
        }
        /*
         * Too risky in terms of regressions else if (isCompatible(4)) { // VOICE_CALL return 0x4; }
         */
        /*
         * if(android.os.Build.MODEL.equalsIgnoreCase("X10i")) { // VOICE_CALL return Integer.toString(0x4); }
         */
        /*
         * Not relevant anymore, atrix I tested sounds fine with that if(android.os.Build.DEVICE.equalsIgnoreCase("olympus")) { //Motorola atrix bug
         * // CAMCORDER return Integer.toString(0x5); }
         */

        return Integer.toString(AudioSource.DEFAULT);
    }

    public static String getDefaultFrequency() {
        if (android.os.Build.DEVICE.equalsIgnoreCase("olympus")) {
            // Atrix bug
            return "32000";
        }
        if (android.os.Build.DEVICE.toUpperCase().equals("GT-P1010")) {
            // Galaxy tab see issue 932
            return "32000";
        }

        return isCompatible(4) ? "16000" : "8000";
    }

    public static String getCpuAbi() {
        if (isCompatible(4)) {
            Field field;
            try {
                field = android.os.Build.class.getField("CPU_ABI");
                return field.get(null).toString();
            } catch (Exception e) {
                Log.w(THIS_FILE, "Announce to be android 1.6 but no CPU ABI field", e);
            }

        }
        return "armeabi";
    }

    public final static int getNumCores() {
        // Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                // Check if filename is "cpu", followed by a single digit number
                if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }
        try {
            // Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            // Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            // Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            return Runtime.getRuntime().availableProcessors();
        }
    }

    private static boolean needPspWorkaround() {
        // New api for 2.3 does not work on Incredible S
        if (android.os.Build.DEVICE.equalsIgnoreCase("vivo")) {
            return true;
        }

        // New API for android 2.3 should be able to manage this but do only for
        // honeycomb cause seems not correctly supported by all yet
        if (isCompatible(11)) {
            return false;
        }

        // All htc except....
        if (android.os.Build.PRODUCT.toLowerCase().startsWith("htc") || android.os.Build.BRAND.toLowerCase().startsWith("htc")
                || android.os.Build.PRODUCT.toLowerCase().equalsIgnoreCase("inc") /*
                                                                                   * For Incredible
                                                                                   */
                || android.os.Build.DEVICE.equalsIgnoreCase("passion") /* N1 */) {
            if (android.os.Build.DEVICE.equalsIgnoreCase("hero") /* HTC HERO */
                    || android.os.Build.DEVICE.equalsIgnoreCase("magic") /*
                                                                          * Magic Aka Dev G2
                                                                          */
                    || android.os.Build.DEVICE.equalsIgnoreCase("tatoo") /* Tatoo */
                    || android.os.Build.DEVICE.equalsIgnoreCase("dream") /*
                                                                          * Dream Aka Dev G1
                                                                          */
                    || android.os.Build.DEVICE.equalsIgnoreCase("legend") /* Legend */

            ) {
                return false;
            }

            // Older than 2.3 has no chance to have the new full perf wifi mode
            // working since does not exists
            if (!isCompatible(9)) {
                return true;
            } else {
                // N1 is fine with that
                if (android.os.Build.DEVICE.equalsIgnoreCase("passion")) {
                    return false;
                }
                return true;
            }

        }
        // Dell streak
        if (android.os.Build.BRAND.toLowerCase().startsWith("dell") && android.os.Build.DEVICE.equalsIgnoreCase("streak")) {
            return true;
        }
        // Motorola milestone 1 and 2 & motorola droid & defy not under 2.3
        if ((android.os.Build.DEVICE.toLowerCase().contains("milestone2") || android.os.Build.BOARD.toLowerCase().contains("sholes")
                || android.os.Build.PRODUCT.toLowerCase().contains("sholes") || android.os.Build.DEVICE.equalsIgnoreCase("olympus") || android.os.Build.DEVICE
                .toLowerCase().contains("umts_jordan")) && !isCompatible(9)) {
            return true;
        }
        // Moto defy mini
        if (android.os.Build.MODEL.equalsIgnoreCase("XT320")) {
            return true;
        }

        // Alcatel ONE touch
        if (android.os.Build.DEVICE.startsWith("one_touch_990")) {
            return true;
        }

        return false;
    }

    private static boolean needToneWorkaround() {
        if (android.os.Build.PRODUCT.toLowerCase().startsWith("gt-i5800") || android.os.Build.PRODUCT.toLowerCase().startsWith("gt-i5801")
                || android.os.Build.PRODUCT.toLowerCase().startsWith("gt-i9003")) {
            return true;
        }
        return false;
    }

    private static boolean needSGSWorkaround() {
        if (isCompatible(9)) {
            return false;
        }
        if (android.os.Build.DEVICE.toUpperCase().startsWith("GT-I9000") || android.os.Build.DEVICE.toUpperCase().startsWith("GT-P1000")) {
            return true;
        }
        return false;
    }

    private static boolean needWebRTCImplementation() {
        if (android.os.Build.DEVICE.toLowerCase().contains("droid2")) {
            return true;
        }
        if (android.os.Build.MODEL.toLowerCase().contains("droid bionic")) {
            return true;
        }
        if (android.os.Build.DEVICE.toLowerCase().contains("sunfire")) {
            return true;
        }
        // Huawei Y300
        if (android.os.Build.DEVICE.equalsIgnoreCase("U8833")) {
            return true;
        }
        return false;
    }

    public static boolean shouldSetupAudioBeforeInit() {
        // Setup for GT / GS samsung devices.
        if (android.os.Build.DEVICE.toLowerCase().startsWith("gt-") || android.os.Build.PRODUCT.toLowerCase().startsWith("gt-")) {
            return true;
        }
        return false;
    }

    private static boolean shouldFocusAudio() {
        /* HTC One X */
        if (android.os.Build.DEVICE.toLowerCase().startsWith("endeavoru") || android.os.Build.DEVICE.toLowerCase().startsWith("evita")) {
            return false;
        }

        if (android.os.Build.DEVICE.toUpperCase().startsWith("GT-P7510") && isCompatible(15)) {
            return false;
        }
        return true;
    }

    public static boolean isTabletScreen(Context ctxt) {
        boolean isTablet = false;
        if (!isCompatible(4)) {
            return false;
        }
        Configuration cfg = ctxt.getResources().getConfiguration();
        int screenLayoutVal = 0;
        try {
            Field f = Configuration.class.getDeclaredField("screenLayout");
            screenLayoutVal = (Integer) f.get(cfg);
        } catch (Exception e) {
            return false;
        }
        int screenLayout = (screenLayoutVal & 0xF);
        // 0xF = SCREENLAYOUT_SIZE_MASK but avoid 1.5 incompat doing that
        if (screenLayout == 0x3 || screenLayout == 0x4) {
            // 0x3 = SCREENLAYOUT_SIZE_LARGE but avoid 1.5 incompat doing that
            // 0x4 = SCREENLAYOUT_SIZE_XLARGE but avoid 1.5 incompat doing that
            isTablet = true;
        }

        return isTablet;
    }

}