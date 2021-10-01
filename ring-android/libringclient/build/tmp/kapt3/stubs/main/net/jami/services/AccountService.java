package net.jami.services;

import java.lang.System;

/**
 * This service handles the accounts
 * - Load and manage the accounts stored in the daemon
 * - Keep a local cache of the accounts
 * - handle the callbacks that are send by the daemon
 */
@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u00bc\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010!\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010$\n\u0002\b)\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b1\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0017\n\u0002\u0010\u001e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0014\u0018\u0000 \u0095\u00022\u00020\u0001:\u0016\u0095\u0002\u0096\u0002\u0097\u0002\u0098\u0002\u0099\u0002\u009a\u0002\u009b\u0002\u009c\u0002\u009d\u0002\u009e\u0002\u009f\u0002B%\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ(\u0010W\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010Z\u001a\u00020[2\b\u0010\\\u001a\u0004\u0018\u00010T2\u0006\u0010]\u001a\u00020TJ\u001e\u0010W\u001a\u00020X2\u0006\u0010^\u001a\u00020_2\u0006\u0010]\u001a\u00020T2\u0006\u0010`\u001a\u00020%J\u0016\u0010a\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010b\u001a\u00020[J\"\u0010c\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0012\u0010d\u001a\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0eJ.\u0010f\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010g\u001a\u00020T2\u0006\u0010\\\u001a\u00020T2\u0006\u0010h\u001a\u00020T2\u0006\u0010i\u001a\u00020\u0018J\"\u0010j\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\b\u0010k\u001a\u0004\u0018\u00010T2\b\u0010l\u001a\u0004\u0018\u00010TJ\u0006\u0010m\u001a\u00020XJ \u0010n\u001a\b\u0012\u0004\u0012\u00020\u000e0\u001c2\u0012\u0010o\u001a\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0eJ\u0016\u0010p\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010q\u001a\u00020TJ,\u0010r\u001a\u0002022\u0006\u0010\u0011\u001a\u00020\u000e2\u0006\u0010^\u001a\u00020_2\u0012\u0010s\u001a\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0eH\u0002J(\u0010t\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010g\u001a\u00020T2\b\u0010\\\u001a\u0004\u0018\u00010T2\u0006\u0010]\u001a\u00020TJ&\u0010u\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010g\u001a\u00020T2\u0006\u0010v\u001a\u00020T2\u0006\u0010i\u001a\u00020\u0018J\u001e\u0010w\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010q\u001a\u00020T2\u0006\u0010x\u001a\u00020?J\u001e\u0010y\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010q\u001a\u00020T2\u0006\u0010z\u001a\u00020?J0\u0010{\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010g\u001a\u00020T2\u0018\u0010|\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0e0\rJ&\u0010}\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010g\u001a\u00020T2\u0006\u0010~\u001a\u00020T2\u0006\u0010\u007f\u001a\u00020\u0018J\u0017\u0010\u0080\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010g\u001a\u00020TJ\u0017\u0010\u0081\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010g\u001a\u00020TJ\u0017\u0010\u0082\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010g\u001a\u00020TJ,\u0010\u0083\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010g\u001a\u00020T2\u0013\u0010\u0084\u0001\u001a\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0eJ1\u0010\u0085\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010g\u001a\u00020T2\u0007\u0010\u0086\u0001\u001a\u00020T2\u0006\u0010]\u001a\u00020T2\u0007\u0010\u0087\u0001\u001a\u00020\u0018J5\u0010\u0085\u0001\u001a\u00020X2\u0006\u0010\u0011\u001a\u00020\u000e2\b\u0010^\u001a\u0004\u0018\u00010_2\t\u0010\u0086\u0001\u001a\u0004\u0018\u00010T2\u0006\u0010]\u001a\u00020T2\u0007\u0010\u0087\u0001\u001a\u00020\u0018J!\u0010\u0088\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u0089\u0001\u001a\u00020T2\u0007\u0010\u008a\u0001\u001a\u00020\u0018J\u0017\u0010\u008b\u0001\u001a\u00020?2\u0006\u0010Y\u001a\u00020T2\u0006\u0010v\u001a\u00020[J\u0010\u0010\u008c\u0001\u001a\u00020X2\u0007\u0010\u008d\u0001\u001a\u00020\u0018J\u001f\u0010\u008e\u0001\u001a\t\u0012\u0004\u0012\u00020T0\u008f\u00012\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u0090\u0001\u001a\u00020TJ!\u0010\u0091\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u0092\u0001\u001a\u00020\u00182\u0007\u0010\u0093\u0001\u001a\u00020TJ\"\u0010\u0094\u0001\u001a\u00030\u0095\u00012\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u0096\u0001\u001a\u00020T2\u0007\u0010\u0090\u0001\u001a\u00020TJ\'\u0010\u0097\u0001\u001a\t\u0012\u0004\u0012\u00020L0\u008f\u00012\u0006\u0010\u0011\u001a\u00020T2\u0007\u0010\u0098\u0001\u001a\u00020T2\u0006\u0010k\u001a\u00020TJ\u0013\u0010\u0099\u0001\u001a\u0004\u0018\u00010\u000e2\b\u0010Y\u001a\u0004\u0018\u00010TJ\u0013\u0010\u009a\u0001\u001a\u0004\u0018\u00010\u000e2\u0006\u0010k\u001a\u00020TH\u0002J\u0016\u0010\u009b\u0001\u001a\t\u0012\u0004\u0012\u00020\u000e0\u008f\u00012\u0006\u0010Y\u001a\u00020TJ5\u0010\u009c\u0001\u001a\'\u0012\"\u0012 \u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0\u009d\u0001j\u000f\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T`\u009e\u00010\u008f\u00012\u0007\u0010\u009f\u0001\u001a\u00020TJ\u001e\u0010\u00a0\u0001\u001a\u0010\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T\u0018\u00010e2\u0007\u0010\u00a1\u0001\u001a\u00020TJ\u001e\u0010\u00a2\u0001\u001a\u0010\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T\u0018\u00010e2\u0007\u0010\u00a3\u0001\u001a\u00020TJ\u001d\u0010\u00a4\u0001\u001a\u0010\u0012\u000b\u0012\t\u0012\u0005\u0012\u00030\u00a5\u00010\r0\u008f\u00012\u0006\u0010Y\u001a\u00020TJ%\u0010\u00a6\u0001\u001a\u0016\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0e\u0018\u00010\r2\b\u0010Y\u001a\u0004\u0018\u00010TJ#\u0010\u00a7\u0001\u001a\u0016\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0e\u0018\u00010\r2\u0006\u0010Y\u001a\u00020TJ\u001b\u0010\u00a8\u0001\u001a\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0e2\u0006\u0010Y\u001a\u00020TJ\"\u0010\u00a9\u0001\u001a\t\u0012\u0005\u0012\u00030\u00aa\u00010\r2\b\u0010Y\u001a\u0004\u0018\u00010T2\b\u0010\u00ab\u0001\u001a\u00030\u00ac\u0001J\u0012\u0010\u00ad\u0001\u001a\u00020T2\t\u0010\u00ae\u0001\u001a\u0004\u0018\u00010TJ\u0015\u0010\u00af\u0001\u001a\b\u0012\u0004\u0012\u00020\u000e0\u001c2\u0006\u0010Y\u001a\u00020TJ\u0015\u0010\u00af\u0001\u001a\b\u0012\u0004\u0012\u00020\u000e0\u001c2\u0006\u0010\u0011\u001a\u00020\u000eJ!\u0010\u00b0\u0001\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020!0 0\u001c2\u0006\u0010Y\u001a\u00020TJ\u0015\u0010\u00b1\u0001\u001a\b\u0012\u0004\u0012\u00020\u000e0\u001c2\u0006\u0010Y\u001a\u00020TJ%\u0010\u00b2\u0001\u001a\u0016\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0e\u0018\u00010\r2\b\u0010Y\u001a\u0004\u0018\u00010TJ.\u0010\u00b3\u0001\u001a\u00020X2\u0006\u0010^\u001a\u00020_2\u0006\u0010v\u001a\u00020[2\t\u0010\u00b4\u0001\u001a\u0004\u0018\u00010.2\b\u0010\u00b5\u0001\u001a\u00030\u00b6\u0001H\u0002J\u0007\u0010\u00b7\u0001\u001a\u00020?J\u0007\u0010\u00b8\u0001\u001a\u00020?J@\u0010\u00b9\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\b\u0010\\\u001a\u0004\u0018\u00010T2\t\u0010\u00ba\u0001\u001a\u0004\u0018\u00010T2\u0006\u0010b\u001a\u00020T2\u0012\u0010|\u001a\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0eJ3\u0010\u00bb\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010g\u001a\u00020T2\u0006\u0010b\u001a\u00020T2\b\u0010s\u001a\u0004\u0018\u00010T2\b\u0010\u00bc\u0001\u001a\u00030\u00ac\u0001J$\u0010\u00bd\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0013\u0010\u00be\u0001\u001a\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0eJ\u0010\u0010\u00bf\u0001\u001a\u00020X2\u0007\u0010\u00c0\u0001\u001a\u00020?J*\u0010\u00c1\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010Z\u001a\u00020[2\u0007\u0010\u00c2\u0001\u001a\u00020T2\b\u0010\u00c3\u0001\u001a\u00030\u00ac\u0001J#\u0010\u00c4\u0001\u001a\t\u0012\u0004\u0012\u00020_0\u008f\u00012\u0006\u0010^\u001a\u00020_2\t\b\u0002\u0010\u00c3\u0001\u001a\u00020\u0018H\u0007J\'\u0010\u00c5\u0001\u001a\u00020X2\b\u0010\u0011\u001a\u0004\u0018\u00010T2\t\u0010\u0098\u0001\u001a\u0004\u0018\u00010T2\t\u0010\u00c6\u0001\u001a\u0004\u0018\u00010TJ \u0010\u00c7\u0001\u001a\u00020X2\u0006\u0010\u0011\u001a\u00020T2\u0007\u0010\u0098\u0001\u001a\u00020T2\u0006\u0010k\u001a\u00020TJ+\u0010\u00c8\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010g\u001a\u00020T2\u0012\u0010s\u001a\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0eJ!\u0010\u00c9\u0001\u001a\u000b\u0012\u0006\u0012\u0004\u0018\u00010T0\u008f\u00012\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u0090\u0001\u001a\u00020TJ\u0018\u0010\u00ca\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u008a\u0001\u001a\u00020TJ \u0010\u00cb\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u008a\u0001\u001a\u00020\u00182\u0006\u0010k\u001a\u00020TJ\u0017\u0010\u00cc\u0001\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010%0\u001c2\u0006\u0010`\u001a\u00020%J!\u0010\u00cd\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u00ce\u0001\u001a\u00020T2\u0007\u0010\u00cf\u0001\u001a\u00020TJ,\u0010\u00d0\u0001\u001a\u00020X2\b\u0010b\u001a\u0004\u0018\u00010T2\u0019\u0010\u00d1\u0001\u001a\u0014\u0012\u0006\u0012\u0004\u0018\u00010T\u0012\u0006\u0012\u0004\u0018\u00010T\u0018\u00010eJ\u0007\u0010\u00d2\u0001\u001a\u00020XJ\t\u0010\u00d3\u0001\u001a\u00020XH\u0002J\u0007\u0010\u00d4\u0001\u001a\u00020XJ&\u0010\u00d5\u0001\u001a\u00020X2\b\u0010\u0011\u001a\u0004\u0018\u00010T2\t\u0010\u0090\u0001\u001a\u0004\u0018\u00010T2\b\u0010k\u001a\u0004\u0018\u00010TJ$\u0010\u00d5\u0001\u001a\u00020X2\u0006\u0010\u0011\u001a\u00020\u000e2\t\u0010\u0090\u0001\u001a\u0004\u0018\u00010T2\b\u0010k\u001a\u0004\u0018\u00010TJ)\u0010\u00d6\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u008a\u0001\u001a\u00020\u00182\u0007\u0010\u00c6\u0001\u001a\u00020T2\u0006\u0010k\u001a\u00020TJ,\u0010\u00d7\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u00d8\u0001\u001a\u00020T2\u0007\u0010\u0092\u0001\u001a\u00020\u00182\t\u0010\u00d9\u0001\u001a\u0004\u0018\u00010TJ\u000f\u0010\u00da\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020TJ \u0010\u00db\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010q\u001a\u00020T2\u0007\u0010\u00dc\u0001\u001a\u00020?J\u0018\u0010\u00dd\u0001\u001a\u00030\u0095\u00012\u0006\u0010Y\u001a\u00020T2\u0006\u0010Z\u001a\u00020[J\u0018\u0010\u00de\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u00df\u0001\u001a\u00020TJ(\u0010\u00e0\u0001\u001a\t\u0012\u0004\u0012\u00020\u00180\u008f\u00012\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u0090\u0001\u001a\u00020T2\u0007\u0010\u00e1\u0001\u001a\u00020TJ\u001f\u0010\u00e2\u0001\u001a\t\u0012\u0004\u0012\u00020P0\u008f\u00012\u0006\u0010\u0011\u001a\u00020T2\u0007\u0010\u00e3\u0001\u001a\u00020TJ \u0010\u00e4\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0006\u0010Z\u001a\u00020[2\u0007\u0010\u00e5\u0001\u001a\u00020TJ!\u0010\u00e6\u0001\u001a\t\u0012\u0004\u0012\u00020%0\u008f\u00012\b\u0010\u00e7\u0001\u001a\u00030\u00e8\u00012\u0007\u0010\u00e9\u0001\u001a\u00020%J\u0019\u0010\u00e6\u0001\u001a\u00020X2\u0006\u0010^\u001a\u00020_2\b\u0010\u00e7\u0001\u001a\u00030\u00e8\u0001J\u0018\u0010\u00ea\u0001\u001a\u00020X2\u0007\u0010\u00ba\u0001\u001a\u00020T2\u0006\u0010Y\u001a\u00020TJ#\u0010\u00eb\u0001\u001a\u00020X2\u0006\u0010^\u001a\u00020_2\u0007\u0010\u00ec\u0001\u001a\u00020[2\t\u0010s\u001a\u0005\u0018\u00010\u00ed\u0001J\u001a\u0010\u00ee\u0001\u001a\u00020X2\b\u0010Y\u001a\u0004\u0018\u00010T2\u0007\u0010\u00ef\u0001\u001a\u00020?J#\u0010\u00f0\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0012\u0010o\u001a\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0eJ\u001a\u0010\u00f1\u0001\u001a\u00020X2\b\u0010Y\u001a\u0004\u0018\u00010T2\u0007\u0010\u00ef\u0001\u001a\u00020?J\u0018\u0010\u00f2\u0001\u001a\u00020X2\r\u0010\u00f3\u0001\u001a\b\u0012\u0004\u0012\u00020T0\rH\u0002J\"\u0010\u00f4\u0001\u001a\u00030\u0095\u00012\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u00f5\u0001\u001a\u00020T2\u0007\u0010\u00f6\u0001\u001a\u00020TJ\u0010\u0010\u00f7\u0001\u001a\u00020X2\u0007\u0010\u00ef\u0001\u001a\u00020?J\u0010\u0010\u00f8\u0001\u001a\u00020X2\u0007\u0010\u00f9\u0001\u001a\u00020?J\u001f\u0010\u00fa\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u000e\u0010\u00fb\u0001\u001a\t\u0012\u0005\u0012\u00030\u00ac\u00010\rJ*\u0010\u00fc\u0001\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0019\u0010\u00fd\u0001\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0e0\rJ#\u0010\u00fe\u0001\u001a\u00020X2\b\u0010Y\u001a\u0004\u0018\u00010T2\u0006\u0010Z\u001a\u00020[2\b\u0010\\\u001a\u0004\u0018\u00010TJ\u0010\u0010\u00ff\u0001\u001a\u00020X2\u0007\u0010\u0080\u0002\u001a\u00020?J\u0012\u0010\u0081\u0002\u001a\u00020X2\t\u0010\u0082\u0002\u001a\u0004\u0018\u00010TJ&\u0010\u0083\u0002\u001a\t\u0012\u0004\u0012\u00020_0\u008f\u00012\u0006\u0010Y\u001a\u00020T2\u000e\u0010\u0084\u0002\u001a\t\u0012\u0004\u0012\u00020T0\u0085\u0002J\u000f\u0010\u0086\u0002\u001a\u00020X2\u0006\u0010Y\u001a\u00020TJ%\u0010\u0087\u0002\u001a\u00020X2\t\u0010\u0088\u0002\u001a\u0004\u0018\u00010T2\b\u0010q\u001a\u0004\u0018\u00010T2\u0007\u0010\u0089\u0002\u001a\u00020?J=\u0010\u008a\u0002\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u008a\u0001\u001a\u00020\u00182\u0007\u0010\u00e3\u0001\u001a\u00020T2\u001a\u0010\u008b\u0002\u001a\u0015\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0e0\u008c\u0002J&\u0010\u008d\u0002\u001a\u0010\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T\u0018\u00010e2\u0006\u0010Y\u001a\u00020T2\u0007\u0010\u008e\u0002\u001a\u00020TJ9\u0010\u008f\u0002\u001a\u0010\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T\u0018\u00010e2\u0007\u0010\u0088\u0002\u001a\u00020T2\u0007\u0010\u00a3\u0001\u001a\u00020T2\u0007\u0010\u0090\u0002\u001a\u00020T2\u0007\u0010\u0091\u0002\u001a\u00020TJ#\u0010\u0092\u0002\u001a\u00020X2\u0006\u0010Y\u001a\u00020T2\u0012\u0010d\u001a\u000e\u0012\u0004\u0012\u00020T\u0012\u0004\u0012\u00020T0eJ\u0019\u0010\u0093\u0002\u001a\u00020X2\u0007\u0010\u0089\u0001\u001a\u00020T2\u0007\u0010\u0094\u0002\u001a\u00020\u0018RT\u0010\u000b\u001aH\u0012\u0018\u0012\u0016\u0012\u0004\u0012\u00020\u000e \u000f*\n\u0012\u0004\u0012\u00020\u000e\u0018\u00010\r0\r \u000f*#\u0012\u0018\u0012\u0016\u0012\u0004\u0012\u00020\u000e \u000f*\n\u0012\u0004\u0012\u00020\u000e\u0018\u00010\r0\r\u0018\u00010\f\u00a2\u0006\u0002\b\u00100\f\u00a2\u0006\u0002\b\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R(\u0010\u0012\u001a\u0004\u0018\u00010\u000e2\b\u0010\u0011\u001a\u0004\u0018\u00010\u000e@FX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0013\u0010\u0014\"\u0004\b\u0015\u0010\u0016R\u0011\u0010\u0017\u001a\u00020\u00188F\u00a2\u0006\u0006\u001a\u0004\b\u0019\u0010\u001aR\u0017\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u000e0\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001eR#\u0010\u001f\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020!0 0\u001c8F\u00a2\u0006\u0006\u001a\u0004\b\"\u0010\u001eR\u0017\u0010#\u001a\b\u0012\u0004\u0012\u00020%0$\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\'R\u0014\u0010(\u001a\b\u0012\u0004\u0012\u00020)0$X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010*\u001a\b\u0012\u0004\u0012\u00020+0\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b,\u0010\u001eR\u0017\u0010-\u001a\b\u0012\u0004\u0012\u00020.0\u001c8F\u00a2\u0006\u0006\u001a\u0004\b/\u0010\u001eR\u0014\u00100\u001a\b\u0012\u0004\u0012\u00020.0$X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u00101\u001a\b\u0012\u0004\u0012\u0002020$X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u00103\u001a\b\u0012\u0004\u0012\u00020+0\u001c8F\u00a2\u0006\u0006\u001a\u0004\b4\u0010\u001eR\u0017\u00105\u001a\b\u0012\u0004\u0012\u0002060\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b7\u0010\u001eR\u0014\u00108\u001a\b\u0012\u0004\u0012\u00020\u000e09X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010:\u001a\b\u0012\u0004\u0012\u00020;0$X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010<\u001a\b\u0012\u0004\u0012\u00020=0$X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010>\u001a\u00020?X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010@\u001a\u00020?X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010A\u001a\b\u0012\u0004\u0012\u00020B0$X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010C\u001a\u0004\u0018\u00010%X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010D\u001a\b\u0012\u0004\u0012\u0002020\u001c8F\u00a2\u0006\u0006\u001a\u0004\bE\u0010\u001eR\u0014\u0010F\u001a\b\u0012\u0004\u0012\u0002020$X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010G\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\r0\u001c8F\u00a2\u0006\u0006\u001a\u0004\bH\u0010\u001eR\u0017\u0010I\u001a\b\u0012\u0004\u0012\u00020\u000e0$\u00a2\u0006\b\n\u0000\u001a\u0004\bJ\u0010\'R\u0014\u0010K\u001a\b\u0012\u0004\u0012\u00020L0$X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010M\u001a\b\u0012\u0004\u0012\u00020L0\u001c8F\u00a2\u0006\u0006\u001a\u0004\bN\u0010\u001eR\u0014\u0010O\u001a\b\u0012\u0004\u0012\u00020P0$X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010Q\u001a\b\u0012\u0004\u0012\u00020P0\u001c8F\u00a2\u0006\u0006\u001a\u0004\bR\u0010\u001eR\u0017\u0010S\u001a\b\u0012\u0004\u0012\u00020T0\r8F\u00a2\u0006\u0006\u001a\u0004\bU\u0010V\u00a8\u0006\u00a0\u0002"}, d2 = {"Lnet/jami/services/AccountService;", "", "mExecutor", "Ljava/util/concurrent/ScheduledExecutorService;", "mHistoryService", "Lnet/jami/services/HistoryService;", "mDeviceRuntimeService", "Lnet/jami/services/DeviceRuntimeService;", "mVCardService", "Lnet/jami/services/VCardService;", "(Ljava/util/concurrent/ScheduledExecutorService;Lnet/jami/services/HistoryService;Lnet/jami/services/DeviceRuntimeService;Lnet/jami/services/VCardService;)V", "accountsSubject", "Lio/reactivex/rxjava3/subjects/BehaviorSubject;", "", "Lnet/jami/model/Account;", "kotlin.jvm.PlatformType", "Lio/reactivex/rxjava3/annotations/NonNull;", "account", "currentAccount", "getCurrentAccount", "()Lnet/jami/model/Account;", "setCurrentAccount", "(Lnet/jami/model/Account;)V", "currentAccountIndex", "", "getCurrentAccountIndex", "()I", "currentAccountSubject", "Lio/reactivex/rxjava3/core/Observable;", "getCurrentAccountSubject", "()Lio/reactivex/rxjava3/core/Observable;", "currentProfileAccountSubject", "Lkotlin/Pair;", "Lnet/jami/model/Profile;", "getCurrentProfileAccountSubject", "dataTransfers", "Lio/reactivex/rxjava3/subjects/Subject;", "Lnet/jami/model/DataTransfer;", "getDataTransfers", "()Lio/reactivex/rxjava3/subjects/Subject;", "incomingMessageSubject", "Lnet/jami/services/AccountService$Message;", "incomingMessages", "Lnet/jami/model/TextMessage;", "getIncomingMessages", "incomingRequests", "Lnet/jami/model/TrustRequest;", "getIncomingRequests", "incomingRequestsSubject", "incomingSwarmMessageSubject", "Lnet/jami/model/Interaction;", "incomingSwarmMessages", "getIncomingSwarmMessages", "locationUpdates", "Lnet/jami/services/AccountService$Location;", "getLocationUpdates", "mAccountList", "", "mDeviceRevocationSubject", "Lnet/jami/services/AccountService$DeviceRevocationResult;", "mExportSubject", "Lnet/jami/services/AccountService$ExportOnRingResult;", "mHasRingAccount", "", "mHasSipAccount", "mMigrationSubject", "Lnet/jami/services/AccountService$MigrationResult;", "mStartingTransfer", "messageStateChanges", "getMessageStateChanges", "messageSubject", "observableAccountList", "getObservableAccountList", "observableAccounts", "getObservableAccounts", "registeredNameSubject", "Lnet/jami/services/AccountService$RegisteredName;", "registeredNames", "getRegisteredNames", "searchResultSubject", "Lnet/jami/services/AccountService$UserSearchResult;", "searchResults", "getSearchResults", "tlsSupportedMethods", "", "getTlsSupportedMethods", "()Ljava/util/List;", "acceptFileTransfer", "", "accountId", "conversationUri", "Lnet/jami/model/Uri;", "messageId", "fileId", "conversation", "Lnet/jami/model/Conversation;", "transfer", "acceptTrustRequest", "from", "accountDetailsChanged", "details", "", "accountMessageStatusChanged", "conversationId", "peer", "status", "accountProfileReceived", "name", "photo", "accountsChanged", "addAccount", "map", "addContact", "uri", "addMessage", "message", "cancelDataTransfer", "composingStatusChanged", "contactUri", "contactAdded", "confirmed", "contactRemoved", "banned", "conversationLoaded", "messages", "conversationMemberEvent", "peerUri", "event", "conversationReady", "conversationRemoved", "conversationRequestDeclined", "conversationRequestReceived", "metadata", "dataTransferEvent", "interactionId", "eventCode", "deviceRevocationEnded", "device", "state", "discardTrustRequest", "errorAlert", "alert", "exportOnRing", "Lio/reactivex/rxjava3/core/Single;", "password", "exportOnRingEnded", "code", "pin", "exportToFile", "Lio/reactivex/rxjava3/core/Completable;", "absolutePath", "findRegistrationByName", "nameserver", "getAccount", "getAccountByName", "getAccountSingle", "getAccountTemplate", "Ljava/util/HashMap;", "Lkotlin/collections/HashMap;", "accountType", "getCertificateDetails", "certificateRaw", "getCertificateDetailsPath", "certificatePath", "getCodecList", "Lnet/jami/model/Codec;", "getContacts", "getCredentials", "getKnownRingDevices", "getLastMessages", "Lnet/jami/daemon/Message;", "baseTime", "", "getNewAccountName", "prefix", "getObservableAccount", "getObservableAccountProfile", "getObservableAccountUpdates", "getTrustRequests", "handleTrustRequest", "request", "type", "Lnet/jami/services/AccountService$ContactType;", "hasRingAccount", "hasSipAccount", "incomingAccountMessage", "callId", "incomingTrustRequest", "received", "knownDevicesChanged", "devices", "loadAccountsFromDaemon", "isConnected", "loadConversationHistory", "root", "n", "loadMore", "lookupAddress", "address", "lookupName", "messageReceived", "migrateAccount", "migrationEnded", "nameRegistrationEnded", "observeDataTransfer", "profileReceived", "peerId", "vcardPath", "pushNotificationReceived", "data", "refreshAccounts", "refreshAccountsCacheFromDaemon", "registerAllAccounts", "registerName", "registeredNameFound", "registrationStateChanged", "newState", "detailString", "removeAccount", "removeContact", "ban", "removeConversation", "renameDevice", "newName", "revokeDevice", "deviceId", "searchUser", "query", "sendConversationMessage", "txt", "sendFile", "file", "Ljava/io/File;", "dataTransfer", "sendProfile", "sendTrustRequest", "to", "Lnet/jami/daemon/Blob;", "setAccountActive", "active", "setAccountDetails", "setAccountEnabled", "setAccountOrder", "accountOrder", "setAccountPassword", "oldPassword", "newPassword", "setAccountsActive", "setAccountsVideoEnabled", "isEnabled", "setActiveCodecList", "codecs", "setCredentials", "credentials", "setMessageDisplayed", "setProxyEnabled", "enabled", "setPushNotificationToken", "pushNotificationToken", "startConversation", "initialMembers", "", "stunStatusFailure", "subscribeBuddy", "accountID", "flag", "userSearchEnded", "results", "Ljava/util/ArrayList;", "validateCertificate", "certificate", "validateCertificatePath", "privateKeyPath", "privateKeyPass", "volatileAccountDetailsChanged", "volumeChanged", "value", "Companion", "ContactType", "ConversationMemberEvent", "DataTransferRefreshTask", "DeviceRevocationResult", "ExportOnRingResult", "Location", "Message", "MigrationResult", "RegisteredName", "UserSearchResult", "libringclient"})
public final class AccountService {
    private final java.util.concurrent.ScheduledExecutorService mExecutor = null;
    private final net.jami.services.HistoryService mHistoryService = null;
    private final net.jami.services.DeviceRuntimeService mDeviceRuntimeService = null;
    private final net.jami.services.VCardService mVCardService = null;
    
