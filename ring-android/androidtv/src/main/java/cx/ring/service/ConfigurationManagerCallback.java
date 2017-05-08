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
package cx.ring.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cx.ring.BuildConfig;
import cx.ring.daemon.IntVect;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.StringVect;
import cx.ring.model.ServiceEvent;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class ConfigurationManagerCallback implements Observer<ServiceEvent> {

    private static final String TAG = ConfigurationManagerCallback.class.getSimpleName();

    static final String ACCOUNTS_CHANGED = BuildConfig.APPLICATION_ID + "accounts.changed";
    static final String ACCOUNTS_DEVICES_CHANGED = BuildConfig.APPLICATION_ID + "accounts.devicesChanged";
    static final String ACCOUNTS_EXPORT_ENDED = BuildConfig.APPLICATION_ID + "accounts.exportEnded";
    static final String ACCOUNT_STATE_CHANGED = BuildConfig.APPLICATION_ID + "account.stateChanged";
    static final String INCOMING_TEXT = BuildConfig.APPLICATION_ID + ".message.incomingTxt";
    static final String MESSAGE_STATE_CHANGED = BuildConfig.APPLICATION_ID + ".message.stateChanged";
    static final String NAME_LOOKUP_ENDED = BuildConfig.APPLICATION_ID + ".name.lookupEnded";
    static final String NAME_REGISTRATION_ENDED = BuildConfig.APPLICATION_ID + ".name.registrationEnded";

    static final String MESSAGE_STATE_CHANGED_EXTRA_ID = "id";
    static final String MESSAGE_STATE_CHANGED_EXTRA_STATUS = "status";

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
                        event.getEventInput(ServiceEvent.EventInput.DEVICE, String.class),
                        event.getEventInput(ServiceEvent.EventInput.VALUE, Integer.class)
                );
                break;
            case ACCOUNTS_CHANGED:
                accountsChanged();
                break;
            case REGISTRATION_STATE_CHANGED:
                registrationStateChanged(
                        event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class),
                        event.getEventInput(ServiceEvent.EventInput.STATE, String.class),
                        event.getEventInput(ServiceEvent.EventInput.DETAIL_CODE, Integer.class),
                        event.getEventInput(ServiceEvent.EventInput.DETAIL_STRING, String.class)
                );
                break;
            case STUN_STATUS_FAILURE:
                stunStatusFailure(event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class));
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
                        event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class),
                        event.getEventInput(ServiceEvent.EventInput.MESSAGE_ID, Long.class),
                        event.getEventInput(ServiceEvent.EventInput.TO, String.class),
                        event.getEventInput(ServiceEvent.EventInput.STATE, Integer.class)
                );
                break;
            case ERROR_ALERT:
                errorAlert(event.getEventInput(ServiceEvent.EventInput.ALERT, Integer.class));
                break;
            case GET_HARDWARE_AUDIO_FORMAT:
                getHardwareAudioFormat(event.getEventInput(ServiceEvent.EventInput.FORMATS, IntVect.class));
                break;
            case GET_APP_DATA_PATH:
                getAppDataPath(
                        event.getEventInput(ServiceEvent.EventInput.NAME, String.class),
                        event.getEventInput(ServiceEvent.EventInput.PATHS, StringVect.class)
                );
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

    private void volumeChanged(String device, int value) {
        // nothing to be done here
    }

    private void accountsChanged() {
        Intent intent = new Intent(ACCOUNTS_CHANGED);
        mContext.sendBroadcast(intent);
    }

    private void stunStatusFailure(String accountId) {
        // nothing to be done here
    }

    private void registrationStateChanged(String accountId, String state, int detailCode, String detailString) {
        Intent intent = new Intent(ACCOUNT_STATE_CHANGED);
        intent.putExtra("account", accountId);
        intent.putExtra("state", state);
        intent.putExtra("code", detailCode);
        mContext.sendBroadcast(intent);
    }

    private void incomingAccountMessage(String accountId, String from, String msg) {
        Intent intent = new Intent(INCOMING_TEXT);
        intent.putExtra("txt", msg);
        intent.putExtra("from", from);
        intent.putExtra("account", accountId);
        mContext.sendBroadcast(intent);
    }

    private void accountMessageStatusChanged(String id, long messageId, String to, int status) {
        Intent intent = new Intent(MESSAGE_STATE_CHANGED);
        intent.putExtra(MESSAGE_STATE_CHANGED_EXTRA_ID, messageId);
        intent.putExtra(MESSAGE_STATE_CHANGED_EXTRA_STATUS, status);
        mContext.sendBroadcast(intent);
    }

    private void errorAlert(int alert) {
        Log.d(TAG, "errorAlert : " + alert);
    }

    private void getHardwareAudioFormat(IntVect ret) {
        OpenSlParams audioParams = OpenSlParams.createInstance(mContext);
        ret.add(audioParams.getSampleRate());
        ret.add(audioParams.getBufferSize());
        Log.d(TAG, "getHardwareAudioFormat: " + audioParams.getSampleRate() + " " + audioParams.getBufferSize());
    }

    private void getAppDataPath(String name, StringVect ret) {
        if (name == null || ret == null) {
            return;
        }

        switch (name) {
            case "files":
                ret.add(mContext.getFilesDir().getAbsolutePath());
                break;
            case "cache":
                ret.add(mContext.getCacheDir().getAbsolutePath());
                break;
            default:
                ret.add(mContext.getDir(name, Context.MODE_PRIVATE).getAbsolutePath());
                break;
        }
    }

    private void knownDevicesChanged(String accountId, StringMap devices) {
        Intent intent = new Intent(ACCOUNTS_DEVICES_CHANGED);
        intent.putExtra("account", accountId);
        intent.putExtra("devices", devices.toNative());
        mContext.sendBroadcast(intent);
    }

    private void exportOnRingEnded(String accountId, int code, String pin) {
        Intent intent = new Intent(ACCOUNTS_EXPORT_ENDED);
        intent.putExtra("account", accountId);
        intent.putExtra("code", code);
        intent.putExtra("pin", pin);
        mContext.sendBroadcast(intent);
    }

    private void nameRegistrationEnded(String accountId, int state, String name) {
        Intent intent = new Intent(NAME_REGISTRATION_ENDED);
        intent.putExtra("account", accountId);
        intent.putExtra("state", state);
        intent.putExtra("name", name);
        mContext.sendBroadcast(intent);
    }

    private void registeredNameFound(String accountId, int state, String address, String name) {
        Intent intent = new Intent(NAME_LOOKUP_ENDED);
        intent.putExtra("account", accountId);
        intent.putExtra("state", state);
        intent.putExtra("name", name);
        intent.putExtra("address", address);
        mContext.sendBroadcast(intent);
    }
}
