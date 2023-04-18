package cx.ring.adapters

import ReactionChipAdapter
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import cx.ring.databinding.ItemReactionVisualizerBinding
import cx.ring.views.AvatarDrawable
import net.jami.model.ContactViewModel
import net.jami.model.Interaction

/**
 * Reaction visualizer (to know who put which reaction)
 *
 * @param context The context of the application
 * @param removeReactionListener A listener to remove a reaction.
 * @param items The list of items to display
 */
class ReactionVisualizerAdapter(
    private val context: Context,
    private val removeReactionListener: (Interaction) -> Unit,
    private var items: List<Pair<ContactViewModel, List<Interaction>>> = emptyList()
) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Pair<ContactViewModel, List<Interaction>> = items[position]

    override fun getItemId(position: Int): Long =
        items[position].first.contact.uri.rawUriString.hashCode().toLong()

    override fun hasStableIds(): Boolean = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val binding: ItemReactionVisualizerBinding

        if (view == null) {
            // If the view does not exist yet,
            // we inflate it from the corresponding XML layout using a LayoutBinding object
            binding = ItemReactionVisualizerBinding.inflate(
                LayoutInflater.from(context), parent, false
            )
            view = binding.root
            view.tag = binding
        } else
        // If the view already exists, we retrieve our LayoutBinding object from the view
            binding = view.tag as ItemReactionVisualizerBinding

        // We retrieve the current item from our data model based on its position
        val data = getItem(position)
        // Now we can update the view with data

        val contactViewModel = data.first

        binding.contactName.text = data.first.displayName
        binding.contactAvatar.setImageDrawable(
            AvatarDrawable.Builder()
                .withContact(contactViewModel)
                .withCircleCrop(true)
                .build(view.context)
        )
        // We set the adapter for the reaction chips.
        binding.contactReactionsTable.adapter =
            ReactionChipAdapter(context, data, removeReactionListener)

        // We return the updated view
        return view
    }

    /**
     * Update dataset and reload view.
     */
    fun setValues(reactions: List<Pair<ContactViewModel, List<Interaction>>>) {
        items = reactions
        notifyDataSetChanged()
    }
}