package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\r\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0002\b\t\n\u0002\u0010\b\n\u0002\b\u000e\u0018\u0000 82\b\u0012\u0004\u0012\u00020\u00020\u0001:\u00018B\u0019\b\u0007\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\b\b\u0001\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007J\u0010\u0010 \u001a\u00020!2\u0006\u0010\"\u001a\u00020\u0002H\u0016J\b\u0010#\u001a\u00020!H\u0002J\u0006\u0010$\u001a\u00020!J\u0010\u0010%\u001a\u00020!2\b\u0010&\u001a\u0004\u0018\u00010\u0014J\"\u0010\'\u001a\u00020!2\u0006\u0010(\u001a\u00020\n2\b\u0010)\u001a\u0004\u0018\u00010\n2\u0006\u0010*\u001a\u00020+H\u0002J\u000e\u0010,\u001a\u00020!2\u0006\u0010-\u001a\u00020\nJ\u0016\u0010,\u001a\u00020!2\u0006\u0010-\u001a\u00020\n2\u0006\u0010.\u001a\u00020\u001aJ\u000e\u0010/\u001a\u00020!2\u0006\u00100\u001a\u00020\nJ\u0006\u00101\u001a\u00020!J\u000e\u00102\u001a\u00020!2\u0006\u00103\u001a\u00020\u000eJ\u000e\u00104\u001a\u00020!2\u0006\u00105\u001a\u00020\u000eJ\u000e\u00106\u001a\u00020!2\u0006\u00107\u001a\u00020\nR<\u0010\b\u001a0\u0012\f\u0012\n \u000b*\u0004\u0018\u00010\n0\n \u000b*\u0017\u0012\f\u0012\n \u000b*\u0004\u0018\u00010\n0\n\u0018\u00010\t\u00a2\u0006\u0002\b\f0\t\u00a2\u0006\u0002\b\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000f\u001a\u00020\u000e8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u000f\u0010\u0010R\u000e\u0010\u0011\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0013\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0003\u001a\u00020\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0015\u0010\u0016\"\u0004\b\u0017\u0010\u0018R\u000e\u0010\u0019\u001a\u00020\u001aX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0005\u001a\u00020\u0006X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001b\u0010\u001c\"\u0004\b\u001d\u0010\u001eR\u000e\u0010\u001f\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u00069"}, d2 = {"Lnet/jami/account/JamiAccountCreationPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/account/JamiAccountCreationView;", "mAccountService", "Lnet/jami/services/AccountService;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Lnet/jami/services/AccountService;Lio/reactivex/rxjava3/core/Scheduler;)V", "contactQuery", "Lio/reactivex/rxjava3/subjects/PublishSubject;", "", "kotlin.jvm.PlatformType", "Lio/reactivex/rxjava3/annotations/NonNull;", "isConfirmCorrect", "", "isInputValid", "()Z", "isPasswordCorrect", "isUsernameCorrect", "mAccountCreationModel", "Lnet/jami/model/AccountCreationModel;", "getMAccountService", "()Lnet/jami/services/AccountService;", "setMAccountService", "(Lnet/jami/services/AccountService;)V", "mPasswordConfirm", "", "getMUiScheduler", "()Lio/reactivex/rxjava3/core/Scheduler;", "setMUiScheduler", "(Lio/reactivex/rxjava3/core/Scheduler;)V", "showLoadingAnimation", "bindView", "", "view", "checkForms", "createAccount", "init", "accountCreationModel", "onLookupResult", "name", "address", "state", "", "passwordChanged", "password", "repeat", "passwordConfirmChanged", "passwordConfirm", "passwordUnset", "registerUsernameChanged", "isChecked", "setPush", "push", "userNameChanged", "userName", "Companion", "libringclient"})
public final class JamiAccountCreationPresenter extends net.jami.mvp.RootPresenter<net.jami.account.JamiAccountCreationView> {
    @org.jetbrains.annotations.NotNull()
    private net.jami.services.AccountService mAccountService;
    @org.jetbrains.annotations.NotNull()
    private io.reactivex.rxjava3.core.Scheduler mUiScheduler;
    private final io.reactivex.rxjava3.subjects.PublishSubject<java.lang.String> contactQuery = null;
    private net.jami.model.AccountCreationModel mAccountCreationModel;
    private boolean isUsernameCorrect = false;
    private boolean isPasswordCorrect = true;
    private boolean isConfirmCorrect = true;
    private boolean showLoadingAnimation = true;
    private java.lang.CharSequence mPasswordConfirm = "";
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.account.JamiAccountCreationPresenter.Companion Companion = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = null;
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final long TYPING_DELAY = 350L;
    
    @javax.inject.Inject()
    public JamiAccountCreationPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "UiScheduler")
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.services.AccountService getMAccountService() {
        return null;
    }
    
    public final void setMAccountService(@org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Scheduler getMUiScheduler() {
        return null;
    }
    
    public final void setMUiScheduler(@org.jetbrains.annotations.NotNull()
    io.reactivex.rxjava3.core.Scheduler p0) {
    }
    
    @java.lang.Override()
    public void bindView(@org.jetbrains.annotations.NotNull()
    net.jami.account.JamiAccountCreationView view) {
    }
    
    public final void init(@org.jetbrains.annotations.Nullable()
    net.jami.model.AccountCreationModel accountCreationModel) {
    }
    
    /**
     * Called everytime the provided username for the new account changes
     * Sends the new value of the username to the ContactQuery subjet and shows the loading
     * animation if it has not been started before
     */
    public final void userNameChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String userName) {
    }
    
    public final void registerUsernameChanged(boolean isChecked) {
    }
    
    public final void passwordUnset() {
    }
    
    public final void passwordChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    java.lang.CharSequence repeat) {
    }
    
    public final void passwordChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String password) {
    }
    
    public final void passwordConfirmChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String passwordConfirm) {
    }
    
    public final void createAccount() {
    }
    
    private final boolean isInputValid() {
        return false;
    }
    
    private final void checkForms() {
    }
    
    private final void onLookupResult(java.lang.String name, java.lang.String address, int state) {
    }
    
    public final void setPush(boolean push) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\t\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u000e\u0010\t\u001a\u00020\nX\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lnet/jami/account/JamiAccountCreationPresenter$Companion;", "", "()V", "PASSWORD_MIN_LENGTH", "", "TAG", "", "getTAG", "()Ljava/lang/String;", "TYPING_DELAY", "", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTAG() {
            return null;
        }
    }
}