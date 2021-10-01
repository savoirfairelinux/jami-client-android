package net.jami.utils;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\bJ\u0016\u0010\u0005\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eJ\u0018\u0010\u000f\u001a\u00020\u00062\u0006\u0010\u0010\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\bH\u0007R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lnet/jami/utils/FileUtils;", "", "()V", "TAG", "", "copyFile", "", "src", "Ljava/io/File;", "dest", "", "input", "Ljava/io/InputStream;", "out", "Ljava/io/OutputStream;", "moveFile", "file", "libringclient"})
public final class FileUtils {
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.utils.FileUtils INSTANCE = null;
    private static final java.lang.String TAG = null;
    
    private FileUtils() {
        super();
    }
    
    @kotlin.jvm.Throws(exceptionClasses = {java.io.IOException.class})
    public final void copyFile(@org.jetbrains.annotations.NotNull()
    java.io.InputStream input, @org.jetbrains.annotations.NotNull()
    java.io.OutputStream out) throws java.io.IOException {
    }
    
    public final boolean copyFile(@org.jetbrains.annotations.NotNull()
    java.io.File src, @org.jetbrains.annotations.NotNull()
    java.io.File dest) {
        return false;
    }
    
    @kotlin.jvm.JvmStatic()
    public static final boolean moveFile(@org.jetbrains.annotations.NotNull()
    java.io.File file, @org.jetbrains.annotations.NotNull()
    java.io.File dest) {
        return false;
    }
}