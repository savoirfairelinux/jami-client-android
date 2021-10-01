package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u00f0\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u001e\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010!\n\u0002\b\n\n\u0002\u0010\u000b\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010%\n\u0002\b\u0002\n\u0002\u0010#\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\b\u0007\n\u0002\u0010\r\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b-\n\u0002\u0018\u0002\n\u0002\b\u0007\u0018\u0000 \u00c1\u00012\u00020\u0001:\b\u00c1\u0001\u00c2\u0001\u00c3\u0001\u00c4\u0001B\u0017\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006B\u001f\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\n\u00a2\u0006\u0002\u0010\u000bJ\u0011\u0010\u0080\u0001\u001a\u00030\u0081\u00012\u0007\u0010\u0082\u0001\u001a\u00020\u0015J\u0013\u0010\u0083\u0001\u001a\u00030\u0081\u00012\t\u0010\u0084\u0001\u001a\u0004\u0018\u00010\u001bJ\u0010\u0010\u0085\u0001\u001a\u00030\u0081\u00012\u0006\u0010\u0004\u001a\u00020\u0005J\u0010\u0010\u0086\u0001\u001a\u00030\u0081\u00012\u0006\u0010\u0004\u001a\u00020\u0005J\u0012\u0010\u0086\u0001\u001a\u00030\u0081\u00012\b\u0010\u0087\u0001\u001a\u00030\u0088\u0001J\u0011\u0010\u0089\u0001\u001a\u00030\u0081\u00012\u0007\u0010\u008a\u0001\u001a\u00020\u0010J\u0012\u0010\u008b\u0001\u001a\u00030\u0081\u00012\b\u0010\u008c\u0001\u001a\u00030\u008d\u0001J\u001a\u0010\u008e\u0001\u001a\u00030\u0081\u00012\b\u0010\u008f\u0001\u001a\u00030\u0090\u00012\u0006\u0010\u0004\u001a\u00020\u0005J\u0010\u0010\u0091\u0001\u001a\u0002092\u0007\u0010\u008a\u0001\u001a\u00020\u0010J\u0011\u0010\u0092\u0001\u001a\u00030\u0081\u00012\u0007\u0010\u0093\u0001\u001a\u00020tJ\u0011\u0010\u0094\u0001\u001a\u00030\u0081\u00012\u0007\u0010\u0095\u0001\u001a\u000209J\u001b\u0010\u0096\u0001\u001a\u00030\u0081\u00012\b\u0010\u0004\u001a\u0004\u0018\u00010\u00052\u0007\u0010\u0097\u0001\u001a\u00020&J\u0011\u0010\u0098\u0001\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u0007\u001a\u00020\bJ\u0014\u0010\u0099\u0001\u001a\u0004\u0018\u00010\u00102\u0007\u0010\u009a\u0001\u001a\u00020$H\u0002J\r\u0010\u009b\u0001\u001a\b\u0012\u0004\u0012\u00020$0\u0019J\u0014\u0010\u009c\u0001\u001a\u0004\u0018\u00010\u001b2\t\u0010\u009d\u0001\u001a\u0004\u0018\u00010\u0003J\r\u0010\u009e\u0001\u001a\b\u0012\u0004\u0012\u00020\u00100\u0019J\u0012\u0010\u009f\u0001\u001a\u0004\u0018\u00010\u00102\u0007\u0010\u00a0\u0001\u001a\u00020\u0003J\r\u0010\u00a1\u0001\u001a\b\u0012\u0004\u0012\u00020o0\u0019J\r\u0010\u00a2\u0001\u001a\b\u0012\u0004\u0012\u0002090\u0019J\u001d\u0010\u00a3\u0001\u001a\u0002092\u0007\u0010\u00a4\u0001\u001a\u00020\u00102\t\u0010\u00a5\u0001\u001a\u0004\u0018\u00010\u0010H\u0002J\u0007\u0010\u00a6\u0001\u001a\u000209J\u0012\u0010\u00a7\u0001\u001a\u0002092\t\u0010\u00a5\u0001\u001a\u0004\u0018\u00010\u0003J\t\u0010\u00a8\u0001\u001a\u0004\u0018\u00010\u0003J\b\u0010\u00a9\u0001\u001a\u00030\u0081\u0001J\u0011\u0010\u00aa\u0001\u001a\u00030\u0081\u00012\u0007\u0010\u00ab\u0001\u001a\u00020\u001bJ\u0010\u0010\u00ac\u0001\u001a\u00030\u0081\u00012\u0006\u0010\u0004\u001a\u00020\u0005J\u0012\u0010\u00ad\u0001\u001a\u0002092\u0007\u0010\u00ae\u0001\u001a\u00020gH\u0002J\u0011\u0010\u00ad\u0001\u001a\u00030\u0081\u00012\u0007\u0010\u008a\u0001\u001a\u00020\u0010J\u0012\u0010\u00af\u0001\u001a\u0002092\u0007\u0010\u00a0\u0001\u001a\u00020\u0003H\u0002J\u0011\u0010\u00b0\u0001\u001a\u00030\u0081\u00012\u0007\u0010\u00ab\u0001\u001a\u00020$J\u0017\u0010\u00b1\u0001\u001a\u00030\u0081\u00012\r\u0010\u00b2\u0001\u001a\b\u0012\u0004\u0012\u00020\u00100\u001aJ\u0013\u0010\u00b3\u0001\u001a\u00030\u0081\u00012\u0007\u0010\u008a\u0001\u001a\u00020\u0010H\u0002J\u0013\u0010\u00b4\u0001\u001a\u00030\u0081\u00012\t\u0010\u00b5\u0001\u001a\u0004\u0018\u00010\u0003J\u0010\u0010\u00b6\u0001\u001a\u00030\u0081\u00012\u0006\u0010\t\u001a\u00020\nJ\u0011\u0010\u00b7\u0001\u001a\u00030\u0081\u00012\u0007\u0010\u00b8\u0001\u001a\u00020oJ\b\u0010\u00b9\u0001\u001a\u00030\u0081\u0001J\u0007\u0010\u00ba\u0001\u001a\u000209J\u001c\u0010\u00bb\u0001\u001a\u00030\u0081\u00012\b\u0010\u00bc\u0001\u001a\u00030\u008d\u00012\b\u0010\u00bd\u0001\u001a\u00030\u00be\u0001J\u0011\u0010\u00bf\u0001\u001a\u00030\u0081\u00012\u0007\u0010\u00c0\u0001\u001a\u00020\u0010R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u001a\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00150\u00148BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0016\u0010\u0017R\u001d\u0010\u0018\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001b0\u001a0\u00198F\u00a2\u0006\u0006\u001a\u0004\b\u001c\u0010\u001dR\u001a\u0010\u001e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001b0\u001a0\u001fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010 \u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u001a0\u00198F\u00a2\u0006\u0006\u001a\u0004\b!\u0010\u001dR\u001a\u0010\"\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u001a0\u001fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010#\u001a\b\u0012\u0004\u0012\u00020$0\u001fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010%\u001a\b\u0012\u0004\u0012\u00020&0\u00198F\u00a2\u0006\u0006\u001a\u0004\b\'\u0010\u001dR\u0014\u0010(\u001a\b\u0012\u0004\u0012\u00020&0\u001fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u00058F\u00a2\u0006\u0006\u001a\u0004\b)\u0010*R\u001d\u0010+\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u001a0\u00198F\u00a2\u0006\u0006\u001a\u0004\b,\u0010\u001dR\u0017\u0010-\u001a\b\u0012\u0004\u0012\u00020\u00050.\u00a2\u0006\b\n\u0000\u001a\u0004\b/\u00100R\u0013\u00101\u001a\u0004\u0018\u00010\u001b8F\u00a2\u0006\u0006\u001a\u0004\b2\u00103R\u0017\u00104\u001a\b\u0012\u0004\u0012\u00020\u001b0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b5\u0010\u0012R\u0013\u00106\u001a\u0004\u0018\u00010\u00038F\u00a2\u0006\u0006\u001a\u0004\b7\u0010\rR\u0011\u00108\u001a\u0002098F\u00a2\u0006\u0006\u001a\u0004\b8\u0010:R$\u0010<\u001a\u0002092\u0006\u0010;\u001a\u0002098F@FX\u0086\u000e\u00a2\u0006\f\u001a\u0004\b<\u0010:\"\u0004\b=\u0010>R\u0010\u0010?\u001a\u0004\u0018\u00010\u0010X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010@\u001a\b\u0012\u0004\u0012\u00020\u00100\u001fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001c\u0010A\u001a\u0004\u0018\u00010BX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bC\u0010D\"\u0004\bE\u0010FR\u0013\u0010G\u001a\u0004\u0018\u00010\u00108F\u00a2\u0006\u0006\u001a\u0004\bH\u0010IR\"\u0010K\u001a\u0004\u0018\u00010\u00032\b\u0010J\u001a\u0004\u0018\u00010\u0003@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\bL\u0010\rR\"\u0010M\u001a\n\u0012\u0004\u0012\u00020\u0000\u0018\u00010NX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bO\u0010P\"\u0004\bQ\u0010RR4\u0010U\u001a\n\u0012\u0004\u0012\u00020\u0000\u0018\u00010T2\u000e\u0010S\u001a\n\u0012\u0004\u0012\u00020\u0000\u0018\u00010T8F@FX\u0086\u000e\u00a2\u0006\f\u001a\u0004\bV\u0010W\"\u0004\bX\u0010YR\u001a\u0010Z\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u001a0\u001fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010[\u001a\u000209X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0016\u0010\\\u001a\n\u0012\u0004\u0012\u00020\u0000\u0018\u00010TX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010]\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00100^X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010_\u001a\b\u0012\u0004\u0012\u00020\n0\u001fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010`\u001a\b\u0012\u0004\u0012\u00020\u00030aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010b\u001a\u000209X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010c\u001a\b\u0012\u0004\u0012\u0002090\u001fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u00198F\u00a2\u0006\u0006\u001a\u0004\bd\u0010\u001dR\u001d\u0010e\u001a\u000e\u0012\u0004\u0012\u00020g\u0012\u0004\u0012\u00020\u00100f\u00a2\u0006\b\n\u0000\u001a\u0004\bh\u0010iR\u001d\u0010j\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u001a0N\u00a2\u0006\b\n\u0000\u001a\u0004\bk\u0010PR\u0017\u0010l\u001a\b\u0012\u0004\u0012\u00020\u00030\u00148F\u00a2\u0006\u0006\u001a\u0004\bm\u0010\u0017R\u0014\u0010n\u001a\b\u0012\u0004\u0012\u00020o0\u001fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0013\u0010p\u001a\u0004\u0018\u00010\u00038F\u00a2\u0006\u0006\u001a\u0004\bq\u0010\rR\u001d\u0010r\u001a\u000e\u0012\u0004\u0012\u00020g\u0012\u0004\u0012\u00020t0s8F\u00a2\u0006\u0006\u001a\u0004\bu\u0010vR \u0010w\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0010\u0012\u0004\u0012\u00020y0x0\u001fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R#\u0010z\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0010\u0012\u0004\u0012\u00020y0x0\u00198F\u00a2\u0006\u0006\u001a\u0004\b{\u0010\u001dR\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b|\u0010}R\u0013\u0010~\u001a\u0004\u0018\u00010\u00038F\u00a2\u0006\u0006\u001a\u0004\b\u007f\u0010\r\u00a8\u0006\u00c5\u0001"}, d2 = {"Lnet/jami/model/Conversation;", "Lnet/jami/model/ConversationHistory;", "accountId", "", "contact", "Lnet/jami/model/Contact;", "(Ljava/lang/String;Lnet/jami/model/Contact;)V", "uri", "Lnet/jami/model/Uri;", "mode", "Lnet/jami/model/Conversation$Mode;", "(Ljava/lang/String;Lnet/jami/model/Uri;Lnet/jami/model/Conversation$Mode;)V", "getAccountId", "()Ljava/lang/String;", "aggregateHistory", "Ljava/util/ArrayList;", "Lnet/jami/model/Interaction;", "getAggregateHistory", "()Ljava/util/ArrayList;", "callHistory", "", "Lnet/jami/model/Call;", "getCallHistory", "()Ljava/util/Collection;", "calls", "Lio/reactivex/rxjava3/core/Observable;", "", "Lnet/jami/model/Conference;", "getCalls", "()Lio/reactivex/rxjava3/core/Observable;", "callsSubject", "Lio/reactivex/rxjava3/subjects/Subject;", "cleared", "getCleared", "clearedSubject", "color", "", "composingStatus", "Lnet/jami/model/Account$ComposingStatus;", "getComposingStatus", "composingStatusSubject", "getContact", "()Lnet/jami/model/Contact;", "contactUpdates", "getContactUpdates", "contacts", "", "getContacts", "()Ljava/util/List;", "currentCall", "getCurrentCall", "()Lnet/jami/model/Conference;", "currentCalls", "getCurrentCalls", "displayName", "getDisplayName", "isSwarm", "", "()Z", "visible", "isVisible", "setVisible", "(Z)V", "lastDisplayed", "lastDisplayedSubject", "lastElementLoaded", "Lio/reactivex/rxjava3/core/Completable;", "getLastElementLoaded", "()Lio/reactivex/rxjava3/core/Completable;", "setLastElementLoaded", "(Lio/reactivex/rxjava3/core/Completable;)V", "lastEvent", "getLastEvent", "()Lnet/jami/model/Interaction;", "<set-?>", "lastRead", "getLastRead", "loaded", "Lio/reactivex/rxjava3/core/Single;", "getLoaded", "()Lio/reactivex/rxjava3/core/Single;", "setLoaded", "(Lio/reactivex/rxjava3/core/Single;)V", "l", "Lio/reactivex/rxjava3/subjects/SingleSubject;", "loading", "getLoading", "()Lio/reactivex/rxjava3/subjects/SingleSubject;", "setLoading", "(Lio/reactivex/rxjava3/subjects/SingleSubject;)V", "mContactSubject", "mDirty", "mLoadingSubject", "mMessages", "", "mMode", "mRoots", "", "mVisible", "mVisibleSubject", "getMode", "rawHistory", "Ljava/util/NavigableMap;", "", "getRawHistory", "()Ljava/util/NavigableMap;", "sortedHistory", "getSortedHistory", "swarmRoot", "getSwarmRoot", "symbol", "", "title", "getTitle", "unreadTextMessages", "Ljava/util/TreeMap;", "Lnet/jami/model/TextMessage;", "getUnreadTextMessages", "()Ljava/util/TreeMap;", "updatedElementSubject", "Lnet/jami/utils/Tuple;", "Lnet/jami/model/Conversation$ElementStatus;", "updatedElements", "getUpdatedElements", "getUri", "()Lnet/jami/model/Uri;", "uriTitle", "getUriTitle", "addCall", "", "call", "addConference", "conference", "addContact", "addContactEvent", "contactEvent", "Lnet/jami/model/ContactEvent;", "addElement", "interaction", "addFileTransfer", "dataTransfer", "Lnet/jami/model/DataTransfer;", "addRequestEvent", "request", "Lnet/jami/model/TrustRequest;", "addSwarmElement", "addTextMessage", "txt", "clearHistory", "delete", "composingStatusChanged", "composing", "findContact", "findConversationElement", "transferId", "getColor", "getConference", "id", "getLastDisplayed", "getMessage", "messageId", "getSymbol", "getVisible", "isAfter", "previous", "query", "isLoaded", "matches", "readMessages", "removeAll", "removeConference", "c", "removeContact", "removeInteraction", "interactionId", "removeSwarmInteraction", "setColor", "setHistory", "loadedConversation", "setInteractionProperties", "setLastMessageRead", "lastMessageRead", "setMode", "setSymbol", "s", "sortHistory", "stopLoading", "updateFileTransfer", "transfer", "eventCode", "Lnet/jami/model/Interaction$InteractionStatus;", "updateInteraction", "element", "Companion", "ConversationActionCallback", "ElementStatus", "Mode", "libringclient"})
public final class Conversation extends net.jami.model.ConversationHistory {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String accountId = null;
    @org.jetbrains.annotations.NotNull()
    private final net.jami.model.Uri uri = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<net.jami.model.Contact> contacts = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.NavigableMap<java.lang.Long, net.jami.model.Interaction> rawHistory = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.ArrayList<net.jami.model.Conference> currentCalls = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.ArrayList<net.jami.model.Interaction> aggregateHistory = null;
    private net.jami.model.Interaction lastDisplayed;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.utils.Tuple<net.jami.model.Interaction, net.jami.model.Conversation.ElementStatus>> updatedElementSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.model.Interaction> lastDisplayedSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<java.util.List<net.jami.model.Interaction>> clearedSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<java.util.List<net.jami.model.Conference>> callsSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.model.Account.ComposingStatus> composingStatusSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<java.lang.Integer> color = null;
    private final io.reactivex.rxjava3.subjects.Subject<java.lang.CharSequence> symbol = null;
    private final io.reactivex.rxjava3.subjects.Subject<java.util.List<net.jami.model.Contact>> mContactSubject = null;
    @org.jetbrains.annotations.Nullable()
    private io.reactivex.rxjava3.core.Single<net.jami.model.Conversation> loaded;
    @org.jetbrains.annotations.Nullable()
    private io.reactivex.rxjava3.core.Completable lastElementLoaded;
    private final java.util.Set<java.lang.String> mRoots = null;
    private final java.util.Map<java.lang.String, net.jami.model.Interaction> mMessages = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String lastRead;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.model.Conversation.Mode> mMode = null;
    private boolean mVisible = false;
    private final io.reactivex.rxjava3.subjects.Subject<java.lang.Boolean> mVisibleSubject = null;
    private boolean mDirty = false;
    private io.reactivex.rxjava3.subjects.SingleSubject<net.jami.model.Conversation> mLoadingSubject;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.core.Single<java.util.List<net.jami.model.Interaction>> sortedHistory = null;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.model.Conversation.Companion Companion = null;
    private static final java.lang.String TAG = null;
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getAccountId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Uri getUri() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<net.jami.model.Contact> getContacts() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.NavigableMap<java.lang.Long, net.jami.model.Interaction> getRawHistory() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.ArrayList<net.jami.model.Conference> getCurrentCalls() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.ArrayList<net.jami.model.Interaction> getAggregateHistory() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.Conversation> getLoaded() {
        return null;
    }
    
