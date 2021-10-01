package net.jami.mvp;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0004\b&\u0018\u0000*\u0004\b\u0000\u0010\u00012\u00020\u0002B\u0005\u00a2\u0006\u0002\u0010\u0003J\u0015\u0010\u000b\u001a\u00020\f2\u0006\u0010\b\u001a\u00028\u0000H\u0016\u00a2\u0006\u0002\u0010\rJ\b\u0010\u000e\u001a\u00020\fH\u0016J\b\u0010\u000f\u001a\u00020\fH\u0016R\u0012\u0010\u0004\u001a\u00020\u00058\u0004@\u0004X\u0085\u000e\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0006\u001a\n\u0012\u0004\u0012\u00028\u0000\u0018\u00010\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0013\u0010\b\u001a\u0004\u0018\u00018\u00008F\u00a2\u0006\u0006\u001a\u0004\b\t\u0010\n\u00a8\u0006\u0010"}, d2 = {"Lnet/jami/mvp/RootPresenter;", "T", "", "()V", "mCompositeDisposable", "Lio/reactivex/rxjava3/disposables/CompositeDisposable;", "mView", "Ljava/lang/ref/WeakReference;", "view", "getView", "()Ljava/lang/Object;", "bindView", "", "(Ljava/lang/Object;)V", "onDestroy", "unbindView", "libringclient"})
public abstract class RootPresenter<T extends java.lang.Object> {
    @org.jetbrains.annotations.NotNull()
    @kotlin.jvm.JvmField()
    protected io.reactivex.rxjava3.disposables.CompositeDisposable mCompositeDisposable;
    private java.lang.ref.WeakReference<T> mView;
    
    public RootPresenter() {
        super();
    }
    
    public void bindView(T view) {
    }
    
    public void unbindView() {
    }
    
    public void onDestroy() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final T getView() {
        return null;
    }
}