    /**
     * @return the current Account from the local cache
     */
    @org.jetbrains.annotations.Nullable()
    private net.jami.model.Account currentAccount;
    private java.util.List<net.jami.model.Account> mAccountList;
    private boolean mHasSipAccount = false;
    private boolean mHasRingAccount = false;
    private net.jami.model.DataTransfer mStartingTransfer;
    private final io.reactivex.rxjava3.subjects.BehaviorSubject<java.util.List<net.jami.model.Account>> accountsSubject = null;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.model.Account> observableAccounts = null;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.core.Observable<net.jami.model.Account> currentAccountSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.services.AccountService.Message> incomingMessageSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.model.Interaction> incomingSwarmMessageSubject = null;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.core.Observable<net.jami.model.TextMessage> incomingMessages = null;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.core.Observable<net.jami.services.AccountService.Location> locationUpdates = null;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.model.Interaction> messageSubject = null;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.model.DataTransfer> dataTransfers = null;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.model.TrustRequest> incomingRequestsSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.services.AccountService.RegisteredName> registeredNameSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.services.AccountService.UserSearchResult> searchResultSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.services.AccountService.ExportOnRingResult> mExportSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.services.AccountService.DeviceRevocationResult> mDeviceRevocationSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.services.AccountService.MigrationResult> mMigrationSubject = null;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.services.AccountService.Companion Companion = null;
    private static final java.lang.String TAG = null;
    private static final int VCARD_CHUNK_SIZE = 1000;
    private static final long DATA_TRANSFER_REFRESH_PERIOD = 500L;
    private static final int PIN_GENERATION_SUCCESS = 0;
    private static final int PIN_GENERATION_WRONG_PASSWORD = 1;
    private static final int PIN_GENERATION_NETWORK_ERROR = 2;
    