    public final void setLoaded(@org.jetbrains.annotations.Nullable()
    io.reactivex.rxjava3.core.Single<net.jami.model.Conversation> p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final io.reactivex.rxjava3.core.Completable getLastElementLoaded() {
        return null;
    }
    
    public final void setLastElementLoaded(@org.jetbrains.annotations.Nullable()
    io.reactivex.rxjava3.core.Completable p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getLastRead() {
        return null;
    }
    
    public Conversation(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact) {
        super();
    }
    
    public Conversation(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri, @org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation.Mode mode) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Conference getConference(@org.jetbrains.annotations.Nullable()
    java.lang.String id) {
        return null;
    }
    
    public final void composingStatusChanged(@org.jetbrains.annotations.Nullable()
    net.jami.model.Contact contact, @org.jetbrains.annotations.NotNull()
    net.jami.model.Account.ComposingStatus composing) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Conversation.Mode> getMode() {
        return null;
    }
    
    public final boolean isSwarm() {
        return false;
    }
    
    public final boolean matches(@org.jetbrains.annotations.Nullable()
    java.lang.String query) {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getDisplayName() {
        return null;
    }
    
    public final void addContact(@org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact) {
    }
    
    public final void removeContact(@org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getTitle() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUriTitle() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<net.jami.model.Contact>> getContactUpdates() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    @kotlin.jvm.Synchronized()
    public final synchronized java.lang.String readMessages() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    @kotlin.jvm.Synchronized()
    public final synchronized net.jami.model.Interaction getMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String messageId) {
        return null;
    }
    
    public final void setLastMessageRead(@org.jetbrains.annotations.Nullable()
    java.lang.String lastMessageRead) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final io.reactivex.rxjava3.subjects.SingleSubject<net.jami.model.Conversation> getLoading() {
        return null;
    }
    
    public final void setLoading(@org.jetbrains.annotations.Nullable()
    io.reactivex.rxjava3.subjects.SingleSubject<net.jami.model.Conversation> l) {
    }
    
    public final boolean stopLoading() {
        return false;
    }
    
    public final void setMode(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation.Mode mode) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.utils.Tuple<net.jami.model.Interaction, net.jami.model.Conversation.ElementStatus>> getUpdatedElements() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Interaction> getLastDisplayed() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<net.jami.model.Interaction>> getCleared() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<net.jami.model.Conference>> getCalls() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Account.ComposingStatus> getComposingStatus() {
        return null;
    }
    
    public final void addConference(@org.jetbrains.annotations.Nullable()
    net.jami.model.Conference conference) {
    }
    
    public final void removeConference(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conference c) {
    }
    
    public final boolean isVisible() {
        return false;
    }
    
    public final void setVisible(boolean visible) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.lang.Boolean> getVisible() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Contact getContact() {
        return null;
    }
    
    public final void addCall(@org.jetbrains.annotations.NotNull()
    net.jami.model.Call call) {
    }
    
    private final void setInteractionProperties(net.jami.model.Interaction interaction) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Contact findContact(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri) {
        return null;
    }
    
    public final void addTextMessage(@org.jetbrains.annotations.NotNull()
    net.jami.model.TextMessage txt) {
    }
    
    public final void addRequestEvent(@org.jetbrains.annotations.NotNull()
    net.jami.model.TrustRequest request, @org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact) {
    }
    
    public final void addContactEvent(@org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact) {
    }
    
    public final void addContactEvent(@org.jetbrains.annotations.NotNull()
    net.jami.model.ContactEvent contactEvent) {
    }
    
    public final void addFileTransfer(@org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer dataTransfer) {
    }
    
    private final boolean isAfter(net.jami.model.Interaction previous, net.jami.model.Interaction query) {
        return false;
    }
    
    public final void updateInteraction(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction element) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<java.util.List<net.jami.model.Interaction>> getSortedHistory() {
        return null;
    }
    
    public final void sortHistory() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Interaction getLastEvent() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Conference getCurrentCall() {
        return null;
    }
    
    private final java.util.Collection<net.jami.model.Call> getCallHistory() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.TreeMap<java.lang.Long, net.jami.model.TextMessage> getUnreadTextMessages() {
        return null;
    }
    
    private final net.jami.model.Interaction findConversationElement(int transferId) {
        return null;
    }
    
    private final boolean removeSwarmInteraction(java.lang.String messageId) {
        return false;
    }
    
    private final boolean removeInteraction(long interactionId) {
        return false;
    }
    
    /**
     * Clears the conversation cache.
     * @param delete true if you do not want to re-add contact events
     */
    public final void clearHistory(boolean delete) {
    }
    
    public final void setHistory(@org.jetbrains.annotations.NotNull()
    java.util.List<? extends net.jami.model.Interaction> loadedConversation) {
    }
    
    public final void addElement(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction interaction) {
    }
    
    public final boolean addSwarmElement(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction interaction) {
        return false;
    }
    
    public final boolean isLoaded() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Collection<java.lang.String> getSwarmRoot() {
        return null;
    }
    
    public final void updateFileTransfer(@org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer transfer, @org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction.InteractionStatus eventCode) {
    }
    
    public final void removeInteraction(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction interaction) {
    }
    
    public final void removeAll() {
    }
    
    public final void setColor(int c) {
    }
    
    public final void setSymbol(@org.jetbrains.annotations.NotNull()
    java.lang.CharSequence s) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.lang.Integer> getColor() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.lang.CharSequence> getSymbol() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0005\b\u0086\u0001\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005\u00a8\u0006\u0006"}, d2 = {"Lnet/jami/model/Conversation$ElementStatus;", "", "(Ljava/lang/String;I)V", "UPDATE", "REMOVE", "ADD", "libringclient"})
    public static enum ElementStatus {
        /*public static final*/ UPDATE /* = new UPDATE() */,
        /*public static final*/ REMOVE /* = new REMOVE() */,
        /*public static final*/ ADD /* = new ADD() */;
        
        ElementStatus() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\b\b\u0086\u0001\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007j\u0002\b\b\u00a8\u0006\t"}, d2 = {"Lnet/jami/model/Conversation$Mode;", "", "(Ljava/lang/String;I)V", "OneToOne", "AdminInvitesOnly", "InvitesOnly", "Syncing", "Public", "Legacy", "libringclient"})
    public static enum Mode {
        /*public static final*/ OneToOne /* = new OneToOne() */,
        /*public static final*/ AdminInvitesOnly /* = new AdminInvitesOnly() */,
        /*public static final*/ InvitesOnly /* = new InvitesOnly() */,
        /*public static final*/ Syncing /* = new Syncing() */,
        /*public static final*/ Public /* = new Public() */,
        /*public static final*/ Legacy /* = new Legacy() */;
        
        Mode() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&J\u0010\u0010\u0006\u001a\u00020\u00032\u0006\u0010\u0007\u001a\u00020\bH&J\u0010\u0010\t\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&\u00a8\u0006\n"}, d2 = {"Lnet/jami/model/Conversation$ConversationActionCallback;", "", "clearConversation", "", "conversationUri", "Lnet/jami/model/Uri;", "copyContactNumberToClipboard", "contactNumber", "", "removeConversation", "libringclient"})
    public static abstract interface ConversationActionCallback {
        
        public abstract void removeConversation(@org.jetbrains.annotations.NotNull()
        net.jami.model.Uri conversationUri);
        
        public abstract void clearConversation(@org.jetbrains.annotations.NotNull()
        net.jami.model.Uri conversationUri);
        
        public abstract void copyContactNumberToClipboard(@org.jetbrains.annotations.NotNull()
        java.lang.String contactNumber);
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u0006H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lnet/jami/model/Conversation$Companion;", "", "()V", "TAG", "", "getTypedInteraction", "Lnet/jami/model/Interaction;", "interaction", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        private final net.jami.model.Interaction getTypedInteraction(net.jami.model.Interaction interaction) {
            return null;
        }
    }
}