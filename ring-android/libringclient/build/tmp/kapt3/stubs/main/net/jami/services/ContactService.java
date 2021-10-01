package net.jami.services;

import java.lang.System;

/**
 * This service handles the contacts
 * - Load the contacts stored in the system
 * - Keep a local cache of the contacts
 * - Provide query tools to search contacts by id, number, ...
 */
@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000z\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0003\b&\u0018\u0000 72\u00020\u0001:\u00017B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0016\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0014J\u001a\u0010\u0015\u001a\u0004\u0018\u00010\u00102\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0019H$J\u0018\u0010\u001a\u001a\u0004\u0018\u00010\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u001b\u001a\u00020\u0019J\u0012\u0010\u001c\u001a\u0004\u0018\u00010\u00102\u0006\u0010\u001b\u001a\u00020\u0019H$J\u0012\u0010\u001d\u001a\u0004\u0018\u00010\u00102\u0006\u0010\u001b\u001a\u00020\u0019H$J0\u0010\u001e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100 0\u001f2\u0006\u0010!\u001a\u00020\u00192\f\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00100 2\u0006\u0010#\u001a\u00020$J\u001c\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u00100\u001f2\u0006\u0010!\u001a\u00020\u00192\u0006\u0010%\u001a\u00020\u0010J$\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u00100\u001f2\u0006\u0010!\u001a\u00020\u00192\u0006\u0010%\u001a\u00020\u00102\u0006\u0010#\u001a\u00020$J\u0018\u0010&\u001a\u00020\'2\u0006\u0010%\u001a\u00020\u00102\u0006\u0010!\u001a\u00020\u0019H&J2\u0010(\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0017\u0012\u0004\u0012\u00020\u00100)0\u001f2\u0006\u0010*\u001a\u00020$2\u0006\u0010+\u001a\u00020$2\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012J$\u0010,\u001a\u000e\u0012\u0004\u0012\u00020\u0017\u0012\u0004\u0012\u00020\u00100)2\u0006\u0010*\u001a\u00020$2\u0006\u0010+\u001a\u00020$H&J0\u0010-\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100 0.2\u0006\u0010!\u001a\u00020\u00192\f\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00100 2\u0006\u0010#\u001a\u00020$J$\u0010-\u001a\b\u0012\u0004\u0012\u00020\u00100.2\u0006\u0010!\u001a\u00020\u00192\u0006\u0010%\u001a\u00020\u00102\u0006\u0010#\u001a\u00020$J0\u0010/\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100.0 2\u0006\u0010!\u001a\u00020\u00192\f\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00100 2\u0006\u0010#\u001a\u00020$J4\u00100\u001a\b\u0012\u0004\u0012\u0002010\u001f2\u0006\u0010!\u001a\u00020\u00192\b\u0010\u0013\u001a\u0004\u0018\u00010\u00192\b\u00102\u001a\u0004\u0018\u00010\u00192\b\u00103\u001a\u0004\u0018\u00010\u0019H&J \u00104\u001a\u0002052\u0006\u0010%\u001a\u00020\u00102\u0006\u0010!\u001a\u00020\u00192\u0006\u00106\u001a\u000201H&R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e\u00a8\u00068"}, d2 = {"Lnet/jami/services/ContactService;", "", "mPreferencesService", "Lnet/jami/services/PreferencesService;", "mDeviceRuntimeService", "Lnet/jami/services/DeviceRuntimeService;", "mAccountService", "Lnet/jami/services/AccountService;", "(Lnet/jami/services/PreferencesService;Lnet/jami/services/DeviceRuntimeService;Lnet/jami/services/AccountService;)V", "getMAccountService", "()Lnet/jami/services/AccountService;", "getMDeviceRuntimeService", "()Lnet/jami/services/DeviceRuntimeService;", "getMPreferencesService", "()Lnet/jami/services/PreferencesService;", "findContact", "Lnet/jami/model/Contact;", "account", "Lnet/jami/model/Account;", "uri", "Lnet/jami/model/Uri;", "findContactByIdFromSystem", "contactId", "", "contactKey", "", "findContactByNumber", "number", "findContactByNumberFromSystem", "findContactBySipNumberFromSystem", "getLoadedContact", "Lio/reactivex/rxjava3/core/Single;", "", "accountId", "contacts", "withPresence", "", "contact", "loadContactData", "Lio/reactivex/rxjava3/core/Completable;", "loadContacts", "", "loadRingContacts", "loadSipContacts", "loadContactsFromSystem", "observeContact", "Lio/reactivex/rxjava3/core/Observable;", "observeLoadedContact", "saveVCardContact", "Lezvcard/VCard;", "displayName", "pictureB64", "saveVCardContactData", "", "vcard", "Companion", "libringclient"})
public abstract class ContactService {
    @org.jetbrains.annotations.NotNull()
    private final net.jami.services.PreferencesService mPreferencesService = null;
    @org.jetbrains.annotations.NotNull()
    private final net.jami.services.DeviceRuntimeService mDeviceRuntimeService = null;
    @org.jetbrains.annotations.NotNull()
    private final net.jami.services.AccountService mAccountService = null;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.services.ContactService.Companion Companion = null;
    private static final java.lang.String TAG = null;
    