    public AccountService(@org.jetbrains.annotations.NotNull()
    java.util.concurrent.ScheduledExecutorService mExecutor, @org.jetbrains.annotations.NotNull()
    net.jami.services.HistoryService mHistoryService, @org.jetbrains.annotations.NotNull()
    net.jami.services.DeviceRuntimeService mDeviceRuntimeService, @org.jetbrains.annotations.NotNull()
    net.jami.services.VCardService mVCardService) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Account getCurrentAccount() {
        return null;
    }
    
    public final void setCurrentAccount(@org.jetbrains.annotations.Nullable()
    net.jami.model.Account account) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.subjects.Subject<net.jami.model.Account> getObservableAccounts() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Account> getCurrentAccountSubject() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.TextMessage> getIncomingMessages() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.services.AccountService.Location> getLocationUpdates() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.subjects.Subject<net.jami.model.DataTransfer> getDataTransfers() {
        return null;
    }
    
    public final void refreshAccounts() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.services.AccountService.RegisteredName> getRegisteredNames() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.services.AccountService.UserSearchResult> getSearchResults() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.TextMessage> getIncomingSwarmMessages() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Interaction> getMessageStateChanges() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.TrustRequest> getIncomingRequests() {
        return null;
    }
    
    /**
     * @return true if at least one of the loaded accounts is a SIP one
     */
    public final boolean hasSipAccount() {
        return false;
    }
    
