package cx.ring.client

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cx.ring.R

class ColorChooserBottomSheet(val onColorSelected: ((Int) -> Unit)? = null) : BottomSheetDialogFragment() {
    private inner class ColorView(itemView: View) : RecyclerView.ViewHolder(itemView)

    private inner class ColorAdapter : RecyclerView.Adapter<ColorView>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ColorView(LayoutInflater.from(parent.context).inflate(R.layout.item_color, parent, false))

        override fun onBindViewHolder(holder: ColorView, position: Int) {
            val color = resources.getColor(colors[position])
            holder.itemView.setOnClickListener {
                onColorSelected?.invoke(color)
                dismiss()
            }
            ImageViewCompat.setImageTintList(holder.itemView as ImageView, ColorStateList.valueOf(color))
        }

        override fun getItemCount() = colors.size
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        (inflater.inflate(R.layout.frag_color_chooser, container) as RecyclerView)
            .apply { adapter = ColorAdapter() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as BottomSheetDialog).behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        return dialog
    }

    companion object {
        private val colors = intArrayOf(
            R.color.pink_500,
            R.color.purple_500, R.color.deep_purple_500,
            R.color.indigo_500, R.color.blue_500,
            R.color.cyan_500, R.color.teal_500,
            R.color.green_500, R.color.light_green_500,
            R.color.grey_500, R.color.lime_500,
            R.color.amber_500, R.color.deep_orange_500,
            R.color.brown_500, R.color.blue_grey_500
        )
    }
}