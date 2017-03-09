/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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

package cx.ring.utils;

import ezvcard.Ezvcard;
import ezvcard.VCard;

public class TrustRequestUtils {

    //TODO : modify this function when the payload protocol will be make
    public static Tuple<VCard, String> parsePayload(String payload) {
        String message = "";
        VCard vCard = Ezvcard.parse(payload).first();
        return new Tuple<>(vCard, message);
    }

    //TODO : modify this function when the payload protocol will be make
    public static String compressPayload(VCard vCard, String message) {
        return VCardUtils.vcardToString(vCard);
    }
}
