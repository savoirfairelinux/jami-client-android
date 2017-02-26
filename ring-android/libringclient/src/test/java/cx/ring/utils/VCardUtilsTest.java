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
        assertTrue(result.contentEquals("BEGIN:VCARD\n" +
                "VERSION:2.1\n" +
                "X-PRODID:ez-vcard 0.9.10\n" +
                "FN:SFL Test\n" +
                "END:VCARD\n"));
    }
}
