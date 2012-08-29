package com.savoirfairelinux.sflphone;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.savoirfairelinux.sflphone.client.SFLPhoneHomeTest \
 * com.savoirfairelinux.sflphone.tests/android.test.InstrumentationTestRunner
 */
public class SFLPhoneHomeTest extends ActivityInstrumentationTestCase2<SFLPhoneHome> {

    public SFLPhoneHomeTest() {
        super("com.savoirfairelinux.sflphone", SFLPhoneHome.class);
    }

}
