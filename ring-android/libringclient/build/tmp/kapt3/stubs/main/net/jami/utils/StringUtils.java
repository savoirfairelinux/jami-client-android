package net.jami.utils;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\"\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u001c\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\u0010\r\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u0007J\u0016\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\u0006\u0010\f\u001a\u00020\u0007H\u0002J\u0010\u0010\r\u001a\u00020\u00072\u0006\u0010\u000e\u001a\u00020\u0007H\u0007J\u0012\u0010\u000f\u001a\u00020\u00102\b\u0010\b\u001a\u0004\u0018\u00010\u0011H\u0007J\u0012\u0010\u000f\u001a\u00020\u00102\b\u0010\b\u001a\u0004\u0018\u00010\u0007H\u0007J\u0012\u0010\u0012\u001a\u00020\u00102\b\u0010\u0013\u001a\u0004\u0018\u00010\u0007H\u0007J\u001c\u0010\u0014\u001a\u00020\u00072\u0006\u0010\u0015\u001a\u00020\u00072\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00070\u0017J\u0014\u0010\u0018\u001a\u0004\u0018\u00010\u00072\b\u0010\b\u001a\u0004\u0018\u00010\u0007H\u0007J\u0010\u0010\u0019\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u0007H\u0007R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001a"}, d2 = {"Lnet/jami/utils/StringUtils;", "", "()V", "EMOJI_BLOCKS", "", "Ljava/lang/Character$UnicodeBlock;", "capitalize", "", "s", "codePoints", "", "", "string", "getFileExtension", "filename", "isEmpty", "", "", "isOnlyEmoji", "message", "join", "separator", "values", "", "toNumber", "toPassword", "libringclient"})
public final class StringUtils {
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.utils.StringUtils INSTANCE = null;
    private static final java.util.Set<java.lang.Character.UnicodeBlock> EMOJI_BLOCKS = null;
    
    private StringUtils() {
        super();
    }
    
    @kotlin.jvm.JvmStatic()
    public static final boolean isEmpty(@org.jetbrains.annotations.Nullable()
    java.lang.String s) {
        return false;
    }
    
    @kotlin.jvm.JvmStatic()
    public static final boolean isEmpty(@org.jetbrains.annotations.Nullable()
    java.lang.CharSequence s) {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String capitalize(@org.jetbrains.annotations.NotNull()
    java.lang.String s) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @kotlin.jvm.JvmStatic()
    public static final java.lang.String toPassword(@org.jetbrains.annotations.NotNull()
    java.lang.String s) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    @kotlin.jvm.JvmStatic()
    public static final java.lang.String toNumber(@org.jetbrains.annotations.Nullable()
    java.lang.String s) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @kotlin.jvm.JvmStatic()
    public static final java.lang.String getFileExtension(@org.jetbrains.annotations.NotNull()
    java.lang.String filename) {
        return null;
    }
    
    private final java.lang.Iterable<java.lang.Integer> codePoints(java.lang.String string) {
        return null;
    }
    
    @kotlin.jvm.JvmStatic()
    public static final boolean isOnlyEmoji(@org.jetbrains.annotations.Nullable()
    java.lang.String message) {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String join(@org.jetbrains.annotations.NotNull()
    java.lang.String separator, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> values) {
        return null;
    }
}