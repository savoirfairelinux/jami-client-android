/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package cx.ring.account;

import cx.ring.model.Account;
import cx.ring.mvp.AccountCreationModel;
import ezvcard.VCard;
import io.reactivex.Single;

public interface AccountWizardView {

    void goToHomeCreation();

    void goToSipCreation();

    void displayProgress(boolean display);

    void displayCreationError();

    void blockOrientation();

    void finish(boolean affinity);

    Single<VCard> saveProfile(Account account, AccountCreationModel accountCreationModel);

    void displayGenericError();

    void displayNetworkError();

    void displayCannotBeFoundError();

    void displaySuccessDialog();

    void goToProfileCreation(AccountCreationModel accountCreationModel);
}