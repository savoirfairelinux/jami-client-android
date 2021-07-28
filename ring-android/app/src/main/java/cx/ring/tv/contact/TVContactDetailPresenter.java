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
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.leanback.widget.Presenter;

import cx.ring.R;
import net.jami.smartlist.SmartListViewModel;
import cx.ring.tv.conversation.TvConversationFragment;
import cx.ring.utils.ConversationPath;

public class TVContactDetailPresenter extends Presenter {

    public static final String FRAGMENT_TAG = "conversation";

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        return new CustomViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.tv, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object object) {
        ((CustomViewHolder) viewHolder).bind((SmartListViewModel) object);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }

    private static class CustomViewHolder extends Presenter.ViewHolder {

        CustomViewHolder(View view) {
            super(view);
        }

        void bind(SmartListViewModel object) {
            Fragment fragment = TvConversationFragment.newInstance(ConversationPath.toBundle(object.getAccountId(), object.getUri()));
            FragmentManager fragmentManager = ((FragmentActivity) view.getContext()).getSupportFragmentManager();

            fragmentManager.beginTransaction()
                    .replace(R.id.content, fragment, FRAGMENT_TAG)
                    .commit();
        }
    }

}