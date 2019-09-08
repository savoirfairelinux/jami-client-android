/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import android.graphics.drawable.BitmapDrawable;
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
                return getAccountAddDeviceCard(pContext);
            case ACCOUNT_EDIT_PROFILE:
                return getAccountManagementCard(pContext);
            case ACCOUNT_SETTINGS:
                return getAccountSettingsCard(pContext);
            case ACCOUNT_SHARE_ACCOUNT:
                return getAccountShareCard(pContext, null);
            default:
                return null;
        }
    }

    public static IconCard getAccountAddDeviceCard(Context pContext) {
        return new IconCard(Card.Type.ACCOUNT_ADD_DEVICE, pContext.getString(R.string.account_link_export_button), "", R.drawable.baseline_add_24);
    }

    public static IconCard getAccountManagementCard(Context pContext) {
        return new IconCard(Card.Type.ACCOUNT_EDIT_PROFILE, pContext.getString(R.string.account_edit_profile), "", R.drawable.baseline_account_card_details);
    }

    public static IconCard getAccountSettingsCard(Context pContext) {
        return new IconCard(Card.Type.ACCOUNT_SETTINGS, pContext.getString(R.string.menu_item_settings), "", R.drawable.baseline_settings_24);
    }

    public static IconCard getAccountShareCard(Context pContext, BitmapDrawable bitmapDrawable) {
        return new IconCard(Card.Type.ACCOUNT_SHARE_ACCOUNT, pContext.getString(R.string.menu_item_share), "", bitmapDrawable);
    }

    public static IconCard getVersionCard(Context pContext) {
        return new IconCard(Card.Type.ABOUT_VERSION, pContext.getString(R.string.version_section) + " " + BuildConfig.VERSION_NAME, "", R.drawable.ic_ring_logo_white_vd);
    }

    public static IconCard getLicencesCard(Context pContext) {
        return new IconCard(Card.Type.ABOUT_LICENCES, pContext.getString(R.string.section_license), formatLicence(pContext), R.drawable.baseline_description_24);
    }

    public static IconCard getContributorCard(Context pContext) {
        return new IconCard(Card.Type.ABOUT_CONTRIBUTOR, pContext.getString(R.string.credits), formatContributors(pContext), R.drawable.baseline_face_24);
    }

    private static CharSequence formatLicence(Context pContext) {
        Resources res = pContext.getResources();

        SpannableString version = new SpannableString(res.getString(R.string.version_section));
        version.setSpan(new UnderlineSpan(), 0, version.length(), 0);
        CharSequence versioned = res.getString(R.string.app_release, BuildConfig.VERSION_NAME);

        SpannableString licence = new SpannableString(res.getString(R.string.section_license));
        licence.setSpan(new UnderlineSpan(), 0, licence.length(), 0);
        CharSequence licenced = res.getString(R.string.license);

        SpannableString copyright = new SpannableString(res.getString(R.string.copyright_section));
        copyright.setSpan(new UnderlineSpan(), 0, copyright.length(), 0);
        CharSequence copyrighted = res.getString(R.string.copyright);


        return Html.fromHtml("<b><u>" + version + "</u></b><br/>" + versioned + "<BR/><BR/>"
                + "<b><u>" + licence + "</u></b><br/>" + licenced + "<BR/><BR/>"
                + "<b><u>" + copyright + "</u></b><br/>" + copyrighted);
    }

    private static CharSequence formatContributors(Context pContext) {
        Resources res = pContext.getResources();

        SpannableString developedby = new SpannableString(res.getString(R.string.developed_by));
        developedby.setSpan(new UnderlineSpan(), 0, developedby.length(), 0);
        CharSequence developed = res.getString(R.string.credits_developer).replaceAll("\n", "<br/>");
        return Html.fromHtml("<b><u>" + developedby + "</u></b><br/>" + developed);
    }
}
