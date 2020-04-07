/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package cx.ring.tv.contact;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.leanback.widget.Presenter;

import cx.ring.R;
import cx.ring.tv.conversation.TvConversationFragment;
import cx.ring.model.TVListViewModel;

public class TVContactDetailPresenter extends Presenter {

    public static final String FRAGMENT_TAG = "conversation";

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.tv, viewGroup, false);
        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object object) {
        ((CustomViewHolder) viewHolder).bind((TVListViewModel) object);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }

    public class CustomViewHolder extends Presenter.ViewHolder {

        public CustomViewHolder(View view) {
            super(view);
        }

        public void bind(TVListViewModel object) {
            String id = object.getContact().getRingUsername();
            String displayName = object.getContact().getDisplayName();
            Fragment fragment = TvConversationFragment.newInstance(id, displayName);
            FragmentManager fragmentManager = ((TVContactActivity) view.getContext()).getSupportFragmentManager();

            fragmentManager.beginTransaction()
                    .replace(R.id.content, fragment, FRAGMENT_TAG)
                    .commit();
        }
    }

}