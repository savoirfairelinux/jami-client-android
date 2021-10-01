package net.jami.utils;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\b\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0010\u0012\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\b\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J$\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000f2\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00042\u0006\u0010\u0014\u001a\u00020\u0012J\u000e\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0010J\u0014\u0010\u0018\u001a\u0004\u0018\u00010\u00102\b\u0010\u0019\u001a\u0004\u0018\u00010\u0012H\u0002J\u001c\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00100\u000f2\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0004J\u001c\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00100\u000f2\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0004J\"\u0010\u001c\u001a\u0004\u0018\u00010\u00102\u0006\u0010\u0011\u001a\u00020\u00122\b\u0010\u001d\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u0013\u001a\u00020\u0004J\u0018\u0010\u001e\u001a\u00020\u00122\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0004H\u0002J\u001a\u0010\u001f\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00040 2\u0006\u0010!\u001a\u00020\u0004J\u0018\u0010\"\u001a\u00020\u00122\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0004H\u0002J,\u0010#\u001a\b\u0012\u0004\u0012\u00020\u00100\u000f2\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00042\u0006\u0010$\u001a\u00020\u00042\u0006\u0010\u0014\u001a\u00020\u0012J \u0010%\u001a\u0012\u0012\u0006\u0012\u0004\u0018\u00010\u0004\u0012\u0006\u0012\u0004\u0018\u00010\'0&2\b\u0010\u0014\u001a\u0004\u0018\u00010\u0010J$\u0010(\u001a\b\u0012\u0004\u0012\u00020\u00100\u000f2\u0006\u0010\u0014\u001a\u00020\u00102\u0006\u0010\u0013\u001a\u00020\u00042\u0006\u0010\u0011\u001a\u00020\u0012J(\u0010)\u001a\u00020*2\b\u0010\u0014\u001a\u0004\u0018\u00010\u00102\u0006\u0010\u0013\u001a\u00020\u00042\u0006\u0010\u001d\u001a\u00020\u00042\u0006\u0010\u0011\u001a\u00020\u0012J\"\u0010+\u001a\u00020*2\b\u0010\u0014\u001a\u0004\u0018\u00010\u00102\u0006\u0010\u001d\u001a\u00020\u00042\u0006\u0010\u0019\u001a\u00020\u0012H\u0002J\u0018\u0010,\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0004H\u0002J\u0012\u0010-\u001a\u0004\u0018\u00010\u00042\b\u0010\u0014\u001a\u0004\u0018\u00010\u0010J$\u0010.\u001a\u00020\u00102\b\u0010/\u001a\u0004\u0018\u00010\u00042\b\u00100\u001a\u0004\u0018\u00010\u00042\b\u00101\u001a\u0004\u0018\u00010\'R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u0006\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u00062"}, d2 = {"Lnet/jami/utils/VCardUtils;", "", "()V", "LOCAL_USER_VCARD_NAME", "", "MIME_PROFILE_VCARD", "TAG", "getTAG", "()Ljava/lang/String;", "VCARD_KEY_MIME_TYPE", "VCARD_KEY_OF", "VCARD_KEY_PART", "VCARD_MAX_SIZE", "", "accountProfileReceived", "Lio/reactivex/rxjava3/core/Single;", "Lezvcard/VCard;", "filesDir", "Ljava/io/File;", "accountId", "vcard", "isEmpty", "", "vCard", "loadFromDisk", "path", "loadLocalProfileFromDisk", "loadLocalProfileFromDiskWithDefault", "loadPeerProfileFromDisk", "filename", "localProfilePath", "parseMimeAttributes", "Ljava/util/HashMap;", "mime", "peerProfilePath", "peerProfileReceived", "peerId", "readData", "Lkotlin/Pair;", "", "saveLocalProfileToDisk", "savePeerProfileToDisk", "", "saveToDisk", "setupDefaultProfile", "vcardToString", "writeData", "uri", "displayName", "picture", "libringclient"})
public final class VCardUtils {
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.utils.VCardUtils INSTANCE = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String MIME_PROFILE_VCARD = "x-ring/ring.profile.vcard";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String VCARD_KEY_MIME_TYPE = "mimeType";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String VCARD_KEY_PART = "part";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String VCARD_KEY_OF = "of";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String LOCAL_USER_VCARD_NAME = "profile.vcf";
    private static final long VCARD_MAX_SIZE = 8388608L;
    
