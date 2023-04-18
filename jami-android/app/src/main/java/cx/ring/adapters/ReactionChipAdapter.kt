import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cx.ring.databinding.ItemReactionChipBinding
import cx.ring.viewholders.ReactionChipViewHolder
import net.jami.model.ContactViewModel
import net.jami.model.Interaction

class ReactionChipAdapter(
    private val context: Context,
    private var items: Pair<ContactViewModel, List<Interaction>>,
    private val removeReactionListener: (Interaction) -> Unit,
) : RecyclerView.Adapter<ReactionChipViewHolder>() {

    // Create a new ViewHolder for the RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReactionChipViewHolder =
        ReactionChipViewHolder(ItemReactionChipBinding.inflate(LayoutInflater.from(context)))

    // Return the number of items to display
    override fun getItemCount(): Int = items.second.size

    // Bind the data to the views in the ViewHolder
    override fun onBindViewHolder(holder: ReactionChipViewHolder, position: Int) {
        // Get the interaction at the current position
        val interaction = items.second[position]
        holder.binding.chip.text = interaction.body
        // If the contact is the user's contact
        if (items.first.contact.isUser) {
            holder.binding.chip.isCheckable = true
            holder.binding.chip.isChecked = true
            // When the Chip view is clicked, invoke the listener to remove the interaction
            holder.binding.chip.setOnClickListener { removeReactionListener.invoke(interaction) }
            // If the contact is not the user's contact
        } else {
            holder.binding.chip.isCheckable = false
            holder.binding.chip.isChecked = false
            holder.binding.chip.setOnClickListener(null)
        }
    }
}