    /**
     * @return true if at least one of the loaded accounts is a Ring one
     */
    public final boolean hasRingAccount() {
        return false;
    }
    
    /**
     * Loads the accounts from the daemon and then builds the local cache (also sends ACCOUNTS_CHANGED event)
     *
     * @param isConnected sets the initial connection state of the accounts
     */
    public final void loadAccountsFromDaemon(boolean isConnected) {
    }
    
    private final void refreshAccountsCacheFromDaemon() {
    }
    
    private final net.jami.model.Account getAccountByName(java.lang.String name) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getNewAccountName(@org.jetbrains.annotations.Nullable()
    java.lang.String prefix) {
        return null;
    }
    
    /**
     * Adds a new Account in the Daemon (also sends an ACCOUNT_ADDED event)
     * Sets the new account as the current one
     *
     * @param map the account details
     * @return the created Account
     */
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Account> addAccount(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> map) {
        return null;
    }
    
    public final int getCurrentAccountIndex() {
        return 0;
    }
    
    /**
     * @return the Account from the local cache that matches the accountId
     */
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Account getAccount(@org.jetbrains.annotations.Nullable()
    java.lang.String accountId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.Account> getAccountSingle(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<net.jami.model.Account>> getObservableAccountList() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Account> getObservableAccountUpdates(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<kotlin.Pair<net.jami.model.Account, net.jami.model.Profile>> getObservableAccountProfile(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Account> getObservableAccount(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Account> getObservableAccount(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<kotlin.Pair<net.jami.model.Account, net.jami.model.Profile>> getCurrentProfileAccountSubject() {
        return null;
    }
    
    public final void subscribeBuddy(@org.jetbrains.annotations.Nullable()
    java.lang.String accountID, @org.jetbrains.annotations.Nullable()
    java.lang.String uri, boolean flag) {
    }
    
    /**
     * Send profile through SIP
     */
    public final void sendProfile(@org.jetbrains.annotations.NotNull()
    java.lang.String callId, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
    }
    
    public final void setMessageDisplayed(@org.jetbrains.annotations.Nullable()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri, @org.jetbrains.annotations.Nullable()
    java.lang.String messageId) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.Conversation> startConversation(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.util.Collection<java.lang.String> initialMembers) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable removeConversation(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri) {
        return null;
    }
    
    public final void loadConversationHistory(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri, @org.jetbrains.annotations.NotNull()
    java.lang.String root, long n) {
    }
    
    @org.jetbrains.annotations.NotNull()
    @kotlin.jvm.JvmOverloads()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.Conversation> loadMore(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @kotlin.jvm.JvmOverloads()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.Conversation> loadMore(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation, int n) {
        return null;
    }
    
    public final void sendConversationMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri, @org.jetbrains.annotations.NotNull()
    java.lang.String txt) {
    }
    
    /**
     * Sets the order of the accounts in the Daemon
     *
     * @param accountOrder The ordered list of account ids
     */
    private final void setAccountOrder(java.util.List<java.lang.String> accountOrder) {
    }
    
    /**
     * Sets the account details in the Daemon
     */
    public final void setAccountDetails(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> map) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<java.lang.String> migrateAccount(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String password) {
        return null;
    }
    
    public final void setAccountEnabled(@org.jetbrains.annotations.Nullable()
    java.lang.String accountId, boolean active) {
    }
    
    /**
     * Sets the activation state of the account in the Daemon
     */
    public final void setAccountActive(@org.jetbrains.annotations.Nullable()
    java.lang.String accountId, boolean active) {
    }
    
    /**
     * Sets the activation state of all the accounts in the Daemon
     */
    public final void setAccountsActive(boolean active) {
    }
    
    /**
     * Sets the video activation state of all the accounts in the local cache
     */
    public final void setAccountsVideoEnabled(boolean isEnabled) {
    }
    
    /**
     * @return the default template (account details) for a type of account
     */
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<java.util.HashMap<java.lang.String, java.lang.String>> getAccountTemplate(@org.jetbrains.annotations.NotNull()
    java.lang.String accountType) {
        return null;
    }
    
    /**
     * Removes the account in the Daemon as well as local history
     */
    public final void removeAccount(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
    }
    
    /**
     * Exports the account on the DHT (used for multi-devices feature)
     */
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<java.lang.String> exportOnRing(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String password) {
        return null;
    }
    
    /**
     * @return the list of the account's devices from the Daemon
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, java.lang.String> getKnownRingDevices(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        return null;
    }
    
    /**
     * @param accountId id of the account used with the device
     * @param deviceId  id of the device to revoke
     * @param password  password of the account
     */
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<java.lang.Integer> revokeDevice(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    java.lang.String deviceId) {
        return null;
    }
    
    /**
     * @param accountId id of the account used with the device
     * @param newName   new device name
     */
    public final void renameDevice(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String newName) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable exportToFile(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String absolutePath, @org.jetbrains.annotations.NotNull()
    java.lang.String password) {
        return null;
    }
    
    /**
     * @param accountId   id of the account
     * @param oldPassword old account password
     */
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable setAccountPassword(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String oldPassword, @org.jetbrains.annotations.NotNull()
    java.lang.String newPassword) {
        return null;
    }
    
    /**
     * Sets the active codecs list of the account in the Daemon
     */
    public final void setActiveCodecList(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.Long> codecs) {
    }
    
    /**
     * @return The account's codecs list from the Daemon
     */
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<java.util.List<net.jami.model.Codec>> getCodecList(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.util.Map<java.lang.String, java.lang.String> validateCertificatePath(@org.jetbrains.annotations.NotNull()
    java.lang.String accountID, @org.jetbrains.annotations.NotNull()
    java.lang.String certificatePath, @org.jetbrains.annotations.NotNull()
    java.lang.String privateKeyPath, @org.jetbrains.annotations.NotNull()
    java.lang.String privateKeyPass) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.util.Map<java.lang.String, java.lang.String> validateCertificate(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String certificate) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.util.Map<java.lang.String, java.lang.String> getCertificateDetailsPath(@org.jetbrains.annotations.NotNull()
    java.lang.String certificatePath) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.util.Map<java.lang.String, java.lang.String> getCertificateDetails(@org.jetbrains.annotations.NotNull()
    java.lang.String certificateRaw) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getTlsSupportedMethods() {
        return null;
    }
    
    /**
     * @return the account's credentials from the Daemon
     */
    @org.jetbrains.annotations.Nullable()
    public final java.util.List<java.util.Map<java.lang.String, java.lang.String>> getCredentials(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        return null;
    }
    
    /**
     * Sets the account's credentials in the Daemon
     */
    public final void setCredentials(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends java.util.Map<java.lang.String, java.lang.String>> credentials) {
    }
    
    /**
     * Sets the registration state to true for all the accounts in the Daemon
     */
    public final void registerAllAccounts() {
    }
    
    /**
     * Registers a new name on the blockchain for the account
     */
    public final void registerName(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account, @org.jetbrains.annotations.Nullable()
    java.lang.String password, @org.jetbrains.annotations.Nullable()
    java.lang.String name) {
    }
    
    /**
     * Register a new name on the blockchain for the account Id
     */
    public final void registerName(@org.jetbrains.annotations.Nullable()
    java.lang.String account, @org.jetbrains.annotations.Nullable()
    java.lang.String password, @org.jetbrains.annotations.Nullable()
    java.lang.String name) {
    }
    
    /**
     * @return all trust requests from the daemon for the account Id
     */
    @org.jetbrains.annotations.Nullable()
    public final java.util.List<java.util.Map<java.lang.String, java.lang.String>> getTrustRequests(@org.jetbrains.annotations.Nullable()
    java.lang.String accountId) {
        return null;
    }
    
    /**
     * Accepts a pending trust request
     */
    public final void acceptTrustRequest(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri from) {
    }
    
    /**
     * Handles adding contacts and is the initial point of conversation creation
     *
     * @param conversation    the user's account
     * @param contactUri the contacts raw string uri
     */
    private final void handleTrustRequest(net.jami.model.Conversation conversation, net.jami.model.Uri contactUri, net.jami.model.TrustRequest request, net.jami.services.AccountService.ContactType type) {
    }
    
    /**
     * Refuses and blocks a pending trust request
     */
    public final boolean discardTrustRequest(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri contactUri) {
        return false;
    }
    
    /**
     * Sends a new trust request
     */
    public final void sendTrustRequest(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri to, @org.jetbrains.annotations.Nullable()
    net.jami.daemon.Blob message) {
    }
    
    /**
     * Add a new contact for the account Id on the Daemon
     */
    public final void addContact(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String uri) {
    }
    
    /**
     * Remove an existing contact for the account Id on the Daemon
     */
    public final void removeContact(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String uri, boolean ban) {
    }
    
    /**
     * @return the contacts list from the daemon
     */
    @org.jetbrains.annotations.Nullable()
    public final java.util.List<java.util.Map<java.lang.String, java.lang.String>> getContacts(@org.jetbrains.annotations.Nullable()
    java.lang.String accountId) {
        return null;
    }
    
    /**
     * Looks up for the availability of the name on the blockchain
     */
    public final void lookupName(@org.jetbrains.annotations.NotNull()
    java.lang.String account, @org.jetbrains.annotations.NotNull()
    java.lang.String nameserver, @org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<net.jami.services.AccountService.RegisteredName> findRegistrationByName(@org.jetbrains.annotations.NotNull()
    java.lang.String account, @org.jetbrains.annotations.NotNull()
    java.lang.String nameserver, @org.jetbrains.annotations.NotNull()
    java.lang.String name) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<net.jami.services.AccountService.UserSearchResult> searchUser(@org.jetbrains.annotations.NotNull()
    java.lang.String account, @org.jetbrains.annotations.NotNull()
    java.lang.String query) {
        return null;
    }
    
    /**
     * Reverse looks up the address in the blockchain to find the name
     */
    public final void lookupAddress(@org.jetbrains.annotations.Nullable()
    java.lang.String account, @org.jetbrains.annotations.Nullable()
    java.lang.String nameserver, @org.jetbrains.annotations.Nullable()
    java.lang.String address) {
    }
    
    public final void pushNotificationReceived(@org.jetbrains.annotations.Nullable()
    java.lang.String from, @org.jetbrains.annotations.Nullable()
    java.util.Map<java.lang.String, java.lang.String> data) {
    }
    
    public final void setPushNotificationToken(@org.jetbrains.annotations.Nullable()
    java.lang.String pushNotificationToken) {
    }
    
    public final void volumeChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String device, int value) {
    }
    
    public final void accountsChanged() {
    }
    
    public final void stunStatusFailure(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
    }
    
    public final void registrationStateChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String newState, int code, @org.jetbrains.annotations.Nullable()
    java.lang.String detailString) {
    }
    
    public final void accountDetailsChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> details) {
    }
    
    public final void volatileAccountDetailsChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> details) {
    }
    
    public final void accountProfileReceived(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.Nullable()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String photo) {
    }
    
    public final void profileReceived(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String peerId, @org.jetbrains.annotations.NotNull()
    java.lang.String vcardPath) {
    }
    
    public final void incomingAccountMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.Nullable()
    java.lang.String messageId, @org.jetbrains.annotations.Nullable()
    java.lang.String callId, @org.jetbrains.annotations.NotNull()
    java.lang.String from, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> messages) {
    }
    
    public final void accountMessageStatusChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
    java.lang.String messageId, @org.jetbrains.annotations.NotNull()
    java.lang.String peer, int status) {
    }
    
    public final void composingStatusChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
    java.lang.String contactUri, int status) {
    }
    
    public final void errorAlert(int alert) {
    }
    
    public final void knownDevicesChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> devices) {
    }
    
