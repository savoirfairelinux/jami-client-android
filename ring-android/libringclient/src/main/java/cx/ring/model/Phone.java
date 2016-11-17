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

    // TODO: make sure this is usefull since a Uri should already know this
    private NumberType mNumberType;

    private Uri mNumber;
    private int mCategory; // Home, work, custom etc.
    private String mLabel;

    public Phone(Uri num, int cat) {
        setNumberType(NumberType.UNKNOWN);
        setNumber(num);
        setLabel(null);
        setCategory(cat);
    }

    public Phone(String num, int cat) {
        this(num, cat, null);
    }

    public Phone(String num, int cat, String label) {
        setNumberType(NumberType.UNKNOWN);
        setCategory(cat);
        setNumber(new Uri(num));
        this.setLabel(label);
    }

    public Phone(String num, int cat, String label, NumberType nty) {
        setNumberType(nty);
        setNumber(new Uri(num));
        this.setLabel(label);
        setCategory(cat);
    }

    public NumberType getType() {
        return getNumbertype();
    }

    public void setType(int type) {
        this.setNumberType(NumberType.fromInteger(type));
    }

    public Uri getNumber() {
        return mNumber;
    }

    public void setNumber(String number) {
        this.setNumber(new Uri(number));
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

    public void setNumber(Uri number) {
        this.mNumber = number;
    }

    public int getCategory() {
        return mCategory;
    }

    public void setCategory(int category) {
        this.mCategory = category;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        this.mLabel = label;
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

        public static NumberType fromInteger(int _id) {
            for (NumberType v : VALS)
                if (v.type == _id)
                    return v;
            return UNKNOWN;
        }
    }
}
