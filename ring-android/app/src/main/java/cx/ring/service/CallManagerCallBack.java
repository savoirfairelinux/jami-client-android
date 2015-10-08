package cx.ring.service;

import android.support.v4.app.NotificationCompat;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import cx.ring.R;
import cx.ring.client.CallActivity;
import cx.ring.history.HistoryText;
import cx.ring.model.CallContact;
import cx.ring.model.TextMessage;
import cx.ring.model.account.Account;
import cx.ring.model.account.AccountDetailSrtp;
import cx.ring.utils.SwigNativeConverter;

import java.util.ArrayList;
import java.util.Map;

import cx.ring.model.Conference;
import cx.ring.model.SecureSipCall;
import cx.ring.model.SipCall;

public class CallManagerCallBack extends Callback {

    private static final String TAG = "CallManagerCallBack";
    private SipService mService;

    static public final String CALL_STATE_CHANGED = "call-State-changed";
    static public final String INCOMING_CALL = "incoming-call";
    static public final String INCOMING_TEXT = "incoming-text";
    static public final String CONF_CREATED = "conf_created";
    static public final String CONF_REMOVED = "conf_removed";
    static public final String CONF_CHANGED = "conf_changed";
    static public final String RECORD_STATE_CHANGED = "record_state";

    static public final String ZRTP_ON = "secure_zrtp_on";
    static public final String ZRTP_OFF = "secure_zrtp_off";
    static public final String DISPLAY_SAS = "display_sas";
    static public final String ZRTP_NEGOTIATION_FAILED = "zrtp_nego_failed";
    static public final String ZRTP_NOT_SUPPORTED = "zrtp_not_supported";

    static public final String RTCP_REPORT_RECEIVED = "on_rtcp_report_received";


    public CallManagerCallBack(SipService context) {
        super();
        mService = context;
    }

    @Override
    public void callStateChanged(String callID, String newState, int detail_code) {
        Log.w(TAG, "on_call_state_changed : (" + callID + ", " + newState + ")");

        Conference toUpdate = mService.findConference(callID);

        if (toUpdate == null) {
            Log.w(TAG, "callStateChanged: can't find call " + callID);
            return;
        }

        Intent intent = new Intent(CALL_STATE_CHANGED);
        intent.putExtra("CallID", callID);
        intent.putExtra("State", newState);
        intent.putExtra("DetailCode", detail_code);

        if (toUpdate.isRinging() && !newState.equals("RINGING")) {
            Log.w(TAG, "Setting call start date " + callID);
            toUpdate.getCallById(callID).setTimestampStart(System.currentTimeMillis());
        }

        switch (newState) {
            case "CONNECTING":
                toUpdate.setCallState(callID, SipCall.State.CONNECTING); break;
            case "RINGING":
                toUpdate.setCallState(callID, SipCall.State.RINGING); break;
            case "CURRENT":
                toUpdate.setCallState(callID, SipCall.State.CURRENT); break;
            case "HOLD":
                toUpdate.setCallState(callID, SipCall.State.HOLD); break;
            case "UNHOLD":
                toUpdate.setCallState(callID, SipCall.State.CURRENT); break;
            case "HUNGUP":
            case "INACTIVE":
                Log.d(TAG, "Hanging up " + callID);
                Log.w("CallNotification ", "Canceling " + toUpdate.notificationId);
                mService.mNotificationManager.notificationManager.cancel(toUpdate.notificationId);
                SipCall call = toUpdate.getCallById(callID);
                if (!toUpdate.hasMultipleParticipants()) {
                    if (toUpdate.isRinging() && toUpdate.isIncoming()) {
                        mService.mNotificationManager.publishMissedCallNotification(mService.getConferences().get(callID));
                    }
                    toUpdate.setCallState(callID, SipCall.State.HUNGUP);
                    mService.mHistoryManager.insertNewEntry(toUpdate);
                    mService.getConferences().remove(toUpdate.getId());
                } else {
                    toUpdate.setCallState(callID, SipCall.State.HUNGUP);
                    mService.mHistoryManager.insertNewEntry(call);
                }
                break;
            case "BUSY":
                mService.mNotificationManager.notificationManager.cancel(toUpdate.notificationId);
                toUpdate.setCallState(callID, SipCall.State.BUSY);
                mService.getConferences().remove(toUpdate.getId());
                break;
            case "FAILURE":
                Log.w("CallNotification ", "Canceling " + toUpdate.notificationId);
                mService.mNotificationManager.notificationManager.cancel(toUpdate.notificationId);
                toUpdate.setCallState(callID, SipCall.State.FAILURE);
                mService.getConferences().remove(toUpdate.getId());
                Ringservice.hangUp(callID);
                break;
        }
        intent.putExtra("conference", toUpdate);
        mService.sendBroadcast(intent);
    }


