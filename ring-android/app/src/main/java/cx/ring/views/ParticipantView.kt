package cx.ring.views

import androidx.recyclerview.widget.RecyclerView
import cx.ring.databinding.ItemConferenceParticipantBinding
import io.reactivex.rxjava3.disposables.Disposable

class ParticipantView(val binding: ItemConferenceParticipantBinding)
    : RecyclerView.ViewHolder(binding.root) {
    var disposable: Disposable? = null
}