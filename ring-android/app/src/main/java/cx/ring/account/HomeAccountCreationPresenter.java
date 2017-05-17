package cx.ring.account;

import javax.inject.Inject;

import cx.ring.mvp.RootPresenter;

/**
 * Created by hdsousa on 17-05-17.
 */

public class HomeAccountCreationPresenter extends RootPresenter<HomeAccountCreationView> {

    @Inject
    public HomeAccountCreationPresenter() {
    }

    public void clickOnCreateAccount() {
        getView().goToAccountCreation();
    }

    public void clickOnLinkAccount() {
        getView().goToAccountLink();
    }

    @Override
    public void afterInjection() {

    }
}
