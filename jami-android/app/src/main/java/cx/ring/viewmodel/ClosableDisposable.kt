package cx.ring.viewmodel

import io.reactivex.rxjava3.disposables.Disposable
import java.io.Closeable

class ClosableDisposable(val disposable: Disposable) : Closeable {
    override fun close() {
        disposable.dispose()
    }
}