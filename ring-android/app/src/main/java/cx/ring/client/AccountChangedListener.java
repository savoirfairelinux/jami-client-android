package cx.ring.client;

import cx.ring.model.account.Account;

public interface AccountChangedListener {
    void accountChanged(Account acc);
    void accountUpdated(Account acc);
}
