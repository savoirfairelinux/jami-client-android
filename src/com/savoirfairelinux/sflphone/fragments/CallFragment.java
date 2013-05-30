/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package com.savoirfairelinux.sflphone.fragments;

import java.util.HashMap;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.ContactPictureLoader;
import com.savoirfairelinux.sflphone.model.Attractor;
import com.savoirfairelinux.sflphone.model.Bubble;
import com.savoirfairelinux.sflphone.model.BubbleModel;
import com.savoirfairelinux.sflphone.model.BubblesView;
import com.savoirfairelinux.sflphone.model.CallContact;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipService;

public class CallFragment extends Fragment implements Callback {

	static final String TAG = "CallFragment";

	static final float BUBBLE_SIZE = 75;
	static final float ATTRACTOR_SIZE = 40;

	private SipCall mCall;

	private TextView callStatusTxt;
	private BubblesView view;
	private BubbleModel model;

	private Callbacks mCallbacks = sDummyCallbacks;

	private HashMap<CallContact, Bubble> contacts = new HashMap<CallContact, Bubble>();

	private CallContact myself = CallContact.ContactBuilder.buildUserContact("Me");

	private Bitmap hangup_icon;
	private Bitmap call_icon;

	@Override
	public void onCreate(Bundle savedBundle) {
		super.onCreate(savedBundle);
		model = new BubbleModel(getResources().getDisplayMetrics().density);
		//metrics = getResources().getDisplayMetrics();
		//screenCenter = new PointF(metrics.widthPixels / 2, metrics.heightPixels / 3);

		Bundle b = getArguments();

		mCall = b.getParcelable("CallInfo");
		//mCall.
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onSendMessage(SipCall call, String msg) {
		}

		@Override
		public void callContact(SipCall call) {
		}

		@Override
		public void onCallAccepted(SipCall call) {
		}

		@Override
		public void onCallRejected(SipCall call) {
		}

		@Override
		public void onCallEnded(SipCall call) {
		}

		@Override
		public void onCallSuspended(SipCall call) {
		}

		@Override
		public void onCallResumed(SipCall call) {
		}

		@Override
		public void onCalltransfered(SipCall call, String to) {
		}

		@Override
		public void onRecordCall(SipCall call) {
		}

		@Override
		public ISipService getService() {
			return null;
		}
	};

	/**
	 * The Activity calling this fragment has to implement this interface
	 * 
	 */
	public interface Callbacks {

		public ISipService getService();

		public void callContact(SipCall call);

		public void onCallAccepted(SipCall call);

		public void onCallRejected(SipCall call);

		public void onCallEnded(SipCall call);

		public void onCallSuspended(SipCall call);

		public void onCallResumed(SipCall call);

		public void onCalltransfered(SipCall call, String to);

		public void onRecordCall(SipCall call);

		public void onSendMessage(SipCall call, String msg);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}

		//rootView.requestDisallowInterceptTouchEvent(true);

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
		//rootView.requestDisallowInterceptTouchEvent(false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.frag_call, container, false);

		view = (BubblesView) rootView.findViewById(R.id.main_view);
		view.setModel(model);
		view.getHolder().addCallback(this);

		callStatusTxt = (TextView) rootView.findViewById(R.id.call_status_txt);

