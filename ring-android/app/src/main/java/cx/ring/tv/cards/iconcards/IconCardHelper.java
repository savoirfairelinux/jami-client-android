/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Loïc Siret <loic.siret@savoirfairelinux.com>
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
package cx.ring.tv.cards.iconcards;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;

import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.tv.cards.Card;


public final class IconCardHelper {

    private IconCardHelper() {
        /* Helper so no constructor */
    }

    public static IconCard getAboutCardByType(Context pContext, Card.Type type) {
        switch (type) {
            case ABOUT_CONTRIBUTOR:
                return getContributorCard(pContext);
            case ABOUT_LICENCES:
                return getLicencesCard(pContext);
            case ABOUT_VERSION:
                return getVersionCard(pContext);
            case ACCOUNT_ADD_DEVICE:
                return getAccountAddDevice(pContext);
            default:
                return null;
        }
    }

    public static IconCard getVersionCard(Context pContext) {
        return new IconCard(Card.Type.ABOUT_VERSION, pContext.getString(R.string.version_section) + " 1.0" + " " + BuildConfig.VERSION_NAME, "", R.drawable.ic_ring_logo_white);
    }

    public static IconCard getLicencesCard(Context pContext) {
        return new IconCard(Card.Type.ABOUT_LICENCES, pContext.getString(R.string.section_license), formatLicence(pContext), R.drawable.ic_description);
    }

    public static IconCard getContributorCard(Context pContext) {
        return new IconCard(Card.Type.ABOUT_CONTRIBUTOR, pContext.getString(R.string.credits), formatContributors(pContext), R.drawable.ic_face);
    }

    public static IconCard getAccountAddDevice(Context pContext) {
        return new IconCard(Card.Type.ACCOUNT_ADD_DEVICE, pContext.getString(R.string.account_link_button), "",R.drawable.ic_add_white);
    }

    private static CharSequence formatLicence(Context pContext) {
        return Html.fromHtml(pContext.getResources().getString(R.string.license)).toString();
    }

    private static CharSequence formatContributors(Context pContext) {
        Resources res = pContext.getResources();

        SpannableString developedby = new SpannableString(res.getString(R.string.developed_by));
        developedby.setSpan(new UnderlineSpan(), 0, developedby.length(), 0);
        CharSequence developed = res.getString(R.string.credits_developer).replaceAll("\n", "<br/>");
        SpannableString designedby = new SpannableString(res.getString(R.string.designed_by));
        designedby.setSpan(new UnderlineSpan(), 0, designedby.length(), 0);
        CharSequence design = res.getString(R.string.credits_designer).replaceAll("\n", "<br/>");
        return Html.fromHtml("<b><u>" + developedby + "</u></b><br/>" + developed + "<br/><br/><b><u>" + designedby + "</u></b><br/>" + design+"<br/>");
    }
}
