package cx.ring.views;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import cx.ring.databinding.ItemConferenceParticipantBinding;
import io.reactivex.rxjava3.disposables.Disposable;

public class ParticipantView extends RecyclerView.ViewHolder {
    public final ItemConferenceParticipantBinding binding;
    public Disposable disposable = null;

    public ParticipantView(@NonNull ItemConferenceParticipantBinding b) {
        super(b.getRoot());
        binding = b;
    }
}