		hangup_icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_hangup);
		call_icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_call);

		// Do nothing here, the view is not initialized yet.
		return rootView;
	}

	private void initNormalStateDisplay() {
		Log.i(TAG, "Start normal display");

		callStatusTxt.setText("0 min");

		getBubbleFor(mCall.getContacts().get(0), model.width/2, model.height/2);
		getBubbleFor(myself, model.width/2, model.height/3);

		model.clearAttractors();
		model.addAttractor(new Attractor(new PointF(model.width / 2, model.height * .8f), ATTRACTOR_SIZE, new Attractor.Callback() {
			@Override
			public boolean onBubbleSucked(Bubble b) {
				Log.w(TAG, "Bubble sucked ! ");
				mCallbacks.onCallEnded(mCall);
				bubbleRemoved(b);
				return true;
			}
		}, hangup_icon));

	}

	private void initIncomingCallDisplay() {
		Log.i(TAG, "Start incoming display");

		callStatusTxt.setText("Incomming call");

		Bubble contact_bubble = getBubbleFor(mCall.getContacts().get(0), model.width/2, model.height/2);
		contacts.put(mCall.getContacts().get(0), contact_bubble);

		model.clearAttractors();
		model.addAttractor(new Attractor(new PointF(4 * model.width / 5, model.height / 2), ATTRACTOR_SIZE, new Attractor.Callback() {
			@Override
			public boolean onBubbleSucked(Bubble b) {
				mCallbacks.onCallAccepted(mCall);
				return false;
			}
		}, call_icon));
		model.addAttractor(new Attractor(new PointF(model.width / 5, model.height / 2), ATTRACTOR_SIZE, new Attractor.Callback() {
			@Override
			public boolean onBubbleSucked(Bubble b) {
				mCallbacks.onCallRejected(mCall);
				bubbleRemoved(b);
				return true;
			}
		}, hangup_icon));
	}

	private void initOutGoingCallDisplay() {
		Log.i(TAG, "Start outgoing display");

		callStatusTxt.setText("Calling...");

		// TODO off-thread image loading
		getBubbleFor(mCall.getContacts().get(0), model.width/2, model.height/2);

		model.clearAttractors();
		model.addAttractor(new Attractor(new PointF(model.width / 2, model.height * .8f), 40, new Attractor.Callback() {
			@Override
			public boolean onBubbleSucked(Bubble b) {
				Log.w(TAG, "Bubble sucked ! ");
				mCallbacks.onCallEnded(mCall);
				bubbleRemoved(b);
				return true;
			}
		}, hangup_icon));
	}

	/**
	 * Retrieves or create a bubble for a given contact.
	 * If the bubble exists, it is moved to the new location.
	 * 
	 * @param contact The contact
	 * @param x Initial or new x position.
	 * @param y Initial or new y position.
	 * @return Bubble corresponding to the contact.
	 */
	private Bubble getBubbleFor(CallContact contact, float x, float y) {
		Bubble contact_bubble = contacts.get(contact);
		if(contact_bubble != null) {
			contact_bubble.attractor.set(x, y);
			return contact_bubble;
		}

		// TODO off-thread image loading
		if (contact.getPhoto_id() > 0) {
			Bitmap photo = ContactPictureLoader.loadContactPhoto(getActivity().getContentResolver(), mCall.getContacts().get(0).getId());
			contact_bubble = new Bubble(x, y, BUBBLE_SIZE, photo);
		} else {
			contact_bubble = new Bubble(x, y, BUBBLE_SIZE, getActivity(), R.drawable.ic_contact_picture);
		}
		contact_bubble.contact = contact;

		model.addBubble(contact_bubble);
		contacts.put(contact, contact_bubble);

		return contact_bubble;
	}

	/**
	 * Should be called when a bubble is removed from the model
	 */
	void bubbleRemoved(Bubble b) {
		if(b.contact == null) {
			return;
		}

		contacts.remove(b.contact);
	}

	public void changeCallState(String callID, String newState) {

		Log.w(TAG, "Changing call state of "+callID);
		mCall.printCallInfo();
		if(!callID.equals(mCall.getCallId()))
			return;

		mCall.setCallState(newState);
		if(mCall.isOngoing()){
			initNormalStateDisplay();
		}
	}

	public boolean draggingBubble()
	{
		return view == null ? false : view.isDraggingBubble();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
		Log.i(TAG, "Init fragment " + mCall.getCallId());

		mCall.printCallInfo();

		if (mCall.isIncoming() && mCall.isRinging()) {
			initIncomingCallDisplay();
		} else {
			if (mCall.isRinging()) {
				initOutGoingCallDisplay();
			}
			try {
				if (mCall.isOutGoing() && mCallbacks.getService().getCall(mCall.getCallId()) == null) {
					mCallbacks.getService().placeCall(mCall);
					initOutGoingCallDisplay();
				} else if(mCall.isOutGoing() && mCall.isRinging()){
					initOutGoingCallDisplay();
				}
			} catch (RemoteException e) {
				Log.e(TAG, e.toString());
			}
		}

		if(mCall.isOngoing()){
			initNormalStateDisplay();
		}

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
	}

}
