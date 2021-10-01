package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0080\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010$\n\u0000\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010\u001e\n\u0002\b\u0003\n\u0002\u0010%\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b+\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0013\n\u0002\u0010!\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\n\n\u0002\u0010\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0015\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\r\n\u0002\b\u0010\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b)\u0018\u0000 \u0089\u00022\u00020\u0001:\n\u0089\u0002\u008a\u0002\u008b\u0002\u008c\u0002\u008d\u0002BO\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u0005\u0012\u0018\u0010\u0006\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u00050\u0007\u0012\u0012\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u0005\u00a2\u0006\u0002\u0010\tJ\u001a\u0010\u009b\u0001\u001a\u00030\u009c\u00012\u0007\u0010\u009d\u0001\u001a\u00020\u00032\u0007\u0010\u009e\u0001\u001a\u000202J\u001c\u0010\u009b\u0001\u001a\u00020\u00152\u0013\u0010\u009f\u0001\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u0005J\u0011\u0010\u00a0\u0001\u001a\u00030\u009c\u00012\u0007\u0010\u00a1\u0001\u001a\u000205J\u0011\u0010\u00a2\u0001\u001a\u00030\u009c\u00012\u0007\u0010\u00a3\u0001\u001a\u00020yJ\u0012\u0010\u00a4\u0001\u001a\u00030\u009c\u00012\b\u0010\u00a5\u0001\u001a\u00030\u00a6\u0001J\u0007\u0010\u00a7\u0001\u001a\u000202J\b\u0010\u00a8\u0001\u001a\u00030\u009c\u0001J\b\u0010\u00a9\u0001\u001a\u00030\u009c\u0001J\u001d\u0010\u00aa\u0001\u001a\u00030\u009c\u00012\n\u0010\u009f\u0001\u001a\u0005\u0018\u00010\u00ab\u00012\u0007\u0010\u00ac\u0001\u001a\u000202J\'\u0010\u00ad\u0001\u001a\u00030\u009c\u00012\u0007\u0010\u00ae\u0001\u001a\u00020\u00032\b\u0010\u00af\u0001\u001a\u00030\u00ab\u00012\n\u0010\u00b0\u0001\u001a\u0005\u0018\u00010\u00b1\u0001J\u0015\u0010\u00b2\u0001\u001a\u00030\u009c\u00012\t\u0010\u009f\u0001\u001a\u0004\u0018\u00010\u0015H\u0002J\u0014\u0010\u00b3\u0001\u001a\u00030\u009c\u00012\b\u0010\u0097\u0001\u001a\u00030\u00ab\u0001H\u0002J\b\u0010\u00b4\u0001\u001a\u00030\u009c\u0001J\u0013\u0010\u00b5\u0001\u001a\u00030\u009c\u00012\u0007\u0010\u00b6\u0001\u001a\u00020\u001fH\u0002J\u0011\u0010\u00b7\u0001\u001a\u00030\u009c\u00012\u0007\u0010\u00b6\u0001\u001a\u00020\u001fJ\u0013\u0010\u00b8\u0001\u001a\u00030\u009c\u00012\t\u0010\u00b6\u0001\u001a\u0004\u0018\u00010\u001fJ\b\u0010\u00b9\u0001\u001a\u00030\u009c\u0001J\u0015\u0010\u00ba\u0001\u001a\u00030\u009c\u00012\t\u0010\u009f\u0001\u001a\u0004\u0018\u00010\u0015H\u0002J\u0012\u0010\u00bb\u0001\u001a\u00020\u001f2\u0007\u0010\u00bc\u0001\u001a\u00020\u0003H\u0002J\u0014\u0010\u00bd\u0001\u001a\u0004\u0018\u00010\u001f2\t\u0010\u0097\u0001\u001a\u0004\u0018\u00010\u0003J\u0015\u0010\u00bd\u0001\u001a\u0004\u0018\u00010\u001f2\n\u0010\u0097\u0001\u001a\u0005\u0018\u00010\u00ab\u0001J\u0014\u0010\u00be\u0001\u001a\u0004\u0018\u00010\u00152\t\u0010\u00bf\u0001\u001a\u0004\u0018\u00010\u0003J\u0010\u0010\u00c0\u0001\u001a\u00020\u00152\u0007\u0010\u00bc\u0001\u001a\u00020\u0003J\u0011\u0010\u00c0\u0001\u001a\u00020\u00152\b\u0010\u0097\u0001\u001a\u00030\u00ab\u0001J\u0014\u0010\u00c1\u0001\u001a\u0004\u0018\u00010\u001f2\u0007\u0010\u00c2\u0001\u001a\u00020\u0003H\u0002J\r\u0010\u00c3\u0001\u001a\b\u0012\u0004\u0012\u00020\u001f0\u0019J\r\u0010\u00c4\u0001\u001a\b\u0012\u0004\u0012\u00020\u001f0\u001aJ\u0013\u0010\u00c5\u0001\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001f0\u00070\u0019J\u001e\u0010\u00c6\u0001\u001a\u0010\u0012\f\u0012\n\u0012\u0005\u0012\u00030\u00c7\u00010\u008d\u00010\u00192\u0007\u0010\u00c8\u0001\u001a\u000202J\u0012\u0010\u00c9\u0001\u001a\u0004\u0018\u00010t2\u0007\u0010\u009d\u0001\u001a\u00020\u0003J\u0013\u0010\u00ca\u0001\u001a\u00020\u00032\b\u0010\u00bc\u0001\u001a\u00030\u00cb\u0001H\u0002J\u0011\u0010\u00cc\u0001\u001a\u0002022\b\u0010\u00bc\u0001\u001a\u00030\u00cb\u0001J\u0010\u0010F\u001a\u00020\u00032\b\u0010\u00cd\u0001\u001a\u00030\u00ce\u0001J\u001c\u0010n\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020*0\u00190\u00192\b\u0010\u00cf\u0001\u001a\u00030\u00ab\u0001J\r\u0010\u00d0\u0001\u001a\b\u0012\u0004\u0012\u00020\u001f0\u001aJ\u0013\u0010\u00d1\u0001\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001f0\u00070\u0019J\u0013\u0010\u00d2\u0001\u001a\u0004\u0018\u00010y2\b\u0010\u0097\u0001\u001a\u00030\u00ab\u0001J\u000f\u0010\u00d3\u0001\u001a\b\u0012\u0004\u0012\u00020\u001f0\u0007H\u0002J\u000f\u0010\u00d4\u0001\u001a\b\u0012\u0004\u0012\u00020\u001f0\u0007H\u0002J\u0012\u0010\u00d5\u0001\u001a\u0004\u0018\u00010\u001f2\u0007\u0010\u00ae\u0001\u001a\u00020\u0003J\u0014\u0010\u0098\u0001\u001a\u0004\u0018\u00010\u00032\u0007\u0010\u00d6\u0001\u001a\u000202H\u0002J\u0007\u0010\u00d7\u0001\u001a\u000202J\u0007\u0010\u00d8\u0001\u001a\u000202J\u0010\u0010\u00d9\u0001\u001a\u0002022\u0007\u0010\u00b6\u0001\u001a\u00020\u001fJ\u0010\u0010\u00da\u0001\u001a\u0002022\u0007\u0010\u0097\u0001\u001a\u00020\u0003J\b\u0010\u00db\u0001\u001a\u00030\u009c\u0001J\u0007\u0010\u00dc\u0001\u001a\u000202J\u001c\u0010\u00dd\u0001\u001a\u00020\u001f2\u0007\u0010\u00ae\u0001\u001a\u00020\u00032\n\u0010\u00de\u0001\u001a\u0005\u0018\u00010\u00df\u0001J\u0010\u0010\u00e0\u0001\u001a\u00020\u001f2\u0007\u0010\u00e1\u0001\u001a\u00020tJ\u0012\u0010\u00e2\u0001\u001a\u00030\u00e3\u00012\b\u0010\u00e4\u0001\u001a\u00030\u00e5\u0001J\n\u0010\u00e6\u0001\u001a\u00030\u009c\u0001H\u0002J\n\u0010\u00e7\u0001\u001a\u00030\u009c\u0001H\u0002J\u0015\u0010\u00e8\u0001\u001a\u00030\u009c\u00012\t\u0010\u00b6\u0001\u001a\u0004\u0018\u00010\u001fH\u0002J\u001a\u0010\u00e9\u0001\u001a\u00030\u009c\u00012\u0007\u0010\u00af\u0001\u001a\u00020\u00032\u0007\u0010\u00ea\u0001\u001a\u000202J\u001a\u0010\u00eb\u0001\u001a\u00030\u009c\u00012\u0007\u0010\u00ec\u0001\u001a\u00020\u00032\u0007\u0010\u00e1\u0001\u001a\u00020tJ\u0011\u0010\u00ed\u0001\u001a\u00030\u009c\u00012\u0007\u0010\u00b6\u0001\u001a\u00020\u001fJ%\u0010\u00ee\u0001\u001a\u0002022\b\u0010\u00ef\u0001\u001a\u00030\u0091\u00012\u0007\u0010\u00f0\u0001\u001a\u00020\u00032\t\u0010\u00f1\u0001\u001a\u0004\u0018\u00010\u0003J\u001a\u0010\u00f2\u0001\u001a\u00030\u009c\u00012\u0007\u0010\u009d\u0001\u001a\u00020\u00032\u0007\u0010\u00f3\u0001\u001a\u000202J\u0011\u0010\u00f4\u0001\u001a\u00030\u009c\u00012\u0007\u0010\u00f5\u0001\u001a\u000205J\u0013\u0010\u00f6\u0001\u001a\u0004\u0018\u00010y2\b\u0010\u00f7\u0001\u001a\u00030\u00ab\u0001J\u0011\u0010\u00f8\u0001\u001a\u00030\u009c\u00012\u0007\u0010\u00ae\u0001\u001a\u00020\u0003J\u0011\u0010\u00f9\u0001\u001a\u00030\u009c\u00012\u0007\u0010\u00ae\u0001\u001a\u00020\u0003J\"\u0010\u00fa\u0001\u001a\u00030\u009c\u00012\u0018\u0010+\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u00050\u0007J#\u0010\u00fb\u0001\u001a\u00030\u009c\u00012\u0019\u0010\u00fc\u0001\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u00050\u0007J\u001b\u0010\u00fd\u0001\u001a\u00030\u009c\u00012\b\u0010\u00bc\u0001\u001a\u00030\u00cb\u00012\u0007\u0010\u00fe\u0001\u001a\u000202J\u001d\u0010\u00fd\u0001\u001a\u00030\u009c\u00012\b\u0010\u00bc\u0001\u001a\u00030\u00cb\u00012\t\u0010\u00fe\u0001\u001a\u0004\u0018\u00010\u0003J\u001c\u0010\u00ff\u0001\u001a\u00030\u009c\u00012\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u0005J\u0016\u0010\u0080\u0002\u001a\u00030\u009c\u00012\f\u00100\u001a\b\u0012\u0004\u0012\u00020\u001f0\u0007J\u001b\u0010\u0081\u0002\u001a\u00030\u009c\u00012\u0007\u0010\u0082\u0002\u001a\u00020\u00032\b\u0010\u0083\u0002\u001a\u00030\u0091\u0001J\u001d\u0010\u0084\u0002\u001a\u00030\u009c\u00012\u0013\u0010\u0085\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u0005J\n\u0010\u0086\u0002\u001a\u00030\u009c\u0001H\u0002J\n\u0010\u0087\u0002\u001a\u00030\u009c\u0001H\u0002J\u0011\u0010\u0088\u0002\u001a\u00030\u009c\u00012\u0007\u0010\u00b6\u0001\u001a\u00020\u001fR\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00030\u000b8F\u00a2\u0006\u0006\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR(\u0010\u0010\u001a\u0004\u0018\u00010\u00032\b\u0010\u0010\u001a\u0004\u0018\u00010\u00038F@FX\u0086\u000e\u00a2\u0006\f\u001a\u0004\b\u0011\u0010\u000f\"\u0004\b\u0012\u0010\u0013R\u0017\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u00078F\u00a2\u0006\u0006\u001a\u0004\b\u0016\u0010\u0017R\u001d\u0010\u0018\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00150\u001a0\u00198F\u00a2\u0006\u0006\u001a\u0004\b\u001b\u0010\u001cR\u001a\u0010\u001d\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u001f0\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001e\u0010\"\u001a\u00020!2\u0006\u0010 \u001a\u00020!@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010$RT\u0010%\u001aH\u0012\u0018\u0012\u0016\u0012\u0004\u0012\u00020\u0015 \'*\n\u0012\u0004\u0012\u00020\u0015\u0018\u00010\u001a0\u001a \'*#\u0012\u0018\u0012\u0016\u0012\u0004\u0012\u00020\u0015 \'*\n\u0012\u0004\u0012\u00020\u0015\u0018\u00010\u001a0\u001a\u0018\u00010&\u00a2\u0006\u0002\b(0&\u00a2\u0006\u0002\b(X\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010)\u001a\u0014\u0012\u0004\u0012\u00020\u0015\u0012\n\u0012\b\u0012\u0004\u0012\u00020*0\u00190\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010+\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00150\u00058F\u00a2\u0006\u0006\u001a\u0004\b,\u0010-R\u0014\u0010.\u001a\b\u0012\u0004\u0012\u00020\u001f0/X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u00100\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u001f0\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u00101\u001a\u000202X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u00103\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001f0\u00070/X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020504\u00a2\u0006\b\n\u0000\u001a\u0004\b6\u00107R#\u00108\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u00050\u00078F\u00a2\u0006\u0006\u001a\u0004\b9\u0010\u0017R\u001d\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030:8F\u00a2\u0006\u0006\u001a\u0004\b;\u0010<R\u0011\u0010=\u001a\u00020\u00038F\u00a2\u0006\u0006\u001a\u0004\b>\u0010\u000fR\u0011\u0010?\u001a\u00020\u00038F\u00a2\u0006\u0006\u001a\u0004\b@\u0010\u000fR&\u0010A\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u0005X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bB\u0010-\"\u0004\bC\u0010DR\u0013\u0010E\u001a\u0004\u0018\u00010\u00038F\u00a2\u0006\u0006\u001a\u0004\bF\u0010\u000fR\u0013\u0010G\u001a\u0004\u0018\u00010\u00038F\u00a2\u0006\u0006\u001a\u0004\bH\u0010\u000fR\u0011\u0010I\u001a\u00020\u00038F\u00a2\u0006\u0006\u001a\u0004\bJ\u0010\u000fR\u000e\u0010K\u001a\u000202X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\"\u0010L\u001a\n\u0012\u0004\u0012\u00020\u0000\u0018\u00010\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bM\u0010\r\"\u0004\bN\u0010OR(\u0010P\u001a\u0004\u0018\u00010\u00032\b\u0010P\u001a\u0004\u0018\u00010\u00038F@FX\u0086\u000e\u00a2\u0006\f\u001a\u0004\bQ\u0010\u000f\"\u0004\bR\u0010\u0013R\u0011\u0010S\u001a\u0002028F\u00a2\u0006\u0006\u001a\u0004\bS\u0010TR\u0011\u0010U\u001a\u0002028F\u00a2\u0006\u0006\u001a\u0004\bU\u0010TR$\u0010W\u001a\u0002022\u0006\u0010V\u001a\u0002028F@FX\u0086\u000e\u00a2\u0006\f\u001a\u0004\bW\u0010T\"\u0004\bX\u0010YR$\u0010[\u001a\u0002022\u0006\u0010Z\u001a\u0002028F@FX\u0086\u000e\u00a2\u0006\f\u001a\u0004\b[\u0010T\"\u0004\b\\\u0010YR\u0011\u0010]\u001a\u0002028F\u00a2\u0006\u0006\u001a\u0004\b]\u0010TR\u0011\u0010^\u001a\u0002028F\u00a2\u0006\u0006\u001a\u0004\b^\u0010TR\u0011\u0010_\u001a\u0002028F\u00a2\u0006\u0006\u001a\u0004\b_\u0010TR\u0011\u0010`\u001a\u0002028F\u00a2\u0006\u0006\u001a\u0004\b`\u0010TR\u0011\u0010a\u001a\u0002028F\u00a2\u0006\u0006\u001a\u0004\ba\u0010TR\u0011\u0010b\u001a\u0002028F\u00a2\u0006\u0006\u001a\u0004\bb\u0010TR\u0014\u0010c\u001a\u00020\u00038BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\bd\u0010\u000fR4\u0010g\u001a\n\u0012\u0004\u0012\u00020f\u0018\u00010\u000b2\u000e\u0010e\u001a\n\u0012\u0004\u0012\u00020f\u0018\u00010\u000b@FX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bh\u0010\r\"\u0004\bi\u0010OR\u0017\u0010j\u001a\b\u0012\u0004\u0012\u00020f0\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\bk\u0010\u001cR\u0017\u0010l\u001a\b\u0012\u0004\u0012\u00020m0\u00198F\u00a2\u0006\u0006\u001a\u0004\bn\u0010\u001cR)\u0010o\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\u0015\u0012\n\u0012\b\u0012\u0004\u0012\u00020*0\u00190\u00050\u00198F\u00a2\u0006\u0006\u001a\u0004\bp\u0010\u001cR\u001a\u0010q\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00150\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010r\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00150\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010s\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020t0:X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010u\u001a\b\u0012\u0004\u0012\u00020m0/X\u0082\u0004\u00a2\u0006\u0002\n\u0000R&\u0010v\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\u0015\u0012\n\u0012\b\u0012\u0004\u0012\u00020*0\u00190\u00050/X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010w\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020f0\u000b0/X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010x\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020y0\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010z\u001a\u00020!X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010{\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u001f0\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010|\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001f0\u00070/X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010}\u001a\u000202X\u0082\u000e\u00a2\u0006\u0002\n\u0000R)\u0010~\u001a\u0004\u0018\u00010\u00032\b\u0010~\u001a\u0004\u0018\u00010\u00038F@FX\u0086\u000e\u00a2\u0006\r\u001a\u0004\b\u007f\u0010\u000f\"\u0005\b\u0080\u0001\u0010\u0013R\u0013\u0010\u0081\u0001\u001a\u00020\u00038F\u00a2\u0006\u0007\u001a\u0005\b\u0082\u0001\u0010\u000fR\u001d\u0010\u0083\u0001\u001a\u000202X\u0086\u000e\u00a2\u0006\u0010\n\u0000\u001a\u0005\b\u0084\u0001\u0010T\"\u0005\b\u0085\u0001\u0010YR\u0013\u0010\u0086\u0001\u001a\u00020\u00038F\u00a2\u0006\u0007\u001a\u0005\b\u0087\u0001\u0010\u000fR5\u0010\u0088\u0001\u001a\b\u0012\u0004\u0012\u00020y0\u00072\r\u0010\u0088\u0001\u001a\b\u0012\u0004\u0012\u00020y0\u00078F@FX\u0086\u000e\u00a2\u0006\u000f\u001a\u0005\b\u0089\u0001\u0010\u0017\"\u0006\b\u008a\u0001\u0010\u008b\u0001R\u0016\u0010\u008c\u0001\u001a\t\u0012\u0004\u0012\u00020\u001f0\u008d\u0001X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u008e\u0001\u001a\t\u0012\u0004\u0012\u00020\u001f0\u008d\u0001X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u008f\u0001\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u001f0\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0090\u0001\u001a\t\u0012\u0005\u0012\u00030\u0091\u00010\u0019\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0092\u0001\u0010\u001cR\u0016\u0010\u0093\u0001\u001a\t\u0012\u0005\u0012\u00030\u0091\u00010/X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0094\u0001\u001a\t\u0012\u0005\u0012\u00030\u0091\u00010\u0019\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0095\u0001\u0010\u001cR\u0016\u0010\u0096\u0001\u001a\t\u0012\u0005\u0012\u00030\u0091\u00010/X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0015\u0010\u0097\u0001\u001a\u0004\u0018\u00010\u00038F\u00a2\u0006\u0007\u001a\u0005\b\u0098\u0001\u0010\u000fR$\u0010\u0099\u0001\u001a\u0004\u0018\u00010\u00032\b\u0010 \u001a\u0004\u0018\u00010\u0003@BX\u0086\u000e\u00a2\u0006\t\n\u0000\u001a\u0005\b\u009a\u0001\u0010\u000f\u00a8\u0006\u008e\u0002"}, d2 = {"Lnet/jami/model/Account;", "", "accountID", "", "details", "", "credentials", "", "volDetails", "(Ljava/lang/String;Ljava/util/Map;Ljava/util/List;Ljava/util/Map;)V", "accountAlias", "Lio/reactivex/rxjava3/core/Single;", "getAccountAlias", "()Lio/reactivex/rxjava3/core/Single;", "getAccountID", "()Ljava/lang/String;", "alias", "getAlias", "setAlias", "(Ljava/lang/String;)V", "bannedContacts", "Lnet/jami/model/Contact;", "getBannedContacts", "()Ljava/util/List;", "bannedContactsUpdates", "Lio/reactivex/rxjava3/core/Observable;", "", "getBannedContactsUpdates", "()Lio/reactivex/rxjava3/core/Observable;", "cache", "", "Lnet/jami/model/Conversation;", "<set-?>", "Lnet/jami/model/AccountConfig;", "config", "getConfig", "()Lnet/jami/model/AccountConfig;", "contactListSubject", "Lio/reactivex/rxjava3/subjects/BehaviorSubject;", "kotlin.jvm.PlatformType", "Lio/reactivex/rxjava3/annotations/NonNull;", "contactLocations", "Lnet/jami/model/Account$ContactLocation;", "contacts", "getContacts", "()Ljava/util/Map;", "conversationSubject", "Lio/reactivex/rxjava3/subjects/Subject;", "conversations", "conversationsChanged", "", "conversationsSubject", "Ljava/util/ArrayList;", "Lnet/jami/model/AccountCredentials;", "getCredentials", "()Ljava/util/ArrayList;", "credentialsHashMapList", "getCredentialsHashMapList", "Ljava/util/HashMap;", "getDetails", "()Ljava/util/HashMap;", "deviceId", "getDeviceId", "deviceName", "getDeviceName", "devices", "getDevices", "setDevices", "(Ljava/util/Map;)V", "displayUri", "getDisplayUri", "displayUsername", "getDisplayUsername", "displayname", "getDisplayname", "historyLoaded", "historyLoader", "getHistoryLoader", "setHistoryLoader", "(Lio/reactivex/rxjava3/core/Single;)V", "host", "getHost", "setHost", "isActive", "()Z", "isAutoanswerEnabled", "active", "isDhtProxyEnabled", "setDhtProxyEnabled", "(Z)V", "isChecked", "isEnabled", "setEnabled", "isIP2IP", "isInError", "isJami", "isRegistered", "isSip", "isTrying", "jamiAlias", "getJamiAlias", "profile", "Lnet/jami/model/Profile;", "loadedProfile", "getLoadedProfile", "setLoadedProfile", "loadedProfileObservable", "getLoadedProfileObservable", "locationUpdates", "Lnet/jami/model/Account$ContactLocationEntry;", "getLocationUpdates", "locationsUpdates", "getLocationsUpdates", "mContactCache", "mContacts", "mDataTransfers", "Lnet/jami/model/DataTransfer;", "mLocationStartedSubject", "mLocationSubject", "mProfileSubject", "mRequests", "Lnet/jami/model/TrustRequest;", "mVolatileDetails", "pending", "pendingSubject", "pendingsChanged", "proxy", "getProxy", "setProxy", "registeredName", "getRegisteredName", "registeringUsername", "getRegisteringUsername", "setRegisteringUsername", "registrationState", "getRegistrationState", "requests", "getRequests", "setRequests", "(Ljava/util/List;)V", "sortedConversations", "", "sortedPending", "swarmConversations", "unreadConversations", "", "getUnreadConversations", "unreadConversationsSubject", "unreadPending", "getUnreadPending", "unreadPendingSubject", "uri", "getUri", "username", "getUsername", "addContact", "", "id", "confirmed", "contact", "addCredential", "newValue", "addRequest", "request", "addTextMessage", "txt", "Lnet/jami/model/TextMessage;", "canSearch", "cleanup", "clearAllHistory", "clearHistory", "Lnet/jami/model/Uri;", "delete", "composingStatusChanged", "conversationId", "contactUri", "status", "Lnet/jami/model/Account$ComposingStatus;", "contactAdded", "contactRemoved", "conversationChanged", "conversationRefreshed", "conversation", "conversationStarted", "conversationUpdated", "dispose", "forceExpireContact", "getByKey", "key", "getByUri", "getContact", "ringId", "getContactFromCache", "getConversationByCallId", "callId", "getConversationSubject", "getConversations", "getConversationsSubject", "getConversationsViewModels", "Lnet/jami/smartlist/SmartListViewModel;", "withPresence", "getDataTransfer", "getDetail", "Lnet/jami/model/ConfigKey;", "getDetailBoolean", "defaultNameSip", "", "contactId", "getPending", "getPendingSubject", "getRequest", "getSortedConversations", "getSortedPending", "getSwarm", "display", "hasManager", "hasPassword", "isContact", "isMe", "maintainLocation", "needsMigration", "newSwarm", "mode", "Lnet/jami/model/Conversation$Mode;", "onDataTransferEvent", "transfer", "onLocationUpdate", "", "location", "Lnet/jami/services/AccountService$Location;", "pendingChanged", "pendingRefreshed", "pendingUpdated", "presenceUpdate", "isOnline", "putDataTransfer", "fileId", "refreshed", "registeredNameFound", "state", "address", "name", "removeContact", "banned", "removeCredential", "accountCredentials", "removeRequest", "conversationUri", "removeRequestPerConvId", "removeSwarm", "setContacts", "setCredentials", "creds", "setDetail", "value", "setDetails", "setHistoryLoaded", "setRegistrationState", "registeredState", "code", "setVolatileDetails", "volatileDetails", "updateUnreadConversations", "updateUnreadPending", "updated", "Companion", "ComposingStatus", "ContactLocation", "ContactLocationEntry", "ConversationComparator", "libringclient"})
public final class Account {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String accountID = null;
    private net.jami.model.AccountConfig mVolatileDetails;
    @org.jetbrains.annotations.NotNull()
    private net.jami.model.AccountConfig config;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String username;
    @org.jetbrains.annotations.NotNull()
    private final java.util.ArrayList<net.jami.model.AccountCredentials> credentials = null;
    @org.jetbrains.annotations.NotNull()
    private java.util.Map<java.lang.String, java.lang.String> devices;
    private final java.util.Map<java.lang.String, net.jami.model.Contact> mContacts = null;
    private final java.util.Map<java.lang.String, net.jami.model.TrustRequest> mRequests = null;
    private final java.util.Map<java.lang.String, net.jami.model.Contact> mContactCache = null;
    private final java.util.Map<java.lang.String, net.jami.model.Conversation> swarmConversations = null;
    private final java.util.HashMap<java.lang.String, net.jami.model.DataTransfer> mDataTransfers = null;
    private final java.util.Map<java.lang.String, net.jami.model.Conversation> conversations = null;
    private final java.util.Map<java.lang.String, net.jami.model.Conversation> pending = null;
    private final java.util.Map<java.lang.String, net.jami.model.Conversation> cache = null;
    private final java.util.List<net.jami.model.Conversation> sortedConversations = null;
    private final java.util.List<net.jami.model.Conversation> sortedPending = null;
    private boolean registeringUsername = false;
    private boolean conversationsChanged = true;
    private boolean pendingsChanged = true;
    private boolean historyLoaded = false;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.model.Conversation> conversationSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<java.util.List<net.jami.model.Conversation>> conversationsSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<java.util.List<net.jami.model.Conversation>> pendingSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<java.lang.Integer> unreadConversationsSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<java.lang.Integer> unreadPendingSubject = null;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.core.Observable<java.lang.Integer> unreadConversations = null;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.core.Observable<java.lang.Integer> unreadPending = null;
    private final io.reactivex.rxjava3.subjects.BehaviorSubject<java.util.Collection<net.jami.model.Contact>> contactListSubject = null;
    private final java.util.Map<net.jami.model.Contact, io.reactivex.rxjava3.core.Observable<net.jami.model.Account.ContactLocation>> contactLocations = null;
    private final io.reactivex.rxjava3.subjects.Subject<java.util.Map<net.jami.model.Contact, io.reactivex.rxjava3.core.Observable<net.jami.model.Account.ContactLocation>>> mLocationSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.model.Account.ContactLocationEntry> mLocationStartedSubject = null;
    @org.jetbrains.annotations.Nullable()
    private io.reactivex.rxjava3.core.Single<net.jami.model.Account> historyLoader;
    @org.jetbrains.annotations.Nullable()
    private io.reactivex.rxjava3.core.Single<net.jami.model.Profile> loadedProfile;
    private final io.reactivex.rxjava3.subjects.Subject<io.reactivex.rxjava3.core.Single<net.jami.model.Profile>> mProfileSubject = null;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.core.Observable<net.jami.model.Profile> loadedProfileObservable = null;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.model.Account.Companion Companion = null;
    private static final java.lang.String TAG = null;
    private static final java.lang.String CONTACT_ADDED = "added";
    private static final java.lang.String CONTACT_CONFIRMED = "confirmed";
    private static final java.lang.String CONTACT_BANNED = "banned";
    private static final java.lang.String CONTACT_ID = "id";
    private static final java.lang.String CONTACT_CONVERSATION = "conversationId";
    private static final int LOCATION_SHARING_EXPIRATION_MS = 120000;
    
