package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u000f\u0018\u00002\u00020\u0001:\u0001\u001aB\u0017\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006B%\b\u0017\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0007\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\u0002\u0010\tB!\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\u0010\b\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\u0002\u0010\nB+\b\u0016\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0007\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\u0010\b\u001a\u0004\u0018\u00010\u0007\u0012\u0006\u0010\u000b\u001a\u00020\f\u00a2\u0006\u0002\u0010\rB)\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\u0010\b\u001a\u0004\u0018\u00010\u0007\u0012\u0006\u0010\u000b\u001a\u00020\f\u00a2\u0006\u0002\u0010\u000eR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0013\u0010\b\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0011\u0010\u0015\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0011\u0010\u0018\u001a\u00020\f8F\u00a2\u0006\u0006\u001a\u0004\b\u0019\u0010\u0017\u00a8\u0006\u001b"}, d2 = {"Lnet/jami/model/Phone;", "", "number", "Lnet/jami/model/Uri;", "category", "", "(Lnet/jami/model/Uri;I)V", "", "label", "(Ljava/lang/String;ILjava/lang/String;)V", "(Lnet/jami/model/Uri;ILjava/lang/String;)V", "numberType", "Lnet/jami/model/Phone$NumberType;", "(Ljava/lang/String;ILjava/lang/String;Lnet/jami/model/Phone$NumberType;)V", "(Lnet/jami/model/Uri;ILjava/lang/String;Lnet/jami/model/Phone$NumberType;)V", "getCategory", "()I", "getLabel", "()Ljava/lang/String;", "getNumber", "()Lnet/jami/model/Uri;", "numbertype", "getNumbertype", "()Lnet/jami/model/Phone$NumberType;", "type", "getType", "NumberType", "libringclient"})
public final class Phone {
    @org.jetbrains.annotations.NotNull()
    private final net.jami.model.Phone.NumberType numbertype = null;
    @org.jetbrains.annotations.NotNull()
    private final net.jami.model.Uri number = null;
    private final int category = 0;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String label = null;
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Phone.NumberType getNumbertype() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Uri getNumber() {
        return null;
    }
    
    public final int getCategory() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getLabel() {
        return null;
    }
    
    public Phone(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri number, int category) {
        super();
    }
    
    @kotlin.jvm.JvmOverloads()
    public Phone(@org.jetbrains.annotations.Nullable()
    java.lang.String number, int category) {
        super();
    }
    
    @kotlin.jvm.JvmOverloads()
    public Phone(@org.jetbrains.annotations.Nullable()
    java.lang.String number, int category, @org.jetbrains.annotations.Nullable()
    java.lang.String label) {
        super();
    }
    
    public Phone(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri number, int category, @org.jetbrains.annotations.Nullable()
    java.lang.String label) {
        super();
    }
    
    public Phone(@org.jetbrains.annotations.Nullable()
    java.lang.String number, int category, @org.jetbrains.annotations.Nullable()
    java.lang.String label, @org.jetbrains.annotations.NotNull()
    net.jami.model.Phone.NumberType numberType) {
        super();
    }
    
    public Phone(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri number, int category, @org.jetbrains.annotations.Nullable()
    java.lang.String label, @org.jetbrains.annotations.NotNull()
    net.jami.model.Phone.NumberType numberType) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Phone.NumberType getType() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\b\n\u0002\b\b\b\u0086\u0001\u0018\u0000 \n2\b\u0012\u0004\u0012\u00020\u00000\u0001:\u0001\nB\u000f\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007j\u0002\b\bj\u0002\b\t\u00a8\u0006\u000b"}, d2 = {"Lnet/jami/model/Phone$NumberType;", "", "type", "", "(Ljava/lang/String;II)V", "UNKNOWN", "TEL", "SIP", "IP", "RING", "Companion", "libringclient"})
    public static enum NumberType {
        /*public static final*/ UNKNOWN /* = new UNKNOWN(0) */,
        /*public static final*/ TEL /* = new TEL(0) */,
        /*public static final*/ SIP /* = new SIP(0) */,
        /*public static final*/ IP /* = new IP(0) */,
        /*public static final*/ RING /* = new RING(0) */;
        private final int type = 0;
        @org.jetbrains.annotations.NotNull()
        public static final net.jami.model.Phone.NumberType.Companion Companion = null;
        private static final net.jami.model.Phone.NumberType[] VALS = null;
        
        NumberType(int type) {
        }
        
        @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0007\u001a\u00020\u00052\u0006\u0010\b\u001a\u00020\tR\u0016\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0004\n\u0002\u0010\u0006\u00a8\u0006\n"}, d2 = {"Lnet/jami/model/Phone$NumberType$Companion;", "", "()V", "VALS", "", "Lnet/jami/model/Phone$NumberType;", "[Lnet/jami/model/Phone$NumberType;", "fromInteger", "id", "", "libringclient"})
        public static final class Companion {
            
            private Companion() {
                super();
            }
            
            @org.jetbrains.annotations.NotNull()
            public final net.jami.model.Phone.NumberType fromInteger(int id) {
                return null;
            }
        }
    }
}