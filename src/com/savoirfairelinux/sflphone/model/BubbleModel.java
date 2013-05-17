package com.savoirfairelinux.sflphone.model;

import java.util.ArrayList;

import android.util.Log;

public class BubbleModel
{
	private static final String TAG = BubbleModel.class.getSimpleName();

	public long lastUpdate = 0;
	public int width, height;
	public ArrayList<Bubble> listBubbles = new ArrayList<Bubble>();

	/*
	private static final double BUBBLE_RETURN_TIME_HALF_LIFE = .25;
	private static final double BUBBLE_RETURN_TIME_LAMBDA = Math.log(2)/BUBBLE_RETURN_TIME_HALF_LIFE;
	 */
	private static final float FRICTION_VISCOUS = .5f;		// Viscous friction factor

	private static final float BUBBLE_MAX_SPEED = 2500.f;	// Max target speed in px/sec
	private static final float ATTRACTOR_SMOOTH_DIST = 100.f;// Size of the "gravity hole" around the attractor
	private static final float ATTRACTOR_STALL_DIST = 15.f; // Size of the "gravity hole" flat bottom
	private static final float ATTRACTOR_ACCEL = 10.f;		// Acceleration factor towards target speed

	public void update()
	{
		long now = System.nanoTime();

		// Do nothing if lastUpdate is in the future.
		if (lastUpdate > now)
			return;

		double ddt = Math.min((now - lastUpdate) / 1000000000.0, .2);
		lastUpdate = now;

		float dt = (float)ddt;
		//Log.w(TAG, "update dt="+dt);

		// Iterators should not be used in frequently called methods
		// to avoid garbage collection glitches caused by iterator objects.
		for(int i=0, n=listBubbles.size(); i<n; i++) {
			Bubble b = listBubbles.get(i);
			//Log.w(TAG, "update b");
			if(!b.dragged && b.attractor != null) {
				float bx=b.getPosX(), by=b.getPosY();

				//
				/// Apply viscous friction
				float friction_coef = 1.f-FRICTION_VISCOUS*dt;
				/*	b.speed.x *= friction_coef;
				b.speed.y *= friction_coef;*/

				float tdx = b.attractor.x - bx, tdy = b.attractor.y - by;
				float dist = (float) Math.sqrt(tdx*tdx + tdy*tdy);
				float speed = (float)Math.sqrt(b.speed.x*b.speed.x + b.speed.y*b.speed.y);

				if(dist < ATTRACTOR_STALL_DIST) {
					b.speed.x *= friction_coef;
					b.speed.y *= friction_coef;

				} else


					if(speed > 10.f || dist > ATTRACTOR_STALL_DIST) {
						dist = Math.max(1.f, dist); // Avoid division by 0

						b.speed.x *= friction_coef;
						b.speed.y *= friction_coef;


						// Target speed (defines the "gravity hole")
						/*	float target_speed = dist > ATTRACTOR_SMOOTH_DIST ? BUBBLE_MAX_SPEED
							: dist < ATTRACTOR_STALL_DIST ? 0 : BUBBLE_MAX_SPEED/ATTRACTOR_SMOOTH_DIST*(dist-ATTRACTOR_STALL_DIST);*/
						float target_speed;
						if(dist > ATTRACTOR_SMOOTH_DIST)
							target_speed = BUBBLE_MAX_SPEED;
						else if(dist < ATTRACTOR_STALL_DIST)
							target_speed = 0;
						else
							target_speed = BUBBLE_MAX_SPEED/(ATTRACTOR_SMOOTH_DIST-ATTRACTOR_STALL_DIST)*(dist-ATTRACTOR_STALL_DIST);

						float target_speed_x = target_speed*tdx/dist;
						float target_speed_y = target_speed*tdy/dist;

						// Acceleration
						float ax = (target_speed_x-b.speed.x) * ATTRACTOR_ACCEL;// + 2*(b.last_speed.x-b.speed.x)*(1-FRICTION_VISCOUS)/FRICTION_VISCOUS*60.f;
						float ay = (target_speed_y-b.speed.y) * ATTRACTOR_ACCEL;// + 2*(b.last_speed.y-b.speed.y)*(1-FRICTION_VISCOUS)/FRICTION_VISCOUS*60.f;

						// Speed update

						b.speed.x += ax*dt;
						b.speed.y += ay*dt;
						b.last_speed.set(b.speed);
						Log.w(TAG, "dist " + dist + " speed " + Math.sqrt(b.speed.x*b.speed.x + b.speed.y*b.speed.y) + " target speed "+target_speed);

						// Position update
						float dx = b.speed.x * dt;
						float dy = b.speed.y * dt;
						b.setPos(bx+dx, by+dy);
					}

				// Prevent speed higher than BUBBLE_MAX_SPEED
				/*
				float ds = (target_speed-speed)*dt;

				// Set motion direction and speed
				float nsr = (speed>BUBBLE_MAX_SPEED ? BUBBLE_MAX_SPEED : speed+ds)/(dist < 1.f ? 1.f : dist);
				b.speed.x = tdx * nsr;
				b.speed.y = tdy * nsr;*/

				/*double bx=b.getPosX(), by=b.getPosY();
				double edt = -Math.expm1(-BUBBLE_RETURN_TIME_LAMBDA*dt);
				double dx = (b.attractor.x - bx) * edt;
				double dy = (b.attractor.y - by) * edt;
				//Log.w(TAG, "update dx="+dt+" dy="+dy);
				b.setPos((float)(bx+dx), (float)(by+dy));*/
			}
		}
	}
}
