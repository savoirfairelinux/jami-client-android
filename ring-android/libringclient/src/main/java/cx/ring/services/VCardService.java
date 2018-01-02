package cx.ring.services;

import ezvcard.VCard;

/**
 * Created by hdesousa on 02/01/18.
 */

public abstract class VCardService {

    public abstract VCard loadSmallVCard(String accountId);

}
