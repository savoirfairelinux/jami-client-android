/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Michel Schmit <michel.schmit@savoirfairelinux.com>
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
package cx.ring.tv.model;

import android.support.annotation.NonNull;

import cx.ring.model.CallContact;

public class TVListViewModel implements Comparable<TVListViewModel> {
    private CallContact mCallContact;
    private boolean isOnline;

    public TVListViewModel(CallContact pCallContact, boolean pIsOnline) {
        mCallContact = pCallContact;
        isOnline = pIsOnline;
    }

    public TVListViewModel(CallContact pCallContact) {
        mCallContact = pCallContact;
        isOnline = false;
    }

    public CallContact getCallContact() {
        return mCallContact;
    }

    public void setCallContact(CallContact pCallContact) {
        mCallContact = pCallContact;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean pOnline) {
        isOnline = pOnline;
    }

    @Override
    public boolean equals(Object pO) {
        if (this == pO) return true;
        if (pO == null || getClass() != pO.getClass()) return false;

        TVListViewModel tvListViewModel = (TVListViewModel) pO;
        return tvListViewModel.mCallContact.equals(mCallContact) && tvListViewModel.isOnline == isOnline;
    }

    @Override
    public int compareTo(@NonNull TVListViewModel o) {
        return mCallContact.getDisplayName().compareTo(o.getCallContact().getDisplayName());
    }

    @Override
    public String toString() {
        return mCallContact.toString() + " " + isOnline;
    }
}