    @Override
    public void incomingCall(String accountID, String callID, String from) {
        Log.w(TAG, "on_incoming_call(" + accountID + ", " + callID + ", " + from + ")");

        try {
            StringMap details = Ringservice.getAccountDetails(accountID);
            //VectMap credentials = Ringservice.getCredentials(accountID);
            //StringMap state = Ringservice.getVolatileAccountDetails(accountID);
            Account acc = new Account(accountID, details.toNative(), null, null);

            Intent toSend = new Intent(CallManagerCallBack.INCOMING_CALL);
            toSend.setClass(mService, CallActivity.class);
            toSend.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            CallContact unknown = CallContact.ContactBuilder.buildUnknownContact(from);

            SipCall newCall = new SipCall(callID, accountID, from, SipCall.Direction.INCOMING);
            newCall.setContact(unknown);
            newCall.setCallState(SipCall.State.RINGING);
            newCall.setTimestampStart(System.currentTimeMillis());

            Conference toAdd;
            if (acc.useSecureLayer()) {
               SecureSipCall secureCall = new SecureSipCall(newCall, acc.getSrtpDetails().getDetailString(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE));
                toAdd = new Conference(secureCall);
            } else {
                toAdd = new Conference(newCall);
            }

            mService.getConferences().put(toAdd.getId(), toAdd);

            NotificationCompat.Builder noti = new NotificationCompat.Builder(mService)
                    .setContentTitle("Incoming call with " + from)
                    .setContentText("incoming call")
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .addAction(R.drawable.ic_call_end_white_24dp, "End call",
                            PendingIntent.getService(mService, 4278,
                                new Intent(mService, SipService.class)
                                        .setAction(SipService.ACTION_CALL_END)
                                        .putExtra("conf", toAdd.getId()),
                                    PendingIntent.FLAG_ONE_SHOT));

            //mService.startForeground(toAdd.notificationId, noti);
            Log.w("CallNotification ", "Adding for incoming " + toAdd.notificationId);
            mService.mNotificationManager.notificationManager.notify(toAdd.notificationId, noti.build());

            Bundle bundle = new Bundle();
            bundle.putParcelable("conference", toAdd);
            toSend.putExtra("resuming", false);
            toSend.putExtras(bundle);
            mService.startActivity(toSend);
            mService.mMediaManager.startRing("");
            mService.mMediaManager.obtainAudioFocus(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void conferenceCreated(final String confID) {
        Log.w(TAG, "CONFERENCE CREATED:" + confID);
        Intent intent = new Intent(CONF_CREATED);
        Conference created = new Conference(confID);

        StringVect all_participants = Ringservice.getParticipantList(confID);
        Log.w(TAG, "all_participants:" + all_participants.size());
        for (int i = 0; i < all_participants.size(); ++i) {
            if (mService.getConferences().get(all_participants.get(i)) != null) {
                created.addParticipant(mService.getConferences().get(all_participants.get(i)).getCallById(all_participants.get(i)));
                mService.getConferences().remove(all_participants.get(i));
            } else {
                for (Map.Entry<String, Conference> stringConferenceEntry : mService.getConferences().entrySet()) {
                    Conference tmp = stringConferenceEntry.getValue();
                    for (SipCall c : tmp.getParticipants()) {
                        if (c.getCallId().contentEquals(all_participants.get(i))) {
                            created.addParticipant(c);
                            mService.getConferences().get(tmp.getId()).removeParticipant(c);
                        }
                    }
                }
            }
        }
        intent.putExtra("conference", created);
        intent.putExtra("confID", created.getId());
        mService.getConferences().put(created.getId(), created);
        mService.sendBroadcast(intent);
    }

    @Override
    public void incomingMessage(String id, String from, StringMap messages) {
        Log.w(TAG, "on_incoming_message:" + messages);

        String msg = messages.get("text/plain");
        if (msg == null)
            return;

        Conference conf = mService.getConferences().get(id);
        if (conf == null) {
            for (Conference tmp : mService.getConferences().values())
                if (tmp.getCallById(id) != null) {
                    conf = tmp;
                    break;
                }
            if (conf == null) {
                Log.w(TAG, "Discarding message for unknown call " + id);
                return;
            }
        }

        TextMessage message = new TextMessage(true, msg, from, id, conf.hasMultipleParticipants() ? null : conf.getParticipants().get(0).getAccount());
        if (!conf.hasMultipleParticipants())
            message.setContact(conf.getParticipants().get(0).getContact());

        conf.addSipMessage(message);

        mService.mHistoryManager.insertNewTextMessage(new HistoryText(message));

        Intent intent = new Intent(INCOMING_TEXT);
        intent.putExtra("txt", message);
        intent.putExtra("conference", conf.getId());
        mService.sendBroadcast(intent);
    }

    @Override
    public void conferenceRemoved(String confID) {
        Log.i(TAG, "on_conference_removed:");
        Intent intent = new Intent(CONF_REMOVED);
        intent.putExtra("confID", confID);

        Conference toReInsert = mService.getConferences().get(confID);
        for (SipCall call : toReInsert.getParticipants()) {
            mService.getConferences().put(call.getCallId(), new Conference(call));
        }

        Conference conf = mService.getConferences().get(confID);

        Log.w("CallNotification ", "Canceling " + conf.notificationId);
        //NotificationManager mNotifyMgr = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        mService.mNotificationManager.notificationManager.cancel(conf.notificationId);

        intent.putExtra("conference", conf);
        mService.getConferences().remove(confID);
        mService.sendBroadcast(intent);

        if (mService.getConferences().size() == 0) {
            mService.stopForeground(true);
        }

    }

    @Override
    public void conferenceChanged(String confID, String state) {
        Log.i(TAG, "on_conference_state_changed:");
        Intent intent = new Intent(CONF_CHANGED);
        intent.putExtra("confID", confID);
        intent.putExtra("State", state);


        Log.i(TAG, "Received:" + intent.getAction());
        Log.i(TAG, "State:" + state);

        Conference toModify = mService.getConferences().get(confID);
        toModify.setCallState(confID, state);

        ArrayList<String> newParticipants = SwigNativeConverter.convertSwigToNative(Ringservice.getParticipantList(intent.getStringExtra("confID")));

        if (toModify.getParticipants().size() < newParticipants.size()) {
            // We need to add the new participant to the conf
            for (String newParticipant : newParticipants) {
                if (toModify.getCallById(newParticipant) == null) {
                    mService.addCallToConference(toModify.getId(), newParticipant);
                }
            }
        } else if (toModify.getParticipants().size() > newParticipants.size()) {
            Log.i(TAG, "toModify.getParticipants().size() > newParticipants.size()");
            for (SipCall participant : toModify.getParticipants()) {
                if (!newParticipants.contains(participant.getCallId())) {
                    mService.detachCallFromConference(toModify.getId(), participant);
                    break;
                }
            }
        }

        mService.sendBroadcast(intent);
    }

    @Override
    public void recordPlaybackFilepath(String id, String filename) {
        Intent intent = new Intent();
        intent.putExtra("callID", id);
        intent.putExtra("file", filename);
        mService.sendBroadcast(intent);
    }

    @Override
    public void secureSdesOn(String callID) {
        Log.i(TAG, "on_secure_sdes_on");
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        call.setInitialized();
        call.useSecureSDES(true);
    }

    @Override
    public void secureSdesOff(String callID) {
        Log.i(TAG, "on_secure_sdes_off");
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        call.setInitialized();
        call.useSecureSDES(false);
    }

    @Override
    public void secureZrtpOn(String callID, String cipher) {
        Log.i(TAG, "on_secure_zrtp_on");
        Intent intent = new Intent(ZRTP_ON);
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        call.setInitialized();
        call.setZrtpSupport(true);
        intent.putExtra("callID", callID);
        intent.putExtra("conference", mService.findConference(callID));
        mService.sendBroadcast(intent);
    }

    @Override
    public void secureZrtpOff(String callID) {
        Log.i(TAG, "on_secure_zrtp_off");
        Intent intent = new Intent(ZRTP_OFF);
        intent.putExtra("callID", callID);
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        // Security can be off because call was hung up
        if (call == null)
            return;

        call.setInitialized();
        call.setZrtpSupport(false);
        intent.putExtra("conference", mService.findConference(callID));
        mService.sendBroadcast(intent);
    }

    @Override
    public void showSAS(String callID, String sas, int verified) {
        Log.i(TAG, "on_show_sas:" + sas);
        Intent intent = new Intent(DISPLAY_SAS);
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        call.setSAS(sas);
        call.sasConfirmedByZrtpLayer(verified);

        intent.putExtra("callID", callID);
        intent.putExtra("SAS", sas);
        intent.putExtra("verified", verified);
        intent.putExtra("conference", mService.findConference(callID));
        mService.sendBroadcast(intent);
    }

    @Override
    public void zrtpNotSuppOther(String callID) {
        Log.i(TAG, "on_zrtp_not_supported");
        Intent intent = new Intent(ZRTP_NOT_SUPPORTED);
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        call.setInitialized();
        call.setZrtpSupport(false);
        intent.putExtra("callID", callID);
        intent.putExtra("conference", mService.findConference(callID));
        mService.sendBroadcast(intent);
    }

    @Override
    public void zrtpNegotiationFailed(String callID, String reason, String severity) {
        Log.i(TAG, "on_zrtp_negociation_failed");
        Intent intent = new Intent(ZRTP_NEGOTIATION_FAILED);
        SecureSipCall call = (SecureSipCall) mService.getCallById(callID);
        call.setInitialized();
        call.setZrtpSupport(false);
        intent.putExtra("callID", callID);
        intent.putExtra("conference", mService.findConference(callID));
        mService.sendBroadcast(intent);
    }

    @Override
    public void onRtcpReportReceived(String callID, IntegerMap stats) {
        Log.i(TAG, "on_rtcp_report_received");
        Intent intent = new Intent(RTCP_REPORT_RECEIVED);
        mService.sendBroadcast(intent);
    }

}
