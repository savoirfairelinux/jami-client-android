/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.client;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import cx.ring.R;

public class EmojiChooserBottomSheet extends BottomSheetDialogFragment {

    interface IEmojiSelected {
        void onEmojiSelected(String emoji);
    }

    private IEmojiSelected callback;

    public void setCallback(IEmojiSelected cb) {
        callback = cb;
    }

    private class EmojiView extends RecyclerView.ViewHolder {
        TextView view;
        String emoji;

        EmojiView(@NonNull View itemView) {
            super(itemView);
            view = (TextView) itemView;
            itemView.setOnClickListener(v -> {
                if (callback != null)
                    callback.onEmojiSelected(emoji);
                dismiss();
            });
        }
    }

    class ColorAdapter extends RecyclerView.Adapter<EmojiView>  {
        private final String[] emojis;

        public ColorAdapter(@ArrayRes int arrayResId) {
            emojis = getResources().getStringArray(arrayResId);
        }

        @NonNull
        @Override
        public EmojiView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_emoji, parent, false);
            return new EmojiView(v);
        }

        @Override
        public void onBindViewHolder(@NonNull EmojiView holder, int position) {
            holder.emoji = emojis[position];
            holder.view.setText(holder.emoji);
        }

        @Override
        public int getItemCount() {
            return emojis.length;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerView view = (RecyclerView) inflater.inflate(R.layout.frag_color_chooser, container);
        view.setAdapter(new ColorAdapter(R.array.conversation_emojis));
        return view;
    }
}
