package net.jami.utils;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001\rB\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\"\u0010\b\u001a\u0004\u0018\u00010\t2\b\u0010\n\u001a\u0004\u0018\u00010\u00072\u0006\u0010\u000b\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u0004R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000e"}, d2 = {"Lnet/jami/utils/QRCodeUtils;", "", "()V", "QRCODE_IMAGE_PADDING", "", "QRCODE_IMAGE_SIZE", "TAG", "", "encodeStringAsQRCodeData", "Lnet/jami/utils/QRCodeUtils$QRCodeData;", "input", "foregroundColor", "backgroundColor", "QRCodeData", "libringclient"})
public final class QRCodeUtils {
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.utils.QRCodeUtils INSTANCE = null;
    private static final java.lang.String TAG = null;
    private static final int QRCODE_IMAGE_SIZE = 256;
    private static final int QRCODE_IMAGE_PADDING = 1;
    
    private QRCodeUtils() {
        super();
    }
    
    /**
     * @param input uri to be displayed
     * @return the resulting data
     */
    @org.jetbrains.annotations.Nullable()
    public final net.jami.utils.QRCodeUtils.QRCodeData encodeStringAsQRCodeData(@org.jetbrains.annotations.Nullable()
    java.lang.String input, int foregroundColor, int backgroundColor) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0015\n\u0000\n\u0002\u0010\b\n\u0002\b\b\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0007R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\u000b\u00a8\u0006\r"}, d2 = {"Lnet/jami/utils/QRCodeUtils$QRCodeData;", "", "data", "", "width", "", "height", "([III)V", "getData", "()[I", "getHeight", "()I", "getWidth", "libringclient"})
    public static final class QRCodeData {
        @org.jetbrains.annotations.NotNull()
        private final int[] data = null;
        private final int width = 0;
        private final int height = 0;
        
        public QRCodeData(@org.jetbrains.annotations.NotNull()
        int[] data, int width, int height) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final int[] getData() {
            return null;
        }
        
        public final int getWidth() {
            return 0;
        }
        
        public final int getHeight() {
            return 0;
        }
    }
}