    public ContactService(@org.jetbrains.annotations.NotNull()
    net.jami.services.PreferencesService mPreferencesService, @org.jetbrains.annotations.NotNull()
    net.jami.services.DeviceRuntimeService mDeviceRuntimeService, @org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.services.PreferencesService getMPreferencesService() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.services.DeviceRuntimeService getMDeviceRuntimeService() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.services.AccountService getMAccountService() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.util.Map<java.lang.Long, net.jami.model.Contact> loadContactsFromSystem(boolean loadRingContacts, boolean loadSipContacts);
    
    @org.jetbrains.annotations.Nullable()
    protected abstract net.jami.model.Contact findContactByIdFromSystem(long contactId, @org.jetbrains.annotations.NotNull()
    java.lang.String contactKey);
    
    @org.jetbrains.annotations.Nullable()
    protected abstract net.jami.model.Contact findContactBySipNumberFromSystem(@org.jetbrains.annotations.NotNull()
    java.lang.String number);
    
    @org.jetbrains.annotations.Nullable()
    protected abstract net.jami.model.Contact findContactByNumberFromSystem(@org.jetbrains.annotations.NotNull()
    java.lang.String number);
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.rxjava3.core.Completable loadContactData(@org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId);
    
    public abstract void saveVCardContactData(@org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    ezvcard.VCard vcard);
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.rxjava3.core.Single<ezvcard.VCard> saveVCardContact(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.Nullable()
    java.lang.String uri, @org.jetbrains.annotations.Nullable()
    java.lang.String displayName, @org.jetbrains.annotations.Nullable()
    java.lang.String pictureB64);
    
    /**
     * Load contacts from system and generate a local contact cache
     *
     * @param loadRingContacts if true, ring contacts will be taken care of
     * @param loadSipContacts  if true, sip contacts will be taken care of
     */
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<java.util.Map<java.lang.Long, net.jami.model.Contact>> loadContacts(boolean loadRingContacts, boolean loadSipContacts, @org.jetbrains.annotations.Nullable()
    net.jami.model.Account account) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Contact> observeContact(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact, boolean withPresence) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<net.jami.model.Contact>> observeContact(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.util.List<net.jami.model.Contact> contacts, boolean withPresence) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.Contact> getLoadedContact(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact, boolean withPresence) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.Contact> getLoadedContact(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<java.util.List<net.jami.model.Contact>> getLoadedContact(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.util.List<net.jami.model.Contact> contacts, boolean withPresence) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<io.reactivex.rxjava3.core.Observable<net.jami.model.Contact>> observeLoadedContact(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.util.List<net.jami.model.Contact> contacts, boolean withPresence) {
        return null;
    }
    
    /**
     * Searches a contact in the local cache and then in the system repository
     * In the last case, the contact is created and added to the local cache
     *
     * @return The found/created contact
     */
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Contact findContactByNumber(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account, @org.jetbrains.annotations.NotNull()
    java.lang.String number) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Contact findContact(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lnet/jami/services/ContactService$Companion;", "", "()V", "TAG", "", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}