    public final void exportOnRingEnded(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, int code, @org.jetbrains.annotations.NotNull()
    java.lang.String pin) {
    }
    
    public final void nameRegistrationEnded(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, int state, @org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    public final void migrationEnded(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String state) {
    }
    
    public final void deviceRevocationEnded(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String device, int state) {
    }
    
    public final void incomingTrustRequest(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
    java.lang.String from, @org.jetbrains.annotations.Nullable()
    java.lang.String message, long received) {
    }
    
    public final void contactAdded(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String uri, boolean confirmed) {
    }
    
    public final void contactRemoved(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String uri, boolean banned) {
    }
    
    public final void registeredNameFound(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, int state, @org.jetbrains.annotations.NotNull()
    java.lang.String address, @org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    public final void userSearchEnded(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, int state, @org.jetbrains.annotations.NotNull()
    java.lang.String query, @org.jetbrains.annotations.NotNull()
    java.util.ArrayList<java.util.Map<java.lang.String, java.lang.String>> results) {
    }
    
    private final net.jami.model.Interaction addMessage(net.jami.model.Account account, net.jami.model.Conversation conversation, java.util.Map<java.lang.String, java.lang.String> message) {
        return null;
    }
    
    public final void conversationLoaded(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends java.util.Map<java.lang.String, java.lang.String>> messages) {
    }
    
    public final void conversationMemberEvent(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
    java.lang.String peerUri, int event) {
    }
    
    public final void conversationReady(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String conversationId) {
    }
    
    public final void conversationRemoved(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String conversationId) {
    }
    
    public final void conversationRequestDeclined(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String conversationId) {
    }
    
    public final void conversationRequestReceived(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> metadata) {
    }
    
    public final void messageReceived(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> message) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.DataTransfer> sendFile(@org.jetbrains.annotations.NotNull()
    java.io.File file, @org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer dataTransfer) {
        return null;
    }
    
    public final void sendFile(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation, @org.jetbrains.annotations.NotNull()
    java.io.File file) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<net.jami.daemon.Message> getLastMessages(@org.jetbrains.annotations.Nullable()
    java.lang.String accountId, long baseTime) {
        return null;
    }
    
    public final void acceptFileTransfer(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri, @org.jetbrains.annotations.Nullable()
    java.lang.String messageId, @org.jetbrains.annotations.NotNull()
    java.lang.String fileId) {
    }
    
    public final void acceptFileTransfer(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation, @org.jetbrains.annotations.NotNull()
    java.lang.String fileId, @org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer transfer) {
    }
    
    public final void cancelDataTransfer(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.Nullable()
    java.lang.String messageId, @org.jetbrains.annotations.NotNull()
    java.lang.String fileId) {
    }
    
    public final void dataTransferEvent(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
    java.lang.String interactionId, @org.jetbrains.annotations.NotNull()
    java.lang.String fileId, int eventCode) {
    }
    
    public final void dataTransferEvent(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account, @org.jetbrains.annotations.Nullable()
    net.jami.model.Conversation conversation, @org.jetbrains.annotations.Nullable()
    java.lang.String interactionId, @org.jetbrains.annotations.NotNull()
    java.lang.String fileId, int eventCode) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.DataTransfer> observeDataTransfer(@org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer transfer) {
        return null;
    }
    
    public final void setProxyEnabled(boolean enabled) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010$\n\u0002\b\t\u0018\u00002\u00020\u0001B=\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0003\u0012\u0012\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\b\u00a2\u0006\u0002\u0010\tR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0006\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\u000bR\u0013\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000bR\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000bR\u001d\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010\u00a8\u0006\u0011"}, d2 = {"Lnet/jami/services/AccountService$Message;", "", "accountId", "", "messageId", "callId", "author", "messages", "", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/Map;)V", "getAccountId", "()Ljava/lang/String;", "getAuthor", "getCallId", "getMessageId", "getMessages", "()Ljava/util/Map;", "libringclient"})
    public static final class Message {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String accountId = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String messageId = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String callId = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String author = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.Map<java.lang.String, java.lang.String> messages = null;
        
