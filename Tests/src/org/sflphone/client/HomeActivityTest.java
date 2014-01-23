package org.sflphone.client;

import android.test.ActivityInstrumentationTestCase2;
import junit.framework.Assert;
import org.sflphone.R;
import org.sflphone.fragments.AboutFragment;
import org.sflphone.fragments.AccountsManagementFragment;
import org.sflphone.fragments.HomeFragment;
import org.sflphone.model.Account;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class org.sflphone.client.HomeActivityTest \
 * org.sflphone.tests/android.test.InstrumentationTestRunner
 */
public class HomeActivityTest extends ActivityInstrumentationTestCase2<HomeActivity> {

    HomeActivity mActivity;

    public HomeActivityTest() {
        super(HomeActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testService() throws Exception {
        Assert.assertTrue(mActivity.getService() != null);
        Assert.assertTrue(mActivity.getService().getRecordPath() != null);
    }

    public void testSections() throws Exception {
        mActivity.onSectionSelected(0);
        Assert.assertTrue(mActivity.fContent instanceof HomeFragment);
        mActivity.onSectionSelected(1);
        Assert.assertTrue(mActivity.fContent instanceof AccountsManagementFragment);
        mActivity.onSectionSelected(2);
        Assert.assertTrue(mActivity.fContent instanceof AboutFragment);
    }

}
