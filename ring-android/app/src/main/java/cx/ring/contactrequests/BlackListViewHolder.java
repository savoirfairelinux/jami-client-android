/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
 *  Author: Adrien Beraud <adrien.beraud@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.contactrequests;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import cx.ring.contacts.AvatarFactory;
import cx.ring.databinding.ItemContactBlacklistBinding;
import net.jami.model.CallContact;

public class BlackListViewHolder extends RecyclerView.ViewHolder {
    private final ItemContactBlacklistBinding binding;

    BlackListViewHolder(View view) {
        super(view);
        binding = ItemContactBlacklistBinding.bind(view);
    }

    void bind(final BlackListListeners clickListener, final CallContact contact) {
        AvatarFactory.loadGlideAvatar(binding.photo, contact);
        binding.displayName.setText(contact.getRingUsername());
        binding.unblock.setOnClickListener(view -> clickListener.onUnblockClicked(contact));
    }

    public interface BlackListListeners {
        void onUnblockClicked(CallContact contact);
    }
}
