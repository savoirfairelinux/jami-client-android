package org.sflphone.client;
/**
 * Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 * Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this program, or any covered work, by linking or
 * combining it with the OpenSSL project's OpenSSL library (or a
 * modified version of that library), containing parts covered by the
 * terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 * grants you additional permission to convey the resulting work.
 * Corresponding Source for a non-source form of such a combination
 * shall include the source code for the parts of OpenSSL used as well
 * as that of the covered work.
 */


        import java.lang.Thread;
        import android.test.AndroidTestCase;
        import org.sflphone.service.*;


public class ManagerImplTest extends AndroidTestCase {
    public static final String ACCOUNT_ID = "IP2IP";
    public static final String CALLING_VALID_IP = "127.0.0.1";
    public static final String CALLING_BAD_IP = "123.234.123.234";
    public static final String CALL_ID = "1234567";
    public static final int CALL_SLEEP_TIME = 1000; // in ms

    static {
        System.loadLibrary("gnustl_shared");
        System.loadLibrary("crypto");
        System.loadLibrary("ssl");
        System.loadLibrary("sflphone");
    }

    ManagerImpl managerImpl;
    private CallManager callManagerJNI;
    private ConfigurationManager configurationManagerJNI;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        managerImpl = SFLPhoneservice.instance();
        /* set static AppPath before calling manager.init */
        // managerImpl.setPath(getApplication().getFilesDir().getAbsolutePath());
        callManagerJNI = new CallManager();
        configurationManagerJNI = new ConfigurationManager();
        /*managerImpl.init("");*/

        assertTrue(managerImpl != null);
        assertTrue(callManagerJNI != null);
        assertTrue(configurationManagerJNI != null);
    }

/*    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }*/


/*    public void testPlaceCallSuccessful() {
        try {
            callManagerJNI.placeCall(ACCOUNT_ID, CALL_ID, CALLING_VALID_IP);
            assertTrue(true);

            // FIXME: We should have a method returning the call state
            //        and wait for the call to be in state CURRENT.
            Thread.sleep(CALL_SLEEP_TIME);

            callManagerJNI.hangUp(CALL_ID);
            assertTrue(true);

        } catch (InterruptedException e) {
            assertTrue(false);
        }
    }*/

    /*public void testPlaceCallBad() {
        try {
            callManagerJNI.placeCall(ACCOUNT_ID, CALL_ID, CALLING_BAD_IP);
            assertTrue(true);

            Thread.sleep(CALL_SLEEP_TIME);

            callManagerJNI.hangUp(CALL_ID);
            assertTrue(true);

        } catch (InterruptedException e) {
            assertTrue(false);
        }
    }*/
}
