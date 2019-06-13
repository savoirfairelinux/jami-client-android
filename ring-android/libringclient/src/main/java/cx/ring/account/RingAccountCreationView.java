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

import cx.ring.mvp.AccountCreationModel;

public interface RingAccountCreationView {

    enum UsernameIconStatus {
        ERROR,
        LOADING,
        VALID,
        INVISIBLE
    }

    void enableTextError();

    void disableTextError();

    void showExistingNameError();

    void showInvalidNameError();

    void showValidName(UsernameIconStatus status);

    void resetUsernameViews();

    void showUnknownError();

    void showDaemonFailedToRespond();

    void showInvalidPasswordError(boolean display);

    void showNonMatchingPasswordError(boolean display);

    void displayUsernameBox(boolean display);

    void enableNextButton(boolean enabled);

    void goToAccountCreation(AccountCreationModel accountCreationModel);

}
