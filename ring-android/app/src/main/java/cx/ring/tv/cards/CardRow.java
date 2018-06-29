/*
 * Copyright (c) 2015 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License
 *  is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing permissions and limitations under
 *  the License.
 */
package cx.ring.tv.cards;

import java.util.List;

/**
 * This class represents a row of cards. In a real world application you might want to store more
 * data than in this example.
 */
public class CardRow {

    // default is a list of cards
    public static final int TYPE_DEFAULT = 0;
    // section header
    public static final int TYPE_SECTION_HEADER = 1;
    // divider
    public static final int TYPE_DIVIDER = 2;

    private int mType = TYPE_DEFAULT;
    // Used to determine whether the row shall use shadows when displaying its cards or not.
    private boolean mShadow = true;
    private String mTitle;
    private List<? extends Card> mCards;

    public CardRow(int pType, boolean pShadow, String pTitle, List<? extends Card> pCards) {
        mType = pType;
        mShadow = pShadow;
        mTitle = pTitle;
        mCards = pCards;
    }

    public int getType() {
        return mType;
    }

    public void setType(int pType) {
        mType = pType;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String pTitle) {
        mTitle = pTitle;
    }

    public boolean useShadow() {
        return mShadow;
    }

    public List<Card> getCards() {
        return (List<Card>) mCards;
    }

    public void setCards(List<Card> pCards) {
        mCards = pCards;
    }

    public void setShadow(boolean pShadow) {
        mShadow = pShadow;
    }
}
