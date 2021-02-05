package cx.ring.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cx.ring.contacts.AvatarFactory;
import cx.ring.databinding.ItemConferenceParticipantBinding;
import cx.ring.fragments.CallFragment;
import net.jami.model.Contact;
import net.jami.model.Call;
import cx.ring.views.ParticipantView;

public class ConfParticipantAdapter extends RecyclerView.Adapter<ParticipantView> {
    protected final ConfParticipantAdapter.ConfParticipantSelected onSelectedCallback;
    private List<Call> calls = null;

    public ConfParticipantAdapter(@NonNull ConfParticipantSelected cb) {
        onSelectedCallback = cb;
    }

    @NonNull
    @Override
    public ParticipantView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ParticipantView(ItemConferenceParticipantBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantView holder, int position) {
        final Call call = calls.get(position);
        final Contact contact = call.getContact();
        final Context context = holder.itemView.getContext();
        Call.CallStatus status = call.getCallStatus();
        if (status == Call.CallStatus.CURRENT)  {
            holder.binding.displayName.setText(contact.getDisplayName());
            holder.binding.photo.setAlpha(1f);
        } else {
            holder.binding.displayName.setText(String.format("%s\n%s", contact.getDisplayName(), context.getText(CallFragment.callStateToHumanState(status))));
            holder.binding.photo.setAlpha(.5f);
        }
        if (holder.disposable != null)
            holder.disposable.dispose();
        holder.disposable = AvatarFactory.getAvatar(context, contact)
                .subscribe(holder.binding.photo::setImageDrawable);
        holder.itemView.setOnClickListener(view -> onSelectedCallback.onParticipantSelected(view, call));
    }

    @Override
    public int getItemCount() {
        return calls == null ? 0 : calls.size();
    }

    public void updateFromCalls(@NonNull final List<Call> contacts) {
        final List<Call> oldCalls = calls;
        calls = contacts;
        if (oldCalls != null) {
            DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return oldCalls.size();
                }

                @Override
                public int getNewListSize() {
                    return contacts.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return oldCalls.get(oldItemPosition) == contacts.get(newItemPosition);
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return false;
                }
            }).dispatchUpdatesTo(this);
        } else {
            notifyDataSetChanged();
        }
    }

    public interface ConfParticipantSelected {
        void onParticipantSelected(View view, Call contact);
    }
}
