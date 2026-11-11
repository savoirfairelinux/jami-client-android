package cx.ring.adapters

import android.annotation.SuppressLint
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cx.ring.databinding.ItemPendingRequestBinding
import cx.ring.utils.DeviceUtils
import cx.ring.utils.TextUtils
import cx.ring.viewholders.SmartListViewHolder.SmartListListeners
import cx.ring.views.AvatarDrawable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Conversation
import net.jami.services.ConversationFacade

class PendingRequestAdapter(
    private val conversationFacade: ConversationFacade,
    private val listener: SmartListListeners,
    private val parentDisposable: CompositeDisposable
) : ListAdapter<Conversation, PendingRequestAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean =
            oldItem.accountId == newItem.accountId && oldItem.uri.uri == newItem.uri.uri

        // Profile changes are observed; replacement objects need new subscriptions.
        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean =
            oldItem === newItem
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemPendingRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.unbind()
        super.onViewRecycled(holder)
    }

    inner class ViewHolder(private val binding: ItemPendingRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        private val disposable = CompositeDisposable().also { parentDisposable.add(it) }

        fun bind(conversation: Conversation) {
            unbind()
            val timestamp = conversation.request?.timestamp
            binding.requestTime.isVisible = timestamp != null && timestamp > 0L
            binding.requestTime.text = if (timestamp != null && timestamp > 0L)
                DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL)
            else null

            binding.root.setOnClickListener { listener.onItemClick(conversation) }
            binding.root.setOnLongClickListener {
                listener.onItemLongClick(conversation)
                true
            }
            disposable.add(conversationFacade.observeConversation(conversation)
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe({ model ->
                    TextUtils.setConversationTitle(binding.requestTitle, model)
                    binding.photo.setAvatar(AvatarDrawable.Builder()
                        .withViewModel(model)
                        .withCircleCrop(true)
                        .build(binding.photo.context))
                }, { error ->
                    Log.e("PendingRequestAdapter", "Unable to load pending request details", error)
                }))
        }

        fun unbind() {
            disposable.clear()
            binding.requestTitle.text = null
            binding.requestTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            binding.requestTitle.contentDescription = null
            binding.photo.setAvatar(null)
        }
    }
}
