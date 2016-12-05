/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 * <p>
 * Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * If you own a pjsip commercial license you can also redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License
 * as an android library.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.tests.dependencyinjection;

import android.util.Log;

import java.util.Observable;
import java.util.Observer;

import cx.ring.BuildConfig;
import cx.ring.model.DaemonEvent;

class ConfigurationManagerCallback implements Observer {

    private static final String TAG = ConfigurationManagerCallback.class.getSimpleName();

    static public final String ACCOUNTS_CHANGED = BuildConfig.APPLICATION_ID + "accounts.changed";
    static public final String ACCOUNTS_DEVICES_CHANGED = BuildConfig.APPLICATION_ID + "accounts.devicesChanged";
    static public final String ACCOUNTS_EXPORT_ENDED = BuildConfig.APPLICATION_ID + "accounts.exportEnded";
    static public final String ACCOUNT_STATE_CHANGED = BuildConfig.APPLICATION_ID + "account.stateChanged";
    static public final String INCOMING_TEXT = BuildConfig.APPLICATION_ID + ".message.incomingTxt";
    static public final String MESSAGE_STATE_CHANGED = BuildConfig.APPLICATION_ID + ".message.stateChanged";
    static public final String NAME_LOOKUP_ENDED = BuildConfig.APPLICATION_ID + ".name.lookupEnded";
    static public final String NAME_REGISTRATION_ENDED = BuildConfig.APPLICATION_ID + ".name.registrationEnded";

    static public final String MESSAGE_STATE_CHANGED_EXTRA_ID = "id";
    static public final String MESSAGE_STATE_CHANGED_EXTRA_STATUS = "status";

    @Override
    public void update(Observable o, Object arg) {
        if (!(arg instanceof DaemonEvent)) {
            return;
        }

        DaemonEvent event = (DaemonEvent) arg;
        switch (event.getEventType()) {
            case VOLUME_CHANGED:
                break;
            case ACCOUNTS_CHANGED:
                break;
            case REGISTRATION_STATE_CHANGED:
                break;
            case STUN_STATUS_FAILURE:
                break;
            case INCOMING_ACCOUNT_MESSAGE:
                break;
            case ACCOUNT_MESSAGE_STATUS_CHANGED:
                break;
            case ERROR_ALERT:
                break;
            case GET_HARDWARE_AUDIO_FORMAT:
                break;
            case GET_APP_DATA_PATH:
                break;
            case KNOWN_DEVICES_CHANGED:
                break;
            case EXPORT_ON_RING_ENDED:
                break;
            case NAME_REGISTRATION_ENDED:
                break;
            case REGISTERED_NAME_FOUND:
                break;
            default:
                Log.i(TAG, "Unknown daemon event");
                break;
        }
    }
}
