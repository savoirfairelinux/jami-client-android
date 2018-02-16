package cx.ring.utils;

import org.junit.Test;

import ezvcard.VCard;
import ezvcard.property.FormattedName;

import static org.junit.Assert.assertTrue;

public class VCardUtilsTest {

    @Test
    public void testVCardToString() {
        VCard vcard = new VCard();
        vcard.addFormattedName(new FormattedName("SFL Test"));

        String result = VCardUtils.vcardToString(vcard);
        assertTrue(result.contentEquals("BEGIN:VCARD\r\n" +
                "VERSION:2.1\r\n" +
                "X-PRODID:ez-vcard 0.10.2\r\n" +
                "FN:SFL Test\r\n" +
                "END:VCARD\r\n"));
    }
}
