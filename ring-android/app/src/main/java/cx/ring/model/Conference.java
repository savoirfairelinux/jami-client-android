/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.model;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.util.Random;

import cx.ring.R;
import cx.ring.client.CallActivity;
import cx.ring.service.LocalService;

public class Conference {

    public static final Uri CONTENT_URI = Uri.withAppendedPath(LocalService.AUTHORITY_URI, "conferences");

    private String id;
    private int mConfState;
    private ArrayList<SipCall> participants;
    private boolean recording;
    private ArrayList<TextMessage> messages;
    public int notificationId;
    // true if this conference is currently presented to the user.
    public boolean mVisible = false;
    public boolean resumeVideo = false;

    private final static Random rand = new Random();

    public static String DEFAULT_ID = "-1";

    public boolean isRinging() {
        return !participants.isEmpty() && participants.get(0).isRinging();
    }

    public interface state {
        int ACTIVE_ATTACHED = 0;
        int ACTIVE_DETACHED = 1;
        int ACTIVE_ATTACHED_REC = 2;
        int ACTIVE_DETACHED_REC = 3;
        int HOLD = 4;
        int HOLD_REC = 5;
    }

    public void setCallState(String callID, int newState) {
        if(id.contentEquals(callID))
            mConfState = newState;
        else {
            getCallById(callID).setCallState(newState);
        }
    }

    public int getCallState(String callID) {
        if(id.contentEquals(callID))
            return mConfState;
        else {
            return getCallById(callID).getCallState();
        }
    }

    public void setCallState(String confID, String newState) {
        if (newState.equals("ACTIVE_ATTACHED")) {
            setCallState(confID, state.ACTIVE_ATTACHED);
        } else if (newState.equals("ACTIVE_DETACHED")) {
            setCallState(confID, state.ACTIVE_DETACHED);
        } else if (newState.equals("ACTIVE_ATTACHED_REC")) {
            setCallState(confID, state.ACTIVE_ATTACHED_REC);
        } else if (newState.equals("ACTIVE_DETACHED_REC")) {
            setCallState(confID, state.ACTIVE_DETACHED_REC);
        } else if (newState.equals("HOLD")) {
            setCallState(confID, state.HOLD);
        } else if (newState.equals("HOLD_REC")) {
            setCallState(confID, state.HOLD_REC);
        }
    }

    public Conference(SipCall call) {
        this(DEFAULT_ID);
        participants.add(call);
        notificationId = rand.nextInt();
    }

    public Conference(String cID) {
        id = cID;
        participants = new ArrayList<>();
        recording = false;
        notificationId = rand.nextInt();
        messages = new ArrayList<>();
    }

    public Conference(Conference c) {
        id = c.id;
        mConfState = c.mConfState;
        participants = new ArrayList<>(c.participants);
        recording = c.recording;
        notificationId = c.notificationId;
        messages = new ArrayList<>();
    }

    public String getId() {
        if (participants.size() == 1)
            return participants.get(0).getCallId();
        else
            return id;
    }

    public String getState() {
        if (participants.size() == 1) {
            return participants.get(0).stateToString();
        }
        return getConferenceStateString();
    }

    public int getHumanState() {
        if (participants.size() == 1) {
            return participants.get(0).getCallHumanState();
        }
        return getConferenceHumanState();
    }

    public int getConferenceHumanState() {
        switch (mConfState) {
            case state.ACTIVE_ATTACHED:
                return R.string.conference_human_state_active_attached;
            case state.ACTIVE_DETACHED:
                return R.string.conference_human_state_active_detached;
            case state.ACTIVE_ATTACHED_REC:
                return R.string.conference_human_state_active_attached_rec;
            case state.ACTIVE_DETACHED_REC:
                return R.string.conference_human_state_active_detached_rec;
            case state.HOLD:
                return R.string.conference_human_state_hold;
            case state.HOLD_REC:
                return R.string.conference_human_state_hold_rec;
            default:
                return R.string.conference_human_state_default;
        }
    }

    public String getConferenceStateString() {

        String text_state;

        switch (mConfState) {
            case state.ACTIVE_ATTACHED:
                text_state = "ACTIVE_ATTACHED";
                break;
            case state.ACTIVE_DETACHED:
                text_state = "ACTIVE_DETACHED";
                break;
            case state.ACTIVE_ATTACHED_REC:
                text_state = "ACTIVE_ATTACHED_REC";
                break;
            case state.ACTIVE_DETACHED_REC:
                text_state = "ACTIVE_DETACHED_REC";
                break;
            case state.HOLD:
                text_state = "HOLD";
                break;
            case state.HOLD_REC:
                text_state = "HOLD_REC";
                break;
            default:
                text_state = "NULL";
        }

        return text_state;
    }

    public ArrayList<SipCall> getParticipants() {
        return participants;
    }

    public void addParticipant(SipCall part) {
        participants.add(part);
    }

    public void removeParticipant(SipCall toRemove) {
        participants.remove(toRemove);
    }

    public boolean hasMultipleParticipants() {
        return participants.size() > 1;
    }

