package cx.ring.views

import androidx.recyclerview.widget.RecyclerView
import cx.ring.databinding.ItemConferenceParticipantBinding
import io.reactivex.rxjava3.disposables.Disposable

/*
class ParticipantView(val participantBinding: ItemConferenceParticipantBinding? = null, val addBinding: ItemConferenceAddBinding? = null)
    : RecyclerView.ViewHolder(participantBinding?.root ?: addBinding!!.root) {
    var disposable: Disposable? = null
}*/

class ParticipantView(val participantBinding: ItemConferenceParticipantBinding): RecyclerView.ViewHolder(participantBinding.root) {
    var disposable: Disposable? = null
}
