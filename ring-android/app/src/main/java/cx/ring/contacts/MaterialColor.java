/*
 * Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.contacts;

import android.content.Context;

import cx.ring.R;

public enum MaterialColor {
    RED(R.color.red_400, R.color.red_700, R.color.red_700, R.color.red_900, "red"),
    PINK(R.color.pink_400, R.color.pink_700, R.color.pink_700, R.color.pink_900, "pink"),
    PURPLE(R.color.purple_400, R.color.purple_700, R.color.purple_700, R.color.purple_900, "purple"),
    DEEP_PURPLE(R.color.deep_purple_400, R.color.deep_purple_700, R.color.deep_purple_700, R.color.deep_purple_900, "deep_purple"),
    INDIGO(R.color.indigo_400, R.color.indigo_700, R.color.indigo_700, R.color.indigo_900, "indigo"),
    BLUE(R.color.blue_500, R.color.blue_700, R.color.blue_700, R.color.blue_900, "blue"),
    LIGHT_BLUE(R.color.light_blue_500, R.color.light_blue_700, R.color.light_blue_700, R.color.light_blue_900, "light_blue"),
    CYAN(R.color.cyan_500, R.color.cyan_700, R.color.cyan_700, R.color.cyan_900, "cyan"),
    TEAL(R.color.teal_500, R.color.teal_700, R.color.teal_700, R.color.teal_900, "teal"),
    GREEN(R.color.green_500, R.color.green_700, R.color.green_700, R.color.green_900, "green"),
    LIGHT_GREEN(R.color.light_green_600, R.color.light_green_700, R.color.light_green_700, R.color.light_green_900, "light_green"),
    LIME(R.color.lime_500, R.color.lime_700, R.color.lime_700, R.color.lime_900, "lime"),
    YELLOW(R.color.yellow_500, R.color.yellow_700, R.color.yellow_700, R.color.yellow_900, "yellow"),
    AMBER(R.color.amber_600, R.color.amber_700, R.color.amber_700, R.color.amber_900, "amber"),
    ORANGE(R.color.orange_500, R.color.orange_700, R.color.orange_700, R.color.orange_900, "orange"),
    DEEP_ORANGE(R.color.deep_orange_500, R.color.deep_orange_700, R.color.deep_orange_700, R.color.deep_orange_900, "deep_orange"),
    BROWN(R.color.brown_500, R.color.brown_700, R.color.brown_700, R.color.brown_900, "brown"),
    GREY(R.color.grey_500, R.color.grey_700, R.color.grey_700, R.color.grey_900, "grey"),
    BLUE_GREY(R.color.blue_grey_500, R.color.blue_grey_700, R.color.blue_grey_700, R.color.blue_grey_900, "blue_grey");

    private final int conversationColorLight;
    private final int actionBarColorLight;
    private final int statusBarColorLight;
    private final int conversationColorDark;
    private final String serialized;

    MaterialColor(int lightColor, int darkColor,
                  int lightStatusBarColor, int darkStatusBarColor,
                  String serialized) {
        this.conversationColorLight = lightColor;
        this.actionBarColorLight = darkColor;
        this.statusBarColorLight = lightStatusBarColor;
        this.conversationColorDark = darkStatusBarColor;
        this.serialized = serialized;
    }

    public boolean represents(Context context, int colorValue) {
        return context.getResources().getColor(conversationColorDark) == colorValue ||
                context.getResources().getColor(conversationColorLight) == colorValue ||
                context.getResources().getColor(actionBarColorLight) == colorValue ||
                context.getResources().getColor(statusBarColorLight) == colorValue;
    }

    public String serialize() {
        return serialized;
    }

    public static MaterialColor fromSerialized(String serialized) throws UnknownColorException {
        for (MaterialColor color : MaterialColor.values()) {
            if (color.serialized.equals(serialized)) return color;
        }

        throw new UnknownColorException("Unknown color: " + serialized);
    }

    public static class UnknownColorException extends Exception {
        public UnknownColorException(String message) {
            super(message);
        }
    }
}