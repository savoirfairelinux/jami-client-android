package net.jami.settings;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&J\u0010\u0010\u0006\u001a\u00020\u00032\u0006\u0010\u0007\u001a\u00020\bH&J\b\u0010\t\u001a\u00020\u0003H&J\b\u0010\n\u001a\u00020\u0003H&J*\u0010\u000b\u001a\u00020\u00032\u0018\u0010\f\u001a\u0014\u0012\u0006\u0012\u0004\u0018\u00010\u000e\u0012\u0006\u0012\u0004\u0018\u00010\u000e\u0018\u00010\r2\u0006\u0010\u000f\u001a\u00020\u000eH&\u00a8\u0006\u0010"}, d2 = {"Lnet/jami/settings/GeneralAccountView;", "", "accountChanged", "", "account", "Lnet/jami/model/Account;", "addJamiPreferences", "accountId", "", "addSipPreferences", "finish", "updateResolutions", "maxResolution", "Lnet/jami/utils/Tuple;", "", "currentResolution", "libringclient"})
public abstract interface GeneralAccountView {
    
    public abstract void addJamiPreferences(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId);
    
    public abstract void addSipPreferences();
    
    public abstract void accountChanged(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account);
    
    public abstract void finish();
    
    public abstract void updateResolutions(@org.jetbrains.annotations.Nullable()
    net.jami.utils.Tuple<java.lang.Integer, java.lang.Integer> maxResolution, int currentResolution);
}