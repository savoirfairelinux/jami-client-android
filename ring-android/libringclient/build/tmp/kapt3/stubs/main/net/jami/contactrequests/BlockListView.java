package net.jami.contactrequests;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u001e\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&J\b\u0010\u0006\u001a\u00020\u0003H&J\u0016\u0010\u0007\u001a\u00020\u00032\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\tH&\u00a8\u0006\u000b"}, d2 = {"Lnet/jami/contactrequests/BlockListView;", "", "displayEmptyListMessage", "", "display", "", "hideListView", "updateView", "list", "", "Lnet/jami/model/Contact;", "libringclient"})
public abstract interface BlockListView {
    
    public abstract void updateView(@org.jetbrains.annotations.NotNull()
    java.util.Collection<net.jami.model.Contact> list);
    
    public abstract void hideListView();
    
    public abstract void displayEmptyListMessage(boolean display);
}