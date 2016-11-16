/**
 * Copyright (C) 2016 by Savoir-faire Linux
 * Author : Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.model;

public class Phone {

    // TODO: make sure this is usefull since a SipUri should already know this
    private NumberType mNumberType;

    private SipUri mNumber;
    private int mCategory; // Home, work, custom etc.
    private String mLabel;

    public Phone(SipUri number, int category) {
        mNumberType = NumberType.UNKNOWN;
        mNumber = number;
        mLabel = null;
        mCategory = category;
    }

    public Phone(String number, int category) {
        this(number, category, null);
    }

    public Phone(String num, int cat, String label) {
        this(num, cat, label, NumberType.UNKNOWN);
    }

    public Phone(String number, int category, String label, NumberType numberType) {
        mNumberType = numberType;
        mNumber = new SipUri(number);
        mLabel = label;
        mCategory = category;
    }

    public SipUri getNumber() {
        return mNumber;
    }

    public void setNumber(String number) {
        setNumber(new SipUri(number));
    }

    public static String getShortenedNumber(String number) {
        if (number != null && !number.isEmpty() && number.length() > 18) {
            return number.substring(0, 18).concat("…");
        }
        return number;
    }

    public NumberType getNumbertype() {
        return mNumberType;
    }

    public void setNumber(SipUri number) {
        this.mNumber = number;
    }

    public int getCategory() {
        return mCategory;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setNumberType(NumberType numberType) {
        this.mNumberType = numberType;
    }

    public enum NumberType {
        UNKNOWN(0),
        TEL(1),
        SIP(2),
        IP(2),
        RING(3);

        private final int type;

        NumberType(int t) {
            type = t;
        }

        private static final NumberType[] VALS = NumberType.values();

        public static NumberType fromInteger(int id) {
            for (NumberType type : VALS) {
                if (type.type == id) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }
}
