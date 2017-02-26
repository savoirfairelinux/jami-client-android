package cx.ring.model;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UriTest {

    @Test
    public void testGoodRawString() {
        String uri = "ring:1234567890123456789012345678901234567890";
        Uri test = new Uri(uri);
        assertTrue(test.getRawUriString().contentEquals(uri));
    }

    @Test
    public void testBadIPAddress() {
        assertFalse(Uri.isIpAddress("not an ip"));
    }

    @Test
    public void testGoodIPAddress() {
        assertTrue(Uri.isIpAddress("127.0.0.1"));
        assertTrue(Uri.isIpAddress("2001:db8:0:85a3:0:0:ac1f:8001"));
    }

    @Test
    public void testRingModel() {
        String uri = "ring:1234567890123456789012345678901234567890";
        Uri test = new Uri(uri);

        assertTrue(test.getDisplayName() == null);
        assertTrue(test.getScheme().contentEquals("ring:"));
        assertTrue(test.getUriString().contentEquals("ring:1234567890123456789012345678901234567890"));
    }

    @Test
    public void testSIPModel() {
        String uri = "100@sipuri";
        Uri test = new Uri(uri);

        assertTrue(test.getUsername().contentEquals("100"));
        assertTrue(test.getHost().contentEquals("sipuri"));
    }
}
