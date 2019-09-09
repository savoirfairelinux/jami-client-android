/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien Desousa <hadrien.desousa@savoirfairelinux.com>
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
package cx.ring.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;

public final class NetworkUtils {
    /**
     * Get the network info
     */
    public static NetworkInfo getNetworkInfo(Context context) {
        if (context == null)
            return null;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return null;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected())
            return activeNetwork;
        else {
            for (Network n: cm.getAllNetworks()) {
                NetworkInfo nInfo = cm.getNetworkInfo(n);
                if(nInfo != null && nInfo.isConnected())
                    return nInfo;
            }
        }
        return activeNetwork;
    }

    public static boolean isConnectivityAllowed(Context context) {
        NetworkInfo info = NetworkUtils.getNetworkInfo(context);
        return info != null && info.isConnected();
    }
    public static boolean isPushAllowed(Context context, boolean allowMobile) {
        if (allowMobile)
            return true;
        NetworkInfo info = NetworkUtils.getNetworkInfo(context);
        return info != null && info.getType() != ConnectivityManager.TYPE_MOBILE;
    }
}
