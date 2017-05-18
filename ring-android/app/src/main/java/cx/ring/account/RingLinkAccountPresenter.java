package cx.ring.account;

import javax.inject.Inject;

import cx.ring.mvp.RootPresenter;

/**
 * Created by hdsousa on 17-05-18.
 */

public class RingLinkAccountPresenter extends RootPresenter<RingLinkAccountView> {

    private String mPassword = "";
    private String mPin = "";

    @Inject
    public RingLinkAccountPresenter() {
    }

    void passwordChanged(String password) {
        mPassword = password;
        checkForms();
    }

    void pinChanged(String pin) {
        mPin = pin;
        checkForms();
    }

    void lastClicked() {
        getView().goToLast();
    }

    void linkClicked() {
        getView().createAccount();
    }

    private void checkForms() {
        getView().enableLinkButton(!mPin.isEmpty() && !mPassword.isEmpty());
    }

    @Override
    public void afterInjection() {

    }
}
