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
package cx.ring.model;

public class Ringtone {


    private String mName, mRingtonePath;
    private boolean isSelected, isPlaying;
    private Object mRingtoneIcon;


    public Ringtone(String name, String path, Object ringtoneIcon) {
        mName = name;
        mRingtonePath = path;
        isSelected = false;
        isPlaying = false;
        mRingtoneIcon = ringtoneIcon;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getRingtonePath() {
        return mRingtonePath;
    }

    public void setRingtonePath(String ringtonePath) {
        mRingtonePath = ringtonePath;
    }

    public Object getRingtoneIcon() {
        return mRingtoneIcon;
    }

    public void setRingtoneIcon(Object ringtoneIcon) {
        mRingtoneIcon = ringtoneIcon;
    }


}
