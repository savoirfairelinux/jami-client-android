/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Authors: Rayan Osseiran <rayan.osseiran@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.DrawableImageViewTarget;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import cx.ring.R;
import cx.ring.model.Ringtone;
import cx.ring.utils.Log;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class RingtoneAdapter extends RecyclerView.Adapter<RingtoneAdapter.RingtoneViewHolder> {

    private final String TAG = RingtoneAdapter.class.getSimpleName();
    private List<Ringtone> ringtoneList;
    private int currentlySelectedPosition = 1; // default item
    private MediaPlayer mp = new MediaPlayer();
    private Subject<Ringtone> ringtoneSubject = PublishSubject.create();

    class RingtoneViewHolder extends RecyclerView.ViewHolder {


        private TextView name;
        private ImageView isSelected, isPlaying, ringtoneIcon;


        RingtoneViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.item_ringtone_name);
            isSelected = view.findViewById(R.id.item_ringtone_selected);
            isPlaying = view.findViewById(R.id.item_ringtone_playing);
            ringtoneIcon = view.findViewById(R.id.item_ringtone_icon);
            Glide.with(view.getContext())
                    .load(R.raw.baseline_graphic_eq_black_24dp)
                    .placeholder(R.drawable.ic_graphic_eq_black_24dp)
                    .into(new DrawableImageViewTarget(isPlaying));
        }
    }


    public RingtoneAdapter(Context context) {
        File ringtonePath = new File(context.getFilesDir(), "ringtones");
        Drawable ringtoneIcon = context.getResources().getDrawable(R.drawable.ic_notifications_active_black_24dp);
        ringtoneList = Arrays.asList(
                // TODO, Populate with correct names/paths when ready
                new Ringtone("Silent", null, context.getResources().getDrawable(R.drawable.ic_notifications_off_black_24dp)),
                new Ringtone("Classic", (new File(ringtonePath, "Jami1 - AfroNigeria.opus")).getAbsolutePath(), ringtoneIcon),
                new Ringtone("Nigeria", (new File(ringtonePath, "Jami2 - AfroNigeria.opus")).getAbsolutePath(), ringtoneIcon),
                new Ringtone("Acoustic Guitar", (new File(ringtonePath, "Jami3 - AcousticGuitar.opus")).getAbsolutePath(), ringtoneIcon),
                new Ringtone("Electric Guitar", (new File(ringtonePath, "Jami4 - ElectricGuitar.opus")).getAbsolutePath(), ringtoneIcon),
                new Ringtone("India", (new File(ringtonePath, "Jami5 - AcousticIndia.opus")).getAbsolutePath(), ringtoneIcon),
                new Ringtone("Soft", (new File(ringtonePath, "Jami6 - RingSoft.opus")).getAbsolutePath(), ringtoneIcon),
                new Ringtone("Tribal", (new File(ringtonePath, "Jami7 - RingTribal.opus")).getAbsolutePath(), ringtoneIcon),
                new Ringtone("Firefly", (new File(ringtonePath, "Jami8 - RingFirefly.opus")).getAbsolutePath(), ringtoneIcon),
                new Ringtone("Electric Cloud", (new File(ringtonePath, "Jami9 - ElectroCloud.opus")).getAbsolutePath(), ringtoneIcon),
                new Ringtone("Urban Trap", (new File(ringtonePath, "Jami10 - UrbanTrap.opus")).getAbsolutePath(), ringtoneIcon));
    }

    @Override
    @NonNull
    public RingtoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RingtoneViewHolder viewHolder = new RingtoneViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ringtone, parent, false));
        configureRingtoneView(viewHolder);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RingtoneViewHolder holder, int position) {
        final Ringtone ringtone = ringtoneList.get(position);
        holder.name.setText(ringtone.getName());
        holder.ringtoneIcon.setImageDrawable((Drawable) ringtone.getRingtoneIcon());
        holder.isSelected.setVisibility((ringtone.isSelected() ? View.VISIBLE : View.INVISIBLE));
        holder.isPlaying.setVisibility((ringtone.isPlaying() ? View.VISIBLE : View.INVISIBLE));
    }

    @Override
    public int getItemCount() {
        return ringtoneList.size();
    }

    private void configureRingtoneView(RingtoneViewHolder viewHolder) {
        viewHolder.itemView.setOnClickListener(view -> {
            if (currentlySelectedPosition == viewHolder.getAdapterPosition() && mp.isPlaying()) {
                stopPreview();
                return;
            } else {
                resetState();
            }

            currentlySelectedPosition = viewHolder.getAdapterPosition();
            Ringtone ringtone = ringtoneList.get(currentlySelectedPosition);
            try {
                mp.setDataSource(ringtone.getRingtonePath());
                mp.prepare();
                mp.start();
                ringtone.setPlaying(true);
            } catch (IOException | IllegalStateException | NullPointerException e) {
                stopPreview();
                Log.e(TAG, "Error previewing ringtone", e);
            } finally {
                ringtoneSubject.onNext(ringtone);
                ringtone.setSelected(true);
                notifyItemChanged(currentlySelectedPosition);
            }
            mp.setOnCompletionListener(mp ->
                    stopPreview());
        });
    }

    /**
     * Stops the preview from playing and disables the playing animation
     */
    public void stopPreview() {
        if(mp.isPlaying())
            mp.stop();
        mp.reset();
        ringtoneList.get(currentlySelectedPosition).setPlaying(false);
        notifyItemChanged(currentlySelectedPosition);
    }

    public void releaseMediaPlayer() {
        mp.release();
    }

    /**
     * Deselects the current item and stops the preview
     */
    public void resetState() {
        ringtoneList.get(currentlySelectedPosition).setSelected(false);
        stopPreview();
    }

    /**
     * Sets the default ringtone (classic)
     */
    public void setDefault() {
        Ringtone ringtone = ringtoneList.get(1);
        currentlySelectedPosition = 1;
        ringtone.setSelected(true);
        ringtoneSubject.onNext(ringtone);
        notifyItemChanged(currentlySelectedPosition);
    }

    /**
     * Sets the ringtone from the user settings
     * @param path the ringtone path
     * @param enabled true if the user did not select silent
     */
    public void selectDefaultItem(String path, boolean enabled) {
        if (!enabled) {
            currentlySelectedPosition = 0;
        } else {
            // ignore first element because it represents silent and has a null path (checked for before)
            for (int ringtoneIndex = 1; ringtoneIndex < ringtoneList.size(); ringtoneIndex++) {
                if (ringtoneList.get(ringtoneIndex).getRingtonePath().equals(path)) {
                    currentlySelectedPosition = ringtoneIndex;
                }
            }
        }
        ringtoneList.get(currentlySelectedPosition).setSelected(true);
        notifyItemChanged(currentlySelectedPosition);
    }

    public Observable<Ringtone> getRingtoneSubject() {
        return ringtoneSubject;
    }

}
