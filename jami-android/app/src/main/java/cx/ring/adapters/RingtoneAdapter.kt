/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.adapters

import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.DrawableImageViewTarget
import cx.ring.R
import cx.ring.adapters.RingtoneAdapter.RingtoneViewHolder
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.model.Ringtone
import java.io.IOException

class RingtoneAdapter(private val ringtoneList: List<Ringtone>) : RecyclerView.Adapter<RingtoneViewHolder>() {
    private var currentlySelectedPosition = 1 // default item
    private val mp = MediaPlayer()
    private val ringtoneSubject: Subject<Ringtone> = PublishSubject.create()

    class RingtoneViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.item_ringtone_name)
        val isSelected: ImageView = view.findViewById(R.id.item_ringtone_selected)
        val isPlaying: ImageView = view.findViewById(R.id.item_ringtone_playing)
        val ringtoneIcon: ImageView = view.findViewById(R.id.item_ringtone_icon)

        init {
            Glide.with(view.context)
                .load(R.raw.baseline_graphic_eq_black_24dp)
                .placeholder(R.drawable.baseline_graphic_eq_24)
                .into(DrawableImageViewTarget(isPlaying))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RingtoneViewHolder {
        val viewHolder = RingtoneViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_ringtone, parent, false))
        configureRingtoneView(viewHolder)
        return viewHolder
    }

    override fun onBindViewHolder(holder: RingtoneViewHolder, position: Int) {
        val ringtone = ringtoneList[position]
        holder.name.text = ringtone.name
        holder.ringtoneIcon.setImageDrawable(ringtone.ringtoneIcon as Drawable)
        holder.isSelected.visibility = if (ringtone.isSelected) View.VISIBLE else View.INVISIBLE
        holder.isPlaying.visibility = if (ringtone.isPlaying) View.VISIBLE else View.INVISIBLE
    }

    override fun getItemCount(): Int {
        return ringtoneList.size
    }

    private fun configureRingtoneView(viewHolder: RingtoneViewHolder) {
        viewHolder.itemView.setOnClickListener {
            if (currentlySelectedPosition == viewHolder.adapterPosition && mp.isPlaying) {
                stopPreview()
                return@setOnClickListener
            } else {
                resetState()
            }
            currentlySelectedPosition = viewHolder.adapterPosition
            val ringtone = ringtoneList[currentlySelectedPosition]
            try {
                mp.setDataSource(ringtone.ringtonePath)
                mp.prepare()
                mp.start()
                ringtone.isPlaying = true
            } catch (e: IOException) {
                stopPreview()
                Log.e(TAG, "Error previewing ringtone", e)
            } catch (e: IllegalStateException) {
                stopPreview()
                Log.e(TAG, "Error previewing ringtone", e)
            } catch (e: NullPointerException) {
                stopPreview()
                Log.e(TAG, "Error previewing ringtone", e)
            } finally {
                ringtoneSubject.onNext(ringtone)
                ringtone.isSelected = true
                notifyItemChanged(currentlySelectedPosition)
            }
            mp.setOnCompletionListener { stopPreview() }
        }
    }

    /**
     * Stops the preview from playing and disables the playing animation
     */
    private fun stopPreview() {
        if (mp.isPlaying) mp.stop()
        mp.reset()
        ringtoneList[currentlySelectedPosition].isPlaying = false
        notifyItemChanged(currentlySelectedPosition)
    }

    fun releaseMediaPlayer() {
        mp.release()
    }

    /**
     * Deselects the current item and stops the preview
     */
    fun resetState() {
        ringtoneList[currentlySelectedPosition].isSelected = false
        stopPreview()
    }

    /**
     * Sets the ringtone from the user settings
     * @param path the ringtone path
     * @param enabled true if the user did not select silent
     */
    fun selectDefaultItem(path: String, enabled: Boolean) {
        if (!enabled) {
            currentlySelectedPosition = 0
        } else {
            // ignore first element because it represents silent and has a null path (checked for before)
            for (ringtoneIndex in 1 until ringtoneList.size) {
                if (ringtoneList[ringtoneIndex].ringtonePath == path) {
                    currentlySelectedPosition = ringtoneIndex
                }
            }
        }
        ringtoneList[currentlySelectedPosition].isSelected = true
        notifyItemChanged(currentlySelectedPosition)
    }

    fun getRingtone(): Observable<Ringtone> {
        return ringtoneSubject
    }

    companion object {
        private val TAG = RingtoneAdapter::class.simpleName!!
    }
}