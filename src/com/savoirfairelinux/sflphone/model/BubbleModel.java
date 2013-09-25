package com.savoirfairelinux.sflphone.model;

import java.util.ArrayList;
import java.util.List;

import android.graphics.PointF;
import android.util.Log;

public class BubbleModel
{
	private static final String TAG = BubbleModel.class.getSimpleName();

	private long lastUpdate = 0;
	public int width, height;
	private ArrayList<Bubble> bubbles = new ArrayList<Bubble>();
	private ArrayList<Attractor> attractors = new ArrayList<Attractor>();

	private static final double BUBBLE_RETURN_TIME_HALF_LIFE = .3;
	private static final double BUBBLE_RETURN_TIME_LAMBDA = Math.log(2)/BUBBLE_RETURN_TIME_HALF_LIFE;

	private static final double FRICTION_VISCOUS = Math.log(2)/.2f;		// Viscous friction factor

	private static final float BUBBLE_MAX_SPEED = 2500.f;	// px.s-1 : Max target speed in px/sec
	private static final float ATTRACTOR_SMOOTH_DIST = 50.f; // px : Size of the "gravity hole" around the attractor
	private static final float ATTRACTOR_STALL_DIST = 15.f; // px : Size of the "gravity hole" flat bottom
	private static final float ATTRACTOR_DIST_SUCK = 20.f; // px

	private static final float BORDER_REPULSION = 60000; // px.s^-2

	private final float border_repulsion;
	private final float bubble_max_speed;
	private final float attractor_smooth_dist;
	private final float attractor_stall_dist;
	private final float attractor_dist_suck;

	private float density = 1.f;

	public BubbleModel(float screen_density) {
	    Log.d(TAG, "Creating BubbleModel");
		this.density = screen_density;
		attractor_dist_suck = ATTRACTOR_DIST_SUCK*density;
		bubble_max_speed = BUBBLE_MAX_SPEED*density;
		attractor_smooth_dist = ATTRACTOR_SMOOTH_DIST*density;
		attractor_stall_dist = ATTRACTOR_STALL_DIST*density;
		border_repulsion = BORDER_REPULSION*density;
	}

	public void addBubble(Bubble b) {
		b.setDensity(density);
		bubbles.add(b);
	}

	public List<Bubble> getBubbles()
	{
		return bubbles;
	}

	public void addAttractor(Attractor a) {
		a.setDensity(density);
		attractors.add(a);
	}

	public List<Attractor> getAttractors()
	{
		return attractors;
	}

	public void clearAttractors() {
		attractors.clear();
	}

	public void clear() {
		clearAttractors();
		bubbles.clear();
	}

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

		int attr_n = attractors.size();

		// Iterators should not be used in frequently called methods
		// to avoid garbage collection glitches caused by iterator objects.
		for(int i=0, n=bubbles.size(); i<n; i++) {
			Bubble b = bubbles.get(i);
			
            if (b.markedToDie){
			    continue;
			}

			if(!b.dragged) {
			    
				float bx=b.getPosX(), by=b.getPosY();

				Attractor attractor = null;
				PointF attractor_pos = b.attractor;
				float attractor_dist = (attractor_pos.x-bx)*(attractor_pos.x-bx) + (attractor_pos.y-by)*(attractor_pos.x-by);

				for(int j=0; j<attr_n; j++) {
				    try{
					Attractor t = attractors.get(j);
				    
					float dx = t.pos.x-bx, dy = t.pos.y-by;
					float adist = dx*dx + dy*dy;
					if(adist < attractor_dist) {
						attractor = t;
						attractor_pos = t.pos;
						attractor_dist = adist;
					}
				    } catch (IndexOutOfBoundsException e){
                        // Try to update when layout was changing
                    }
				}

				//float friction_coef = 1.f-FRICTION_VISCOUS*dt;
				double friction_coef = 1+Math.expm1(-FRICTION_VISCOUS*ddt);
				b.speed.x *= friction_coef;
				b.speed.y *= friction_coef;

				//if(attractor != null) {
				float target_speed;
				float tdx = attractor_pos.x - bx, tdy = attractor_pos.y - by;
				float dist = Math.max(1.f, (float) Math.sqrt(tdx*tdx + tdy*tdy));
				if(dist > attractor_smooth_dist)
					target_speed = bubble_max_speed;
				else if(dist < attractor_stall_dist)
					target_speed = 0;
				else {
					float a = (dist-attractor_stall_dist)/(attractor_smooth_dist-attractor_stall_dist);
					target_speed = bubble_max_speed*a;
				}
				if(attractor != null) {
					if(dist > attractor_smooth_dist)
						b.target_scale = 1.f;
					else if(dist < attractor_stall_dist)
						b.target_scale = .2f;
					else {
						float a = (dist-attractor_stall_dist)/(attractor_smooth_dist-attractor_stall_dist);
						b.target_scale = a*.8f+.2f;
					}
				}

				// border repulsion
				if(bx < 0 && b.speed.x < 0) {
					b.speed.x += dt * border_repulsion;
				} else if(bx > width && b.speed.x > 0) {
					b.speed.x -= dt * border_repulsion;
				}
				if(by < 0 && b.speed.y < 0) {
					b.speed.y += dt * border_repulsion;
				} else if(by > height && b.speed.y > 0) {
					b.speed.y -= dt * border_repulsion;
				}

				b.speed.x += dt * target_speed * tdx/dist;
				b.speed.y += dt * target_speed * tdy/dist;

				double edt = -Math.expm1(-BUBBLE_RETURN_TIME_LAMBDA*ddt);
				double dx = (attractor_pos.x - bx) * edt + Math.min(bubble_max_speed, b.speed.x) * dt;
				double dy = (attractor_pos.y - by) * edt + Math.min(bubble_max_speed, b.speed.y) * dt;
				//	Log.w(TAG, "update dx="+dt+" dy="+dy);
				b.setPos((float)(bx+dx), (float)(by+dy));

//				Log.i(TAG,"Model:");
				if(attractor != null && attractor_dist < attractor_dist_suck*attractor_dist_suck) {
					b.dragged = false;
					if(attractor.callback.onBubbleSucked(b)) {
						bubbles.remove(b);
						n--;
					} else {
						b.target_scale = 1.f;
					}
				}
			}

			b.setScale(b.getScale() + (b.target_scale-b.getScale())*dt*10.f);

		}
	}

    public Bubble getBubble(SipCall call) {
        for(Bubble b : bubbles){
            if(b.associated_call.getCallId().contentEquals(call.getCallId()))
                return b;
        }
        return null;
    }

    public void removeBubble(SipCall sipCall) {
        bubbles.remove(getBubble(sipCall));
        
    }


}
