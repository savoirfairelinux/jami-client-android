package cx.ring.navigation;

import java.io.File;

import cx.ring.model.Account;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;

/**
 * Created by abonnet on 16-12-02.
 */

public class RingNavigationViewModel {

    private Account mAccount;

    public RingNavigationViewModel(Account account){
        mAccount = account;
    }

    public VCard getVcard(File filesDir, String defaultName){
        return VCardUtils.loadLocalProfileFromDisk(filesDir, mAccount.getAccountID(), defaultName);
    }

    public Account getAccount(){
        return mAccount;
    }
}
