package cx.ring.adapters

import android.app.ActionBar.LayoutParams
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.marginBottom
import com.google.android.material.chip.Chip
import cx.ring.databinding.ItemReactionLayoutBinding
import cx.ring.databinding.ItemReactionBinding
import cx.ring.views.AvatarDrawable
import net.jami.model.ContactViewModel
import net.jami.model.Interaction
import net.jami.utils.Log
import kotlin.math.ceil

class ReactionAdapter(val context: Context, val listener: (Interaction)->Unit, private var items: List<Pair<ContactViewModel, List<Interaction>>> = emptyList()) : BaseAdapter() {
    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Pair<ContactViewModel, List<Interaction>> = items[position]

    override fun getItemId(position: Int): Long = items[position].first.contact.uri.rawUriString.hashCode().toLong()

    override fun hasStableIds(): Boolean = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val binding: ItemReactionLayoutBinding
        if (view == null) {
            binding = ItemReactionLayoutBinding.inflate(LayoutInflater.from(context))
            view = binding.root
            view.setTag(binding)
        } else
            binding = view.tag as ItemReactionLayoutBinding

        val data = getItem(position)
        val contactViewModel = data.first
        val isUser = contactViewModel.contact.isUser

        binding.contactAvatar.setImageDrawable(AvatarDrawable.Builder()
            .withContact(data.first)
            .withCircleCrop(true)
            .build(view.context))

        binding.contactName.setText(data.first.displayName + if(isUser) " (is user)" else "")
//        5.toString()
//        val numberItems = data.second.count().toDouble()
//        val row = ceil(numberItems / 5).toInt()

        binding.contactReactionsTable.removeAllViews()



        var x=0
        var x2=0
        lateinit var newRow:TableRow
        for (i in data.second){
            if (x==0){
                newRow = TableRow(context) // TODO aimanter a droite
            }
            x2+=1

            val chipToAdd = ItemReactionBinding.inflate(LayoutInflater.from(context))
            val chipView = chipToAdd.root
            chipView.tag = chipToAdd

            chipToAdd.chip.text = i.body

            if (!isUser) {
                chipToAdd.chip.isClickable = false
            }
            else {
                chipToAdd.chip.isChecked = true
                chipToAdd.chip.setOnClickListener{listener.invoke(i)}
            }

//            val newChip = Chip(context)
            newRow.addView(chipView)
            x+=1

            if ((x==5) or (data.second.size==x2)){
                binding.contactReactionsTable.addView(newRow)
                x=0
            }


        }


//        for(i in 0 until binding.contactReactionsTable.childCount){
//            Log.w("DEVDEBUG", "ROW $i $isUser")
//            val child = binding.contactReactionsTable.getChildAt(i) as TableRow
//
//            val newChip = Chip(context)
//
//            child.addView(newChip)
//
//
//            for(j in 0 until child.childCount){
//                Log.w("DEVDEBUG", "\tITEM $j")
//                val item= child.getChildAt(j) as Chip
//                item.isChecked = false
//
////                if (!isUser) {
////                    Log.w("DEVDEBUG", "\tNOT CHECKABLE $j")
////                    item.isCheckable = false
////                }
//            }
//        }


//        val r = TableRow(context)
//        r.layoutParams = WindowManager.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
//
//        val t0 = TextView(context)
//        t0.setPadding(5, 5, 5, 5)
//        t0.setText("a")
//
//        val t1 = TextView(context)
//        t1.setPadding(5, 5, 5, 5)
//        t1.setText("a")
//
//        val t2 = TextView(context)
//        t2.setPadding(5, 5, 5, 5)
//        t2.setText("a")
//
//        val t3 = TextView(context)
//        t3.setPadding(5, 5, 5, 5)
//        t3.setText("a")
//
//        val t4 = TextView(context)
//        t4.setPadding(5, 5, 5, 5)
//        t4.setText("a")
//
//        r.addView(t0)
//        r.addView(t1)
//        r.addView(t2)
//        r.addView(t3)
//        r.addView(t4)
//
//        binding.test.addView(r)



//        binding.test.addView()



        // afficher emojis


        return view
    }

    fun setValues(reactions: List<Pair<ContactViewModel, List<Interaction>>>) {
        items = reactions
        notifyDataSetChanged()
    }
}