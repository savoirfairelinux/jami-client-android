package net.jami.utils;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\u0018\u0000 \u00112\u00020\u0001:\u0002\u0011\u0012B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0005R\u0014\u0010\u0007\u001a\b\u0018\u00010\bR\u00020\u0000X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00030\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lnet/jami/utils/NameLookupInputHandler;", "", "accountService", "Lnet/jami/services/AccountService;", "accountId", "", "(Lnet/jami/services/AccountService;Ljava/lang/String;)V", "lastTask", "Lnet/jami/utils/NameLookupInputHandler$NameTask;", "mAccountId", "mAccountService", "Ljava/lang/ref/WeakReference;", "timer", "Ljava/util/Timer;", "enqueueNextLookup", "", "text", "Companion", "NameTask", "libringclient"})
public final class NameLookupInputHandler {
    private final java.lang.ref.WeakReference<net.jami.services.AccountService> mAccountService = null;
    private final java.lang.String mAccountId = null;
    private final java.util.Timer timer = null;
    private net.jami.utils.NameLookupInputHandler.NameTask lastTask;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.utils.NameLookupInputHandler.Companion Companion = null;
    private static final int WAIT_DELAY = 350;
    
    public NameLookupInputHandler(@org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService accountService, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        super();
    }
    
    public final void enqueueNextLookup(@org.jetbrains.annotations.NotNull()
    java.lang.String text) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\b\u0082\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\u0005\u001a\u00020\u0006H\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lnet/jami/utils/NameLookupInputHandler$NameTask;", "Ljava/util/TimerTask;", "mTextToLookup", "", "(Lnet/jami/utils/NameLookupInputHandler;Ljava/lang/String;)V", "run", "", "libringclient"})
    final class NameTask extends java.util.TimerTask {
        private final java.lang.String mTextToLookup = null;
        
        public NameTask(@org.jetbrains.annotations.NotNull()
        java.lang.String mTextToLookup) {
            super();
        }
        
        @java.lang.Override()
        public void run() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lnet/jami/utils/NameLookupInputHandler$Companion;", "", "()V", "WAIT_DELAY", "", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}