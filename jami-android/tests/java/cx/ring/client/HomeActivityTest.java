package cx.ring.client;

import android.test.ActivityInstrumentationTestCase2;
import com.robotium.solo.Solo;
import junit.framework.Assert;
import cx.ring.fragments.AboutFragment;
import cx.ring.fragments.AccountsManagementFragment;
import cx.ring.fragments.HomeFragment;

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
    Solo mSolo;

    public HomeActivityTest() {
        super(HomeActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mSolo = new Solo(getInstrumentation(), getActivity());
    }

    public void testService() throws Exception {
        Assert.assertTrue(mActivity.getService() != null);
        Assert.assertTrue(mActivity.getService().getRecordPath() != null);
    }

    public void testSections() throws Exception {
        mActivity.onSectionSelected(0);
        Assert.assertTrue(mActivity.fContent instanceof HomeFragment);
        String homeScreenTitle = String.format("%s.%s", getClass().getSimpleName(), getName());
        mSolo.takeScreenshot(homeScreenTitle);
        mActivity.onSectionSelected(1);
        Assert.assertTrue(mActivity.fContent instanceof AccountsManagementFragment);
        mSolo.sleep(500);
        String accountsScreenTitle = String.format("%s.%s", ((Object)mActivity.fContent).getClass().getSimpleName(), getName());
        mSolo.takeScreenshot(accountsScreenTitle);
        mActivity.onSectionSelected(2);
        Assert.assertTrue(mActivity.fContent instanceof AboutFragment);
        mSolo.sleep(500);
        String aboutScreenTitle = String.format("%s.%s", ((Object)mActivity.fContent).getClass().getSimpleName(), getName());
        mSolo.takeScreenshot(aboutScreenTitle);
    }

}
