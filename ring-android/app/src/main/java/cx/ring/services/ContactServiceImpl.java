package cx.ring.services;

import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

import cx.ring.model.CallContact;

/**
 * Created by twittemberg on 16-12-12.
 */

public class ContactServiceImpl extends ContactService {

    static private final String SELECT = "((" + ContactsContract.Contacts.DISPLAY_NAME + " NOTNULL) AND (" + ContactsContract.Contacts.DISPLAY_NAME + " != '' ))";

    @Override
    public List<CallContact> loadSystemContacts() {
        return new ArrayList<>();
    }

}
