package cx.ring.client

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cx.ring.R

class ColorChooserBottomSheet : BottomSheetDialogFragment() {
    interface IColorSelected {
        fun onColorSelected(color: Int)
    }

    private var callback: IColorSelected? = null
    fun setCallback(cb: IColorSelected?) {
        callback = cb
    }

    private inner class ColorView(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val view: ImageView = itemView as ImageView
        var color = 0

        init {
            itemView.setOnClickListener {
                if (callback != null) callback!!.onColorSelected(color)
                dismiss()
            }
        }
    }

    private inner class ColorAdapter : RecyclerView.Adapter<ColorView>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorView {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_color, parent, false)
            return ColorView(v)
        }

        override fun onBindViewHolder(holder: ColorView, position: Int) {
            val color = colors[position]
            holder.color = resources.getColor(color)
            ImageViewCompat.setImageTintList(holder.view, ColorStateList.valueOf(holder.color))
        }

        override fun getItemCount(): Int {
            return colors.size
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.frag_color_chooser, container) as RecyclerView
        view.adapter = ColorAdapter()
        return view
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