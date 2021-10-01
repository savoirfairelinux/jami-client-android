package net.jami.settings;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\r\n\u0000\bf\u0018\u00002\u00020\u0001J\u001e\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007H&\u00a8\u0006\t"}, d2 = {"Lnet/jami/settings/AdvancedAccountView;", "", "initView", "", "config", "Lnet/jami/model/AccountConfig;", "networkInterfaces", "Ljava/util/ArrayList;", "", "libringclient"})
public abstract interface AdvancedAccountView {
    
    public abstract void initView(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountConfig config, @org.jetbrains.annotations.NotNull()
    java.util.ArrayList<java.lang.CharSequence> networkInterfaces);
}