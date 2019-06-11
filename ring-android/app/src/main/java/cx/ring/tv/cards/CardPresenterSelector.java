/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package cx.ring.tv.cards;

import android.content.Context;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;

import java.security.InvalidParameterException;
import java.util.HashMap;

import cx.ring.R;
import cx.ring.tv.cards.contacts.ContactCardPresenter;
import cx.ring.tv.cards.iconcards.IconCardPresenter;

/**
 * This PresenterSelector will decide what Presenter to use depending on a given card's type.
 */
public class CardPresenterSelector extends PresenterSelector {

    private final Context mContext;
    private final HashMap<Card.Type, Presenter> presenters = new HashMap<>();

    public CardPresenterSelector(Context context) {
        mContext = context;
    }

    @Override
    public Presenter getPresenter(Object item) {
        if (!(item instanceof Card)) throw new RuntimeException(
                String.format("The PresenterSelector only supports data items of type '%s'",
                        Card.class.getName()));
        Card card = (Card) item;
        Presenter presenter = presenters.get(card.getType());
        if (presenter == null) {
            switch (card.getType()) {
                case ABOUT_VERSION:
                case ABOUT_CONTRIBUTOR:
                case ABOUT_LICENCES:
                case ACCOUNT_ADD_DEVICE:
                case ACCOUNT_EDIT_PROFILE:
                case ACCOUNT_SHARE_ACCOUNT:
                case ACCOUNT_SETTINGS:
                    presenter = new IconCardPresenter(mContext);
                    break;
                case SEARCH_RESULT:
                    presenter = new ContactCardPresenter(mContext, R.style.SearchCardTheme);
                    break;
                case CONTACT:
                    presenter = new ContactCardPresenter(mContext, R.style.ContactCardTheme);
                    break;
                case CONTACT_ONLINE:
                    presenter = new ContactCardPresenter(mContext, R.style.ContactCardOnlineTheme);
                    break;
                case CONTACT_WITH_USERNAME:
                    presenter = new ContactCardPresenter(mContext, R.style.ContactCompleteCardTheme);
                    break;
                case CONTACT_WITH_USERNAME_ONLINE:
                    presenter = new ContactCardPresenter(mContext, R.style.ContactCompleteCardOnlineTheme);
                    break;
                default:
                    throw new InvalidParameterException("Uncatched card type");
            }
            presenters.put(card.getType(), presenter);
        }
        return presenter;
    }

}