    private VCardUtils() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getTAG() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlin.Pair<java.lang.String, byte[]> readData(@org.jetbrains.annotations.Nullable()
    ezvcard.VCard vcard) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final ezvcard.VCard writeData(@org.jetbrains.annotations.Nullable()
    java.lang.String uri, @org.jetbrains.annotations.Nullable()
    java.lang.String displayName, @org.jetbrains.annotations.Nullable()
    byte[] picture) {
        return null;
    }
    
    /**
     * Parse the "elements" of the mime attributes to build a proper hashtable
     *
     * @param mime the mimetype as returned by the daemon
     * @return a correct hashtable, null if invalid input
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.HashMap<java.lang.String, java.lang.String> parseMimeAttributes(@org.jetbrains.annotations.NotNull()
    java.lang.String mime) {
        return null;
    }
    
    public final void savePeerProfileToDisk(@org.jetbrains.annotations.Nullable()
    ezvcard.VCard vcard, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String filename, @org.jetbrains.annotations.NotNull()
    java.io.File filesDir) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<ezvcard.VCard> saveLocalProfileToDisk(@org.jetbrains.annotations.NotNull()
    ezvcard.VCard vcard, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.io.File filesDir) {
        return null;
    }
    
    /**
     * Saves a vcard string to an internal new vcf file.
     *
     * @param vcard    the VCard to save
     * @param filename the filename of the vcf
     * @param path     the path of the vcf
     */
    private final void saveToDisk(ezvcard.VCard vcard, java.lang.String filename, java.io.File path) {
    }
    
    @org.jetbrains.annotations.Nullable()
    @kotlin.jvm.Throws(exceptionClasses = {java.io.IOException.class})
    public final ezvcard.VCard loadPeerProfileFromDisk(@org.jetbrains.annotations.NotNull()
    java.io.File filesDir, @org.jetbrains.annotations.Nullable()
    java.lang.String filename, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId) throws java.io.IOException {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<ezvcard.VCard> loadLocalProfileFromDisk(@org.jetbrains.annotations.NotNull()
    java.io.File filesDir, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<ezvcard.VCard> loadLocalProfileFromDiskWithDefault(@org.jetbrains.annotations.NotNull()
    java.io.File filesDir, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        return null;
    }
    
    /**
     * Loads the vcard file from the disk
     *
     * @param path the filename of the vcard
     * @return the VCard or null
     */
    @kotlin.jvm.Throws(exceptionClasses = {java.io.IOException.class})
    private final ezvcard.VCard loadFromDisk(java.io.File path) throws java.io.IOException {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String vcardToString(@org.jetbrains.annotations.Nullable()
    ezvcard.VCard vcard) {
        return null;
    }
    
    public final boolean isEmpty(@org.jetbrains.annotations.NotNull()
    ezvcard.VCard vCard) {
        return false;
    }
    
    private final java.io.File peerProfilePath(java.io.File filesDir, java.lang.String accountId) {
        return null;
    }
    
    private final java.io.File localProfilePath(java.io.File filesDir, java.lang.String accountId) {
        return null;
    }
    
    private final ezvcard.VCard setupDefaultProfile(java.io.File filesDir, java.lang.String accountId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<ezvcard.VCard> accountProfileReceived(@org.jetbrains.annotations.NotNull()
    java.io.File filesDir, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.io.File vcard) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<ezvcard.VCard> peerProfileReceived(@org.jetbrains.annotations.NotNull()
    java.io.File filesDir, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String peerId, @org.jetbrains.annotations.NotNull()
    java.io.File vcard) {
        return null;
    }
}