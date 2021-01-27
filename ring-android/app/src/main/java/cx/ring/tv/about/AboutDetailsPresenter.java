/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package cx.ring.tv.about;

import android.content.Context;
import androidx.leanback.widget.Presenter;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import cx.ring.databinding.DetailViewContentBinding;
import cx.ring.tv.cards.iconcards.IconCard;

public class AboutDetailsPresenter extends Presenter {
    private Context mContext;
    private DetailViewContentBinding binding;

    public AboutDetailsPresenter(Context context) {
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        binding = DetailViewContentBinding.inflate(LayoutInflater.from(mContext));
        return new ViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object itemData) {
        IconCard card = (IconCard) itemData;
        binding.primaryText.setText(card.getTitle());
        binding.extraText.setText(card.getDescription());
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