    public Account(@org.jetbrains.annotations.NotNull()
    java.lang.String accountID, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> details, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends java.util.Map<java.lang.String, java.lang.String>> credentials, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> volDetails) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getAccountID() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.AccountConfig getConfig() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUsername() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.ArrayList<net.jami.model.AccountCredentials> getCredentials() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, java.lang.String> getDevices() {
        return null;
    }
    
    public final void setDevices(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> p0) {
    }
    
    public final boolean getRegisteringUsername() {
        return false;
    }
    
    public final void setRegisteringUsername(boolean p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.lang.Integer> getUnreadConversations() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.lang.Integer> getUnreadPending() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.Account> getHistoryLoader() {
        return null;
    }
    
    public final void setHistoryLoader(@org.jetbrains.annotations.Nullable()
    io.reactivex.rxjava3.core.Single<net.jami.model.Account> p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.Profile> getLoadedProfile() {
        return null;
    }
    
    public final void setLoadedProfile(@org.jetbrains.annotations.Nullable()
    io.reactivex.rxjava3.core.Single<net.jami.model.Profile> profile) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Profile> getLoadedProfileObservable() {
        return null;
    }
    
    public final void cleanup() {
    }
    
    public final boolean canSearch() {
        return false;
    }
    
    public final boolean isContact(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation) {
        return false;
    }
    
    public final void conversationStarted(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Conversation getSwarm(@org.jetbrains.annotations.NotNull()
    java.lang.String conversationId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Conversation newSwarm(@org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.Nullable()
    net.jami.model.Conversation.Mode mode) {
        return null;
    }
    
    public final void removeSwarm(@org.jetbrains.annotations.NotNull()
    java.lang.String conversationId) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<net.jami.model.Conversation>> getConversationsSubject() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<net.jami.smartlist.SmartListViewModel>> getConversationsViewModels(boolean withPresence) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Conversation> getConversationSubject() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<net.jami.model.Conversation>> getPendingSubject() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Collection<net.jami.model.Conversation> getConversations() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Collection<net.jami.model.Conversation> getPending() {
        return null;
    }
    
    private final void pendingRefreshed() {
    }
    
    private final void pendingChanged() {
    }
    
    private final void pendingUpdated(net.jami.model.Conversation conversation) {
    }
    
    private final void conversationRefreshed(net.jami.model.Conversation conversation) {
    }
    
    public final void conversationChanged() {
    }
    
    public final void conversationUpdated(@org.jetbrains.annotations.Nullable()
    net.jami.model.Conversation conversation) {
    }
    
    private final void updateUnreadConversations() {
    }
    
    private final void updateUnreadPending() {
    }
    
    /**
     * Clears a conversation
     *
     * @param contact the contact
     * @param delete  true if you want to remove the conversation
     */
    public final void clearHistory(@org.jetbrains.annotations.Nullable()
    net.jami.model.Uri contact, boolean delete) {
    }
    
    public final void clearAllHistory() {
    }
    
    public final void updated(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation) {
    }
    
    public final void refreshed(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation) {
    }
    
    public final void addTextMessage(@org.jetbrains.annotations.NotNull()
    net.jami.model.TextMessage txt) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Conversation onDataTransferEvent(@org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer transfer) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.Collection<net.jami.model.Contact>> getBannedContactsUpdates() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Contact getContactFromCache(@org.jetbrains.annotations.NotNull()
    java.lang.String key) {
        return null;
    }
    
    public final boolean isMe(@org.jetbrains.annotations.NotNull()
    java.lang.String uri) {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Contact getContactFromCache(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri) {
        return null;
    }
    
    public final void dispose() {
    }
    
    public final void setCredentials(@org.jetbrains.annotations.NotNull()
    java.util.List<? extends java.util.Map<java.lang.String, java.lang.String>> creds) {
    }
    
    public final void setDetails(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> details) {
    }
    
    public final void setDetail(@org.jetbrains.annotations.NotNull()
    net.jami.model.ConfigKey key, @org.jetbrains.annotations.Nullable()
    java.lang.String value) {
    }
    
    public final void setDetail(@org.jetbrains.annotations.NotNull()
    net.jami.model.ConfigKey key, boolean value) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDisplayname() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getDisplayUsername() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getHost() {
        return null;
    }
    
    public final void setHost(@org.jetbrains.annotations.Nullable()
    java.lang.String host) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getProxy() {
        return null;
    }
    
    public final void setProxy(@org.jetbrains.annotations.Nullable()
    java.lang.String proxy) {
    }
    
    public final boolean isDhtProxyEnabled() {
        return false;
    }
    
    public final void setDhtProxyEnabled(boolean active) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getRegistrationState() {
        return null;
    }
    
    public final void setRegistrationState(@org.jetbrains.annotations.NotNull()
    java.lang.String registeredState, int code) {
    }
    
    public final void setVolatileDetails(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> volatileDetails) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getRegisteredName() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAlias() {
        return null;
    }
    
    public final void setAlias(@org.jetbrains.annotations.Nullable()
    java.lang.String alias) {
    }
    
    public final boolean isSip() {
        return false;
    }
    
    public final boolean isJami() {
        return false;
    }
    
    private final java.lang.String getDetail(net.jami.model.ConfigKey key) {
        return null;
    }
    
    public final boolean getDetailBoolean(@org.jetbrains.annotations.NotNull()
    net.jami.model.ConfigKey key) {
        return false;
    }
    
    public final boolean isEnabled() {
        return false;
    }
    
    public final void setEnabled(boolean isChecked) {
    }
    
    public final boolean isActive() {
        return false;
    }
    
    public final boolean hasPassword() {
        return false;
    }
    
    public final boolean hasManager() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.HashMap<java.lang.String, java.lang.String> getDetails() {
        return null;
    }
    
    public final boolean isTrying() {
        return false;
    }
    
    public final boolean isRegistered() {
        return false;
    }
    
    public final boolean isInError() {
        return false;
    }
    
    public final boolean isIP2IP() {
        return false;
    }
    
    public final boolean isAutoanswerEnabled() {
        return false;
    }
    
    public final void addCredential(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCredentials newValue) {
    }
    
    public final void removeCredential(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCredentials accountCredentials) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.util.Map<java.lang.String, java.lang.String>> getCredentialsHashMapList() {
        return null;
    }
    
    private final java.lang.String getUri(boolean display) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUri() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getDisplayUri() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDisplayUri(@org.jetbrains.annotations.NotNull()
    java.lang.CharSequence defaultNameSip) {
        return null;
    }
    
    public final boolean needsMigration() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDeviceId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDeviceName() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, net.jami.model.Contact> getContacts() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<net.jami.model.Contact> getBannedContacts() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Contact getContact(@org.jetbrains.annotations.Nullable()
    java.lang.String ringId) {
        return null;
    }
    
    public final void addContact(@org.jetbrains.annotations.NotNull()
    java.lang.String id, boolean confirmed) {
    }
    
    public final void removeContact(@org.jetbrains.annotations.NotNull()
    java.lang.String id, boolean banned) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Contact addContact(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> contact) {
        return null;
    }
    
    public final void setContacts(@org.jetbrains.annotations.NotNull()
    java.util.List<? extends java.util.Map<java.lang.String, java.lang.String>> contacts) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<net.jami.model.TrustRequest> getRequests() {
        return null;
    }
    
    public final void setRequests(@org.jetbrains.annotations.NotNull()
    java.util.List<net.jami.model.TrustRequest> requests) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.TrustRequest getRequest(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri) {
        return null;
    }
    
    public final void addRequest(@org.jetbrains.annotations.NotNull()
    net.jami.model.TrustRequest request) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.TrustRequest removeRequest(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri) {
        return null;
    }
    
    public final void removeRequestPerConvId(@org.jetbrains.annotations.NotNull()
    java.lang.String conversationId) {
    }
    
    public final boolean registeredNameFound(int state, @org.jetbrains.annotations.NotNull()
    java.lang.String address, @org.jetbrains.annotations.Nullable()
    java.lang.String name) {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Conversation getByUri(@org.jetbrains.annotations.Nullable()
    net.jami.model.Uri uri) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Conversation getByUri(@org.jetbrains.annotations.Nullable()
    java.lang.String uri) {
        return null;
    }
    
    private final net.jami.model.Conversation getByKey(java.lang.String key) {
        return null;
    }
    
    public final void setHistoryLoaded(@org.jetbrains.annotations.NotNull()
    java.util.List<net.jami.model.Conversation> conversations) {
    }
    
    private final java.util.List<net.jami.model.Conversation> getSortedConversations() {
        return null;
    }
    
    private final java.util.List<net.jami.model.Conversation> getSortedPending() {
        return null;
    }
    
    private final void contactAdded(net.jami.model.Contact contact) {
    }
    
    private final void contactRemoved(net.jami.model.Uri uri) {
    }
    
    private final net.jami.model.Conversation getConversationByCallId(java.lang.String callId) {
        return null;
    }
    
    public final void presenceUpdate(@org.jetbrains.annotations.NotNull()
    java.lang.String contactUri, boolean isOnline) {
    }
    
    public final void composingStatusChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri contactUri, @org.jetbrains.annotations.Nullable()
    net.jami.model.Account.ComposingStatus status) {
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized long onLocationUpdate(@org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService.Location location) {
        return 0L;
    }
    
    @kotlin.jvm.Synchronized()
    private final synchronized void forceExpireContact(net.jami.model.Contact contact) {
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void maintainLocation() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Account.ContactLocationEntry> getLocationUpdates() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.Map<net.jami.model.Contact, io.reactivex.rxjava3.core.Observable<net.jami.model.Account.ContactLocation>>> getLocationsUpdates() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<io.reactivex.rxjava3.core.Observable<net.jami.model.Account.ContactLocation>> getLocationUpdates(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri contactId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<java.lang.String> getAccountAlias() {
        return null;
    }
    
    private final java.lang.String getJamiAlias() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.DataTransfer getDataTransfer(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
        return null;
    }
    
    public final void putDataTransfer(@org.jetbrains.annotations.NotNull()
    java.lang.String fileId, @org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer transfer) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\u0018\u00002\u00020\u0001B%\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u00a2\u0006\u0002\u0010\tR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\u000bR\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010\u00a8\u0006\u0011"}, d2 = {"Lnet/jami/model/Account$ContactLocation;", "", "latitude", "", "longitude", "timestamp", "", "receivedDate", "Ljava/util/Date;", "(DDJLjava/util/Date;)V", "getLatitude", "()D", "getLongitude", "getReceivedDate", "()Ljava/util/Date;", "getTimestamp", "()J", "libringclient"})
    public static final class ContactLocation {
        private final double latitude = 0.0;
        private final double longitude = 0.0;
        private final long timestamp = 0L;
        @org.jetbrains.annotations.NotNull()
        private final java.util.Date receivedDate = null;
        
        public ContactLocation(double latitude, double longitude, long timestamp, @org.jetbrains.annotations.NotNull()
        java.util.Date receivedDate) {
            super();
        }
        
        public final double getLatitude() {
            return 0.0;
        }
        
        public final double getLongitude() {
            return 0.0;
        }
        
        public final long getTimestamp() {
            return 0L;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.Date getReceivedDate() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u00002\u00020\u0001B\u001b\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\u0002\u0010\u0007R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\f"}, d2 = {"Lnet/jami/model/Account$ContactLocationEntry;", "", "contact", "Lnet/jami/model/Contact;", "location", "Lio/reactivex/rxjava3/core/Observable;", "Lnet/jami/model/Account$ContactLocation;", "(Lnet/jami/model/Contact;Lio/reactivex/rxjava3/core/Observable;)V", "getContact", "()Lnet/jami/model/Contact;", "getLocation", "()Lio/reactivex/rxjava3/core/Observable;", "libringclient"})
    public static final class ContactLocationEntry {
        @org.jetbrains.annotations.NotNull()
        private final net.jami.model.Contact contact = null;
        @org.jetbrains.annotations.NotNull()
        private final io.reactivex.rxjava3.core.Observable<net.jami.model.Account.ContactLocation> location = null;
        
        public ContactLocationEntry(@org.jetbrains.annotations.NotNull()
        net.jami.model.Contact contact, @org.jetbrains.annotations.NotNull()
        io.reactivex.rxjava3.core.Observable<net.jami.model.Account.ContactLocation> location) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final net.jami.model.Contact getContact() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.reactivex.rxjava3.core.Observable<net.jami.model.Account.ContactLocation> getLocation() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0005\b\u0086\u0001\u0018\u0000 \u00052\b\u0012\u0004\u0012\u00020\u00000\u0001:\u0001\u0005B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004\u00a8\u0006\u0006"}, d2 = {"Lnet/jami/model/Account$ComposingStatus;", "", "(Ljava/lang/String;I)V", "Idle", "Active", "Companion", "libringclient"})
    public static enum ComposingStatus {
        /*public static final*/ Idle /* = new Idle() */,
        /*public static final*/ Active /* = new Active() */;
        @org.jetbrains.annotations.NotNull()
        public static final net.jami.model.Account.ComposingStatus.Companion Companion = null;
        
        ComposingStatus() {
        }
        
        @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006\u00a8\u0006\u0007"}, d2 = {"Lnet/jami/model/Account$ComposingStatus$Companion;", "", "()V", "fromInt", "Lnet/jami/model/Account$ComposingStatus;", "status", "", "libringclient"})
        public static final class Companion {
            
            private Companion() {
                super();
            }
            
            @org.jetbrains.annotations.NotNull()
            public final net.jami.model.Account.ComposingStatus fromInt(int status) {
                return null;
            }
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\b\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0003J\u0018\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016\u00a8\u0006\b"}, d2 = {"Lnet/jami/model/Account$ConversationComparator;", "Ljava/util/Comparator;", "Lnet/jami/model/Conversation;", "()V", "compare", "", "a", "b", "libringclient"})
    static final class ConversationComparator implements java.util.Comparator<net.jami.model.Conversation> {
        
        public ConversationComparator() {
            super();
        }
        
        @java.lang.Override()
        public int compare(@org.jetbrains.annotations.NotNull()
        net.jami.model.Conversation a, @org.jetbrains.annotations.NotNull()
        net.jami.model.Conversation b) {
            return 0;
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lnet/jami/model/Account$Companion;", "", "()V", "CONTACT_ADDED", "", "CONTACT_BANNED", "CONTACT_CONFIRMED", "CONTACT_CONVERSATION", "CONTACT_ID", "LOCATION_SHARING_EXPIRATION_MS", "", "TAG", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}