/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package net.jami.utils;

import net.jami.services.AccountService;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

public class NameLookupInputHandler {
    private static final int WAIT_DELAY = 350;
    private final WeakReference<AccountService> mAccountService;
    private final String mAccountId;
    private final Timer timer = new Timer(true);
    private NameTask lastTask = null;

    public NameLookupInputHandler(AccountService accountService, String accountId) {
        mAccountService = new WeakReference<>(accountService);
        mAccountId = accountId;
    }

    public void enqueueNextLookup(String text) {
        if (lastTask != null) {
            lastTask.cancel();
        }
        lastTask = new NameTask(text);
        timer.schedule(lastTask, WAIT_DELAY);
    }

    private class NameTask extends TimerTask {
        private final String mTextToLookup;

        NameTask(String name) {
            mTextToLookup = name;
        }

        @Override
        public void run() {
            final AccountService accountService = mAccountService.get();
            if (accountService != null) {
                accountService.lookupName(mAccountId, "", mTextToLookup);
            }
        }
    }
}