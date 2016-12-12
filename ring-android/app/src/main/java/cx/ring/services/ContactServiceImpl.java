package cx.ring.services;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import cx.ring.model.CallContact;

/**
 * Created by twittemberg on 16-12-12.
 */

public class ContactServiceImpl extends ContactService {

    static private final String SELECT = "((" + ContactsContract.Contacts.DISPLAY_NAME + " NOTNULL) AND (" + ContactsContract.Contacts.DISPLAY_NAME + " != '' ))";

    @Inject
    Context mContext;

    @Override
    public List<CallContact> loadSystemContacts() {

        ContentResolver contentResolver = mContext.getContentResolver();


        return new ArrayList<>();
    }

}
