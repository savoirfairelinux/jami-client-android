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
package cx.ring.services;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cx.ring.daemon.StringMap;
import cx.ring.model.ServiceEvent;
import cx.ring.utils.Constants;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class ConfigurationManagerCallback implements Observer<ServiceEvent> {

    private static final String TAG = ConfigurationManagerCallback.class.getSimpleName();


    private final Context mContext;

    public ConfigurationManagerCallback(Context context) {
        super();
        mContext = context;
    }

    @Override
    public void update(Observable o, ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case VOLUME_CHANGED:
                volumeChanged(
                );
                break;
            case ACCOUNTS_CHANGED:
                accountsChanged();
                break;
            case REGISTRATION_STATE_CHANGED:
                registrationStateChanged(
                        event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class),
                        event.getEventInput(ServiceEvent.EventInput.STATE, String.class),
                        event.getEventInput(ServiceEvent.EventInput.DETAIL_CODE, Integer.class)
                );
                break;
            case STUN_STATUS_FAILURE:
                stunStatusFailure();
                break;
            case INCOMING_ACCOUNT_MESSAGE:
                incomingAccountMessage(
                        event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class),
                        event.getEventInput(ServiceEvent.EventInput.FROM, String.class),
                        event.getEventInput(ServiceEvent.EventInput.MESSAGES, String.class)
                );
                break;
            case ACCOUNT_MESSAGE_STATUS_CHANGED:
                accountMessageStatusChanged(
                        event.getEventInput(ServiceEvent.EventInput.MESSAGE_ID, Long.class),
                        event.getEventInput(ServiceEvent.EventInput.STATE, Integer.class)
                );
                break;
            case ERROR_ALERT:
                errorAlert(event.getEventInput(ServiceEvent.EventInput.ALERT, Integer.class));
                break;
            case KNOWN_DEVICES_CHANGED:
                knownDevicesChanged(
                        event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class),
                        event.getEventInput(ServiceEvent.EventInput.DEVICES, StringMap.class)
                );
                break;
            case EXPORT_ON_RING_ENDED:
                exportOnRingEnded(
                        event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class),
                        event.getEventInput(ServiceEvent.EventInput.CODE, Integer.class),
                        event.getEventInput(ServiceEvent.EventInput.PIN, String.class)
                );
                break;
            case NAME_REGISTRATION_ENDED:
                nameRegistrationEnded(
                        event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class),
                        event.getEventInput(ServiceEvent.EventInput.STATE, Integer.class),
                        event.getEventInput(ServiceEvent.EventInput.NAME, String.class)
                );
                break;
            case REGISTERED_NAME_FOUND:
                registeredNameFound(
                        event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class),
                        event.getEventInput(ServiceEvent.EventInput.STATE, Integer.class),
                        event.getEventInput(ServiceEvent.EventInput.ADDRESS, String.class),
                        event.getEventInput(ServiceEvent.EventInput.NAME, String.class)
                );
                break;
            default:
                Log.i(TAG, "Unknown daemon event");
                break;
        }
    }

    private void volumeChanged() {
        // nothing to be done here
    }

    private void accountsChanged() {
        Intent intent = new Intent(Constants.ACCOUNTS_CHANGED);
        mContext.sendBroadcast(intent);
    }

    private void stunStatusFailure() {
        // nothing to be done here
    }

    private void registrationStateChanged(String accountId, String state, int detailCode) {
        Intent intent = new Intent(Constants.ACCOUNT_STATE_CHANGED);
        intent.putExtra("account", accountId);
        intent.putExtra("state", state);
        intent.putExtra("code", detailCode);
        mContext.sendBroadcast(intent);
    }

    private void incomingAccountMessage(String accountId, String from, String msg) {
        Intent intent = new Intent(Constants.INCOMING_TEXT);
        intent.putExtra("txt", msg);
        intent.putExtra("from", from);
        intent.putExtra("account", accountId);
        mContext.sendBroadcast(intent);
    }

    private void accountMessageStatusChanged(long messageId, int status) {
        Intent intent = new Intent(Constants.MESSAGE_STATE_CHANGED);
        intent.putExtra(Constants.MESSAGE_STATE_CHANGED_EXTRA_ID, messageId);
        intent.putExtra(Constants.MESSAGE_STATE_CHANGED_EXTRA_STATUS, status);
        mContext.sendBroadcast(intent);
    }

    private void errorAlert(int alert) {
        Log.d(TAG, "errorAlert : " + alert);
    }

    private void knownDevicesChanged(String accountId, StringMap devices) {
        Intent intent = new Intent(Constants.ACCOUNTS_DEVICES_CHANGED);
        intent.putExtra("account", accountId);
        intent.putExtra("devices", devices.toNative());
        mContext.sendBroadcast(intent);
    }

    private void exportOnRingEnded(String accountId, int code, String pin) {
        Intent intent = new Intent(Constants.ACCOUNTS_EXPORT_ENDED);
        intent.putExtra("account", accountId);
        intent.putExtra("code", code);
        intent.putExtra("pin", pin);
        mContext.sendBroadcast(intent);
    }

    private void nameRegistrationEnded(String accountId, int state, String name) {
        Intent intent = new Intent(Constants.NAME_REGISTRATION_ENDED);
        intent.putExtra("account", accountId);
        intent.putExtra("state", state);
        intent.putExtra("name", name);
        mContext.sendBroadcast(intent);
    }

    private void registeredNameFound(String accountId, int state, String address, String name) {
        Intent intent = new Intent(Constants.NAME_LOOKUP_ENDED);
        intent.putExtra("account", accountId);
        intent.putExtra("state", state);
        intent.putExtra("name", name);
        intent.putExtra("address", address);
        mContext.sendBroadcast(intent);
    }
}