    public boolean contains(String callID) {
        for (SipCall participant : participants) {
            if (participant.getCallId().contentEquals(callID))
                return true;
        }
        return false;
    }

    public SipCall getCallById(String callID) {
        for (SipCall participant : participants) {
            if (participant.getCallId().contentEquals(callID))
                return participant;
        }
        return null;
    }

    /**
     * Compare conferences based on confID/participants
     */
    @Override
    public boolean equals(Object c) {
        if (c instanceof Conference) {
            if (((Conference) c).id.contentEquals(id) && !id.contentEquals("-1")) {
                return true;
            } else {
                if (((Conference) c).id.contentEquals(id)) {
                    for (SipCall participant : participants) {
                        if (!((Conference) c).contains(participant.getCallId()))
                            return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isOnHold() {
        return participants.size() == 1 && participants.get(0).isOnHold() || getConferenceStateString().contentEquals("HOLD");
    }

    public boolean isIncoming() {
        return participants.size() == 1 && participants.get(0).isIncoming();
    }


    public void setRecording(boolean b) {
        recording = b;
    }

    public boolean isRecording() {
            return recording;
    }


    public boolean isOnGoing() {
        return participants.size() == 1 && participants.get(0).isOngoing() || participants.size() > 1;
    }

    public ArrayList<TextMessage> getMessages() {
        return messages;
    }

    public void addSipMessage(TextMessage sipMessage) {
        messages.add(sipMessage);
    }

    public Intent getViewIntent(Context ctx)
    {
        final Uri conf_uri = Uri.withAppendedPath(CONTENT_URI, getId());
        return new Intent(Intent.ACTION_VIEW).setData(conf_uri).setClass(ctx, CallActivity.class);
    }

    public void showCallNotification(Context ctx)
    {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(ctx);
        notificationManager.cancel(notificationId);

        if (getParticipants().isEmpty())
            return;
        SipCall call = getParticipants().get(0);
        CallContact contact = call.getContact();
        final Uri call_uri = Uri.withAppendedPath(SipCall.CONTENT_URI, call.getCallId());
        PendingIntent goto_intent = PendingIntent.getActivity(ctx, new Random().nextInt(),
                getViewIntent(ctx), PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder noti = new NotificationCompat.Builder(ctx);
        if (isOnGoing()) {
            noti.setContentTitle(ctx.getString(R.string.notif_current_call_title, contact.getDisplayName()))
                    .setContentText(ctx.getText(R.string.notif_current_call))
                    .setContentIntent(goto_intent)
                    .addAction(R.drawable.ic_call_end_white_24dp, ctx.getText(R.string.action_call_hangup),
                            PendingIntent.getService(ctx, new Random().nextInt(),
                                    new Intent(LocalService.ACTION_CALL_END)
                                            .setClass(ctx, LocalService.class)
                                            .setData(call_uri),
                                    PendingIntent.FLAG_ONE_SHOT));
        } else if (isRinging()) {
            if (isIncoming()) {
                noti.setContentTitle(ctx.getString(R.string.notif_incoming_call_title, contact.getDisplayName()))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setContentText(ctx.getText(R.string.notif_incoming_call))
                        .setContentIntent(goto_intent)
                        .setFullScreenIntent(goto_intent, true)
                        .addAction(R.drawable.ic_action_accept, ctx.getText(R.string.action_call_accept),
                                PendingIntent.getService(ctx, new Random().nextInt(),
                                        new Intent(LocalService.ACTION_CALL_ACCEPT)
                                                .setClass(ctx, LocalService.class)
                                                .setData(call_uri),
                                        PendingIntent.FLAG_ONE_SHOT))
                        .addAction(R.drawable.ic_call_end_white_24dp, ctx.getText(R.string.action_call_decline),
                                PendingIntent.getService(ctx, new Random().nextInt(),
                                        new Intent(LocalService.ACTION_CALL_REFUSE)
                                                .setClass(ctx, LocalService.class)
                                                .setData(call_uri),
                                        PendingIntent.FLAG_ONE_SHOT));
            } else {
                noti.setContentTitle(ctx.getString(R.string.notif_outgoing_call_title, contact.getDisplayName()))
                        .setContentText(ctx.getText(R.string.notif_outgoing_call))
                        .setContentIntent(goto_intent)
                        .addAction(R.drawable.ic_call_end_white_24dp, ctx.getText(R.string.action_call_hangup),
                                PendingIntent.getService(ctx, new Random().nextInt(),
                                        new Intent(LocalService.ACTION_CALL_END)
                                                .setClass(ctx, LocalService.class)
                                                .setData(call_uri),
                                        PendingIntent.FLAG_ONE_SHOT));
            }

        } else {
            notificationManager.cancel(notificationId);
            return;
        }

        noti.setOngoing(true).setCategory(NotificationCompat.CATEGORY_CALL).setSmallIcon(R.drawable.ic_launcher);

        if (contact.getPhoto() != null) {
            Resources res = ctx.getResources();
            int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
            int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
            noti.setLargeIcon(Bitmap.createScaledBitmap(contact.getPhoto(), width, height, false));
        }
        notificationManager.notify(notificationId, noti.build());
    }

}