        public Message(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.Nullable()
        java.lang.String messageId, @org.jetbrains.annotations.Nullable()
        java.lang.String callId, @org.jetbrains.annotations.NotNull()
        java.lang.String author, @org.jetbrains.annotations.NotNull()
        java.util.Map<java.lang.String, java.lang.String> messages) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getAccountId() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getMessageId() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getCallId() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getAuthor() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.Map<java.lang.String, java.lang.String> getMessages() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\t\n\u0002\u0010\u0006\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u00002\u00020\u0001:\u0001\"B\'\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u00a2\u0006\u0002\u0010\tR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\u000bR\u001a\u0010\u0007\u001a\u00020\bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010R\u001a\u0010\u0011\u001a\u00020\u0012X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0013\u0010\u0014\"\u0004\b\u0015\u0010\u0016R\u001a\u0010\u0017\u001a\u00020\u0012X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0018\u0010\u0014\"\u0004\b\u0019\u0010\u0016R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u001bR\u001a\u0010\u001c\u001a\u00020\u001dX\u0086.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001e\u0010\u001f\"\u0004\b \u0010!\u00a8\u0006#"}, d2 = {"Lnet/jami/services/AccountService$Location;", "", "account", "", "callId", "peer", "Lnet/jami/model/Uri;", "date", "", "(Ljava/lang/String;Ljava/lang/String;Lnet/jami/model/Uri;J)V", "getAccount", "()Ljava/lang/String;", "getCallId", "getDate", "()J", "setDate", "(J)V", "latitude", "", "getLatitude", "()D", "setLatitude", "(D)V", "longitude", "getLongitude", "setLongitude", "getPeer", "()Lnet/jami/model/Uri;", "type", "Lnet/jami/services/AccountService$Location$Type;", "getType", "()Lnet/jami/services/AccountService$Location$Type;", "setType", "(Lnet/jami/services/AccountService$Location$Type;)V", "Type", "libringclient"})
    public static final class Location {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String account = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String callId = null;
        @org.jetbrains.annotations.NotNull()
        private final net.jami.model.Uri peer = null;
        private long date;
        public net.jami.services.AccountService.Location.Type type;
        private double latitude = 0.0;
        private double longitude = 0.0;
        
        public Location(@org.jetbrains.annotations.NotNull()
        java.lang.String account, @org.jetbrains.annotations.Nullable()
        java.lang.String callId, @org.jetbrains.annotations.NotNull()
        net.jami.model.Uri peer, long date) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getAccount() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getCallId() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final net.jami.model.Uri getPeer() {
            return null;
        }
        
        public final long getDate() {
            return 0L;
        }
        
        public final void setDate(long p0) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final net.jami.services.AccountService.Location.Type getType() {
            return null;
        }
        
        public final void setType(@org.jetbrains.annotations.NotNull()
        net.jami.services.AccountService.Location.Type p0) {
        }
        
        public final double getLatitude() {
            return 0.0;
        }
        
        public final void setLatitude(double p0) {
        }
        
        public final double getLongitude() {
            return 0.0;
        }
        
        public final void setLongitude(double p0) {
        }
        
        @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0004\b\u0086\u0001\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004\u00a8\u0006\u0005"}, d2 = {"Lnet/jami/services/AccountService$Location$Type;", "", "(Ljava/lang/String;I)V", "Position", "Stop", "libringclient"})
        public static enum Type {
            /*public static final*/ Position /* = new Position() */,
            /*public static final*/ Stop /* = new Stop() */;
            
            Type() {
            }
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\b\u0018\u00002\u00020\u0001B+\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0013\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\nR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\nR\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e\u00a8\u0006\u000f"}, d2 = {"Lnet/jami/services/AccountService$RegisteredName;", "", "accountId", "", "name", "address", "state", "", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V", "getAccountId", "()Ljava/lang/String;", "getAddress", "getName", "getState", "()I", "libringclient"})
    public static final class RegisteredName {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String accountId = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String name = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String address = null;
        private final int state = 0;
        
        public RegisteredName(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String name, @org.jetbrains.annotations.Nullable()
        java.lang.String address, int state) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getAccountId() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getName() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getAddress() {
            return null;
        }
        
        public final int getState() {
            return 0;
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\tR\"\u0010\u000b\u001a\n\u0012\u0004\u0012\u00020\r\u0018\u00010\fX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000e\u0010\u000f\"\u0004\b\u0010\u0010\u0011R\u001d\u0010\u0012\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00150\u00140\u00138F\u00a2\u0006\u0006\u001a\u0004\b\u0016\u0010\u000fR\u001a\u0010\u0005\u001a\u00020\u0006X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0017\u0010\u0018\"\u0004\b\u0019\u0010\u001a\u00a8\u0006\u001b"}, d2 = {"Lnet/jami/services/AccountService$UserSearchResult;", "", "accountId", "", "query", "state", "", "(Ljava/lang/String;Ljava/lang/String;I)V", "getAccountId", "()Ljava/lang/String;", "getQuery", "results", "", "Lnet/jami/model/Contact;", "getResults", "()Ljava/util/List;", "setResults", "(Ljava/util/List;)V", "resultsViewModels", "", "Lio/reactivex/rxjava3/core/Observable;", "Lnet/jami/smartlist/SmartListViewModel;", "getResultsViewModels", "getState", "()I", "setState", "(I)V", "libringclient"})
    public static final class UserSearchResult {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String accountId = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String query = null;
        private int state;
        @org.jetbrains.annotations.Nullable()
        private java.util.List<net.jami.model.Contact> results;
        
        public UserSearchResult(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String query, int state) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getAccountId() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getQuery() {
            return null;
        }
        
        public final int getState() {
            return 0;
        }
        
        public final void setState(int p0) {
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.util.List<net.jami.model.Contact> getResults() {
            return null;
        }
        
        public final void setResults(@org.jetbrains.annotations.Nullable()
        java.util.List<net.jami.model.Contact> p0) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel>> getResultsViewModels() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\r\b\u0002\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\u0007R\u001a\u0010\u0002\u001a\u00020\u0003X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u001a\u0010\u0004\u001a\u00020\u0005X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u001c\u0010\u0006\u001a\u0004\u0018\u00010\u0003X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0010\u0010\t\"\u0004\b\u0011\u0010\u000b\u00a8\u0006\u0012"}, d2 = {"Lnet/jami/services/AccountService$ExportOnRingResult;", "", "accountId", "", "code", "", "pin", "(Ljava/lang/String;ILjava/lang/String;)V", "getAccountId", "()Ljava/lang/String;", "setAccountId", "(Ljava/lang/String;)V", "getCode", "()I", "setCode", "(I)V", "getPin", "setPin", "libringclient"})
    static final class ExportOnRingResult {
        @org.jetbrains.annotations.NotNull()
        private java.lang.String accountId;
        private int code;
        @org.jetbrains.annotations.Nullable()
        private java.lang.String pin;
        
        public ExportOnRingResult(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, int code, @org.jetbrains.annotations.Nullable()
        java.lang.String pin) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getAccountId() {
            return null;
        }
        
        public final void setAccountId(@org.jetbrains.annotations.NotNull()
        java.lang.String p0) {
        }
        
        public final int getCode() {
            return 0;
        }
        
        public final void setCode(int p0) {
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getPin() {
            return null;
        }
        
        public final void setPin(@org.jetbrains.annotations.Nullable()
        java.lang.String p0) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\f\b\u0002\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007R\u001a\u0010\u0002\u001a\u00020\u0003X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u001a\u0010\u0005\u001a\u00020\u0006X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u001a\u0010\u0004\u001a\u00020\u0003X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0010\u0010\t\"\u0004\b\u0011\u0010\u000b\u00a8\u0006\u0012"}, d2 = {"Lnet/jami/services/AccountService$DeviceRevocationResult;", "", "accountId", "", "deviceId", "code", "", "(Ljava/lang/String;Ljava/lang/String;I)V", "getAccountId", "()Ljava/lang/String;", "setAccountId", "(Ljava/lang/String;)V", "getCode", "()I", "setCode", "(I)V", "getDeviceId", "setDeviceId", "libringclient"})
    static final class DeviceRevocationResult {
        @org.jetbrains.annotations.NotNull()
        private java.lang.String accountId;
        @org.jetbrains.annotations.NotNull()
        private java.lang.String deviceId;
        private int code;
        
        public DeviceRevocationResult(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String deviceId, int code) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getAccountId() {
            return null;
        }
        
        public final void setAccountId(@org.jetbrains.annotations.NotNull()
        java.lang.String p0) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getDeviceId() {
            return null;
        }
        
        public final void setDeviceId(@org.jetbrains.annotations.NotNull()
        java.lang.String p0) {
        }
        
        public final int getCode() {
            return 0;
        }
        
        public final void setCode(int p0) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\t\b\u0002\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0005R\u001a\u0010\u0002\u001a\u00020\u0003X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\tR\u001a\u0010\u0004\u001a\u00020\u0003X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\n\u0010\u0007\"\u0004\b\u000b\u0010\t\u00a8\u0006\f"}, d2 = {"Lnet/jami/services/AccountService$MigrationResult;", "", "accountId", "", "state", "(Ljava/lang/String;Ljava/lang/String;)V", "getAccountId", "()Ljava/lang/String;", "setAccountId", "(Ljava/lang/String;)V", "getState", "setState", "libringclient"})
    static final class MigrationResult {
        @org.jetbrains.annotations.NotNull()
        private java.lang.String accountId;
        @org.jetbrains.annotations.NotNull()
        private java.lang.String state;
        
        public MigrationResult(@org.jetbrains.annotations.NotNull()
        java.lang.String accountId, @org.jetbrains.annotations.NotNull()
        java.lang.String state) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getAccountId() {
            return null;
        }
        
        public final void setAccountId(@org.jetbrains.annotations.NotNull()
        java.lang.String p0) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getState() {
            return null;
        }
        
        public final void setState(@org.jetbrains.annotations.NotNull()
        java.lang.String p0) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0006\b\u0082\u0001\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006\u00a8\u0006\u0007"}, d2 = {"Lnet/jami/services/AccountService$ContactType;", "", "(Ljava/lang/String;I)V", "ADDED", "INVITATION_RECEIVED", "INVITATION_ACCEPTED", "INVITATION_DISCARDED", "libringclient"})
    static enum ContactType {
        /*public static final*/ ADDED /* = new ADDED() */,
        /*public static final*/ INVITATION_RECEIVED /* = new INVITATION_RECEIVED() */,
        /*public static final*/ INVITATION_ACCEPTED /* = new INVITATION_ACCEPTED() */,
        /*public static final*/ INVITATION_DISCARDED /* = new INVITATION_DISCARDED() */;
        
        ContactType() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0006\b\u0082\u0001\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006\u00a8\u0006\u0007"}, d2 = {"Lnet/jami/services/AccountService$ConversationMemberEvent;", "", "(Ljava/lang/String;I)V", "Add", "Join", "Remove", "Ban", "libringclient"})
    static enum ConversationMemberEvent {
        /*public static final*/ Add /* = new Add() */,
        /*public static final*/ Join /* = new Join() */,
        /*public static final*/ Remove /* = new Remove() */,
        /*public static final*/ Ban /* = new Ban() */;
        
        ConversationMemberEvent() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\b\u0082\u0004\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\b\u0010\u000f\u001a\u00020\u0010H\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0004\u001a\u0004\u0018\u00010\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010\t\u001a\b\u0012\u0002\b\u0003\u0018\u00010\nX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000e\u00a8\u0006\u0011"}, d2 = {"Lnet/jami/services/AccountService$DataTransferRefreshTask;", "Ljava/lang/Runnable;", "mAccount", "Lnet/jami/model/Account;", "mConversation", "Lnet/jami/model/Conversation;", "mToUpdate", "Lnet/jami/model/DataTransfer;", "(Lnet/jami/services/AccountService;Lnet/jami/model/Account;Lnet/jami/model/Conversation;Lnet/jami/model/DataTransfer;)V", "scheduledTask", "Ljava/util/concurrent/ScheduledFuture;", "getScheduledTask", "()Ljava/util/concurrent/ScheduledFuture;", "setScheduledTask", "(Ljava/util/concurrent/ScheduledFuture;)V", "run", "", "libringclient"})
    final class DataTransferRefreshTask implements java.lang.Runnable {
        private final net.jami.model.Account mAccount = null;
        private final net.jami.model.Conversation mConversation = null;
        private final net.jami.model.DataTransfer mToUpdate = null;
        @org.jetbrains.annotations.Nullable()
        private java.util.concurrent.ScheduledFuture<?> scheduledTask;
        
        public DataTransferRefreshTask(@org.jetbrains.annotations.NotNull()
        net.jami.model.Account mAccount, @org.jetbrains.annotations.Nullable()
        net.jami.model.Conversation mConversation, @org.jetbrains.annotations.NotNull()
        net.jami.model.DataTransfer mToUpdate) {
            super();
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.util.concurrent.ScheduledFuture<?> getScheduledTask() {
            return null;
        }
        
        public final void setScheduledTask(@org.jetbrains.annotations.Nullable()
        java.util.concurrent.ScheduledFuture<?> p0) {
        }
        
        @java.lang.Override()
        public void run() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\"\u0010\r\u001a\u0004\u0018\u00010\u000e2\u000e\u0010\u000f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000e0\u00102\u0006\u0010\u0011\u001a\u00020\nH\u0002J\u0017\u0010\u0012\u001a\u00020\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\u0004H\u0002\u00a2\u0006\u0002\u0010\u0015J\u0010\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0006H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u0016\u0010\t\u001a\n \u000b*\u0004\u0018\u00010\n0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0019"}, d2 = {"Lnet/jami/services/AccountService$Companion;", "", "()V", "DATA_TRANSFER_REFRESH_PERIOD", "", "PIN_GENERATION_NETWORK_ERROR", "", "PIN_GENERATION_SUCCESS", "PIN_GENERATION_WRONG_PASSWORD", "TAG", "", "kotlin.jvm.PlatformType", "VCARD_CHUNK_SIZE", "findAccount", "Lnet/jami/model/Account;", "accounts", "", "accountId", "getDataTransferError", "Lnet/jami/model/DataTransferError;", "errorCode", "(Ljava/lang/Long;)Lnet/jami/model/DataTransferError;", "getDataTransferEventCode", "Lnet/jami/model/Interaction$InteractionStatus;", "eventCode", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        private final net.jami.model.Account findAccount(java.util.List<net.jami.model.Account> accounts, java.lang.String accountId) {
            return null;
        }
        
        private final net.jami.model.Interaction.InteractionStatus getDataTransferEventCode(int eventCode) {
            return null;
        }
        
        private final net.jami.model.DataTransferError getDataTransferError(java.lang.Long errorCode) {
            return null;
        }
    }
}