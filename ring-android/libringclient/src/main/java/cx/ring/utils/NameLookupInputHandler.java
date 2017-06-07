/*
 *  Copyright (C) 2016-2017 Savoir-faire Linux Inc.
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
package cx.ring.utils;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import cx.ring.services.ContactService;

public class NameLookupInputHandler {
    private static final int WAIT_DELAY = 350;
    private WeakReference<ContactService> mContactService;
    private final Timer timer = new Timer(true);
    private NameTask lastTask = null;

    public NameLookupInputHandler(WeakReference<ContactService> accountService) {
        mContactService = accountService;
    }

    public void enqueueNextLookup(String text) {
        if (lastTask != null)
            lastTask.cancel();
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
            final ContactService service = mContactService.get();
            if (service != null) {
                service.lookupName("", "", mTextToLookup);
            }
        }
    }
}