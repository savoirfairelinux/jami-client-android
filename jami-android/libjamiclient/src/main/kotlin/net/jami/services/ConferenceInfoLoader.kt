package net.jami.services

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.model.Conference.ParticipantInfo

internal class ConferenceInfoLoader(
    scheduler: Scheduler,
    onInfo: (List<ParticipantInfo>) -> Unit,
    onError: (Throwable) -> Unit
) {
    private val updates = PublishSubject.create<Observable<List<ParticipantInfo>>>().toSerialized()
    private val subscription = updates
        .observeOn(scheduler)
        .switchMap { update ->
            update.onErrorResumeNext { error: Throwable ->
                onError(error)
                Observable.empty()
            }
        }
        .observeOn(scheduler)
        .subscribe(onInfo, onError)

    fun update(info: Observable<List<ParticipantInfo>>) {
        updates.onNext(info)
    }

    fun dispose() {
        subscription.dispose()
    }
}
