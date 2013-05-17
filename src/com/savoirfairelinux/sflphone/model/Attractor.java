package com.savoirfairelinux.sflphone.model;

import android.graphics.PointF;

public class Attractor {

	public interface Callback {
		public void onBubbleSucked(Bubble b);
	}

	PointF pos;
	Callback callback;
	public Attractor(PointF pos, Callback callback) {
		this.pos = pos;
		this.callback = callback;
	}
}