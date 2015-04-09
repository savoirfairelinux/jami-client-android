/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Adrien Beraud <adrien.beraud@savoirfairelinux.com>
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

package cx.ring.model;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BubbleModel {
    private static final String TAG = BubbleModel.class.getSimpleName();

    public enum State {
        None, Incoming, Outgoing, Incall
    }

    public interface ModelCallback {
        public void bubbleGrabbed(Bubble b);

        /**
         * A bubble is put beyond view borders.
         * @param b The bubble
         * @return true if the bubble should be ejected, false if it should bounce.
         */
        public boolean bubbleEjected(Bubble b);
    }

    public interface ActionGroupCallback {

        /**
         * Called when a bubble is on the "active" zone of the attractor.
         *
         * @param b The bubble that is on the attractor.
         * @return true if the bubble should be removed from the model, false otherwise.
         */
        public boolean onBubbleAction(Bubble b, int action);
    }

    static public class ActionGroup {
        public boolean enabled = true;
        private final ArrayList<Attractor> buttons = new ArrayList<Attractor>();
        private final ActionGroupCallback callback;
        final private float margin;
        public Bubble bubble = null;
        public long viewStart = 0;
        private final float appearTime;
        private final float disappearTime;

        public ActionGroup(ActionGroupCallback cb, float btn_margin, float appear_time, float disapear_time) {
            this.callback = cb;
            this.margin = btn_margin;
            appearTime = appear_time;
            disappearTime = disapear_time;
        }

        public ArrayList<Attractor> getActions() {
            return buttons;
        }

        public void addAction(final int id, Bitmap btn, String name, float size) {
            final Attractor a = new Attractor(name, size, new Attractor.Callback() {
                @Override
                public boolean onBubbleSucked(Bubble b) {
                    if (!enabled) return false;
                    return callback.onBubbleAction(b, id);
                }
            }, btn);
            buttons.add(a);
        }

        public void show(Bubble b) {
            this.bubble = b;
            long now = System.nanoTime();
            double dt = (now - viewStart) / 1000000000.;
            double r = 1. - Math.min(dt / disappearTime, 1.);
            this.enabled = true;
            this.viewStart = now - (long)(r * appearTime * 1000000000.);
        }

        public void hide() {
            long now = System.nanoTime();
            double dt = (now - viewStart) / 1000000000.;
            double r = 1. - Math.min(dt / appearTime, 1.);
            this.enabled = false;
            viewStart = now - (long)(r * disappearTime * 1000000000.);
        }

        public float getVisibility(long now) {
            double dt = (now - viewStart) / 1000000000.;
            if (enabled)
                return (float) Math.min(dt / appearTime, 1.);
            else
                return 1.f - (float) Math.min(dt / disappearTime, 1.);
        }

        public void order(int w, int h) {
            int n = buttons.size();
            if (n == 0) return;
            //float y = h - 3 * buttons.get(0).radius;
            float y = bubble.getPosY() - margin - bubble.radius;
            final float WIDTH = 2 * buttons.get(0).radius;
            float totw = n * WIDTH + (n-1) * margin;
            float xs = (w - totw) / 2 + buttons.get(0).radius;
            float xstep = WIDTH+margin;
            for (int i=0; i<n; i++) {
                buttons.get(i).setPos(xs + i*xstep, y);
            }
        }
    }

    private final ModelCallback callback;

    private long lastUpdate = 0;
    private int width, height;
    private final ArrayList<Bubble> bubbles = new ArrayList<Bubble>();
    private final ArrayList<Attractor> attractors = new ArrayList<Attractor>();
    private ActionGroup actions = null;

    private static final double BUBBLE_RETURN_TIME_HALF_LIFE = .3;
    private static final double BUBBLE_RETURN_TIME_LAMBDA = Math.log(2) / BUBBLE_RETURN_TIME_HALF_LIFE;

    private static final double FRICTION_VISCOUS = Math.log(2) / .2f; // Viscous friction factor

    private static final float BUBBLE_MAX_SPEED = 2500.f; // px.s⁻¹ : Max target speed in px/sec
    private static final float ATTRACTOR_SMOOTH_DIST = 50.f; // px : Size of the "gravity hole" around the attractor
    private static final float ATTRACTOR_STALL_DIST = 15.f; // px : Size of the "gravity hole" flat bottom
    private static final float ATTRACTOR_DIST_SUCK = 20.f; // px

    private static final float BORDER_REPULSION = 60000; // px.s⁻²

    private final float border_repulsion;
    private final float bubble_max_speed;
    private final float attractor_smooth_dist;
    private final float attractor_stall_dist;
    private final float attractor_dist_suck;

    private final float density;

    private float circle_radius;
    private final PointF circle_center = new PointF();

    public State curState = State.None;

    public BubbleModel(float screen_density, ModelCallback cb) {
        Log.d(TAG, "Creating BubbleModel");
        callback = cb;
        this.density = screen_density;
        attractor_dist_suck = ATTRACTOR_DIST_SUCK * density;
        bubble_max_speed = BUBBLE_MAX_SPEED * density;
        attractor_smooth_dist = ATTRACTOR_SMOOTH_DIST * density;
        attractor_stall_dist = ATTRACTOR_STALL_DIST * density;
        border_repulsion = BORDER_REPULSION * density;
    }

    public void setSize(int w, int h, float bubble_sz)
    {
        width = w;
        height = h;
        for (Attractor a : attractors) {
            a.setSize(w, h);
        }
        if (actions != null) {
            actions.order(width, height);
        }
        circle_radius = Math.min(width, height) / 2 - bubble_sz;
        circle_center.set(width/2, height/2);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getCircleSize() {
        return circle_radius;
    }

    public PointF getCircleCenter() {
        return circle_center;
    }

    public void addBubble(Bubble b) {
        bubbles.add(b);
    }

    public List<Bubble> getBubbles() {
        return bubbles;
    }

    public Bubble getBubble(String call) {
        for (Bubble b : bubbles) {
            if (!b.isUser && b.callIDEquals(call))
                return b;
        }
        return null;
    }

    public void removeBubble(SipCall sipCall) {
        bubbles.remove(getBubble(sipCall.getCallId()));

    }

    public void addAttractor(Attractor a) {
        attractors.add(a);
    }

    public List<Attractor> getAttractors() {
        return attractors;
    }

    public void clearAttractors() {
        attractors.clear();
    }

    public void setActions(Bubble b, ActionGroup actions) {
        actions.show(b);
        /*actions.enabled = true;
        actions.viewStart = 0;*/
        actions.order(width, height);
        this.actions = actions;
    }

    public ActionGroup getActions() {
        return actions;
    }

    public void clearActions() {
        this.actions = null;
    }

    public Bubble getUser() {
        for (Bubble b : bubbles) {
            if (b.isUser)
                return b;
        }
        return null;
    }

    public void clear() {
        clearAttractors();
        bubbles.clear();
    }

    public void grabBubble(Bubble b) {
        b.grab();
        callback.bubbleGrabbed(b);
    }

    public void ungrabBubble(Bubble b) {
        b.ungrab();
    }

    public void ejectBubble(Bubble b) {
        boolean eject = callback.bubbleEjected(b);
        if (eject) {
            b.close();
        }
    }

    public void update() {
        /* INFO: if you get a NullPointer or OutOfBounds exception here,
         * you may have some wrong/missing locks. */

        long now = System.nanoTime();

        // Do nothing if lastUpdate is in the future.
        if (lastUpdate > now)
            return;

        double ddt = Math.min((now - lastUpdate) / 1000000000.0, .2);
        lastUpdate = now;

        float dt = (float) ddt;

        //int attr_n = attractors.size();
        boolean actionAttr = false;

        // Iterators should not be used in frequently called methods
        // to avoid garbage collection glitches caused by iterator objects.
        for (int i = 0, n = bubbles.size(); i < n; i++) {
            Bubble b = bubbles.get(i);

            if (b.markedToDie) {
                b.update(dt);
                continue;
            }

            float bx = b.getPosX(), by = b.getPosY();

            Attractor attractor = null;
            PointF attractor_pos = b.attractionPoint;
            float attractor_dist = (attractor_pos.x - bx) * (attractor_pos.x - bx) + (attractor_pos.y - by) * (attractor_pos.y - by);

            boolean actionGrp = actions != null && actions.enabled && actions.bubble == b;
            final List<Attractor> attr = (actionGrp) ? actions.getActions() : attractors;

            for (Attractor t : attr) {
                float dx = t.pos.x - bx, dy = t.pos.y - by;
                float adist = dx * dx + dy * dy;
                if (adist < attractor_dist) {
                    attractor = t;
                    attractor_pos = t.pos;
                    attractor_dist = adist;
                }
            }

            b.attractor = attractor;

            if (!b.isGrabbed()) {
                if (actionGrp) {
                    for (Attractor anAttr : attr) {
                        if (anAttr == attractor) {
                            actionAttr = true;
                            break;
                        }
                    }
                }

                // float friction_coef = 1.f-FRICTION_VISCOUS*dt;
                double friction_coef = 1 + Math.expm1(-FRICTION_VISCOUS * ddt);
                b.speed.x *= friction_coef;
                b.speed.y *= friction_coef;

                float target_speed;
                float tdx = attractor_pos.x - bx, tdy = attractor_pos.y - by;
                float dist = Math.max(1.f, (float) Math.sqrt(tdx * tdx + tdy * tdy));
                if (dist > attractor_smooth_dist)
                    target_speed = bubble_max_speed;
                else if (dist < attractor_stall_dist)
                    target_speed = 0;
                else {
                    float a = (dist - attractor_stall_dist) / (attractor_smooth_dist - attractor_stall_dist);
                    target_speed = bubble_max_speed * a;
                }
                if (attractor != null) {
                    if (dist > attractor_smooth_dist)
                        b.setTargetScale(1.f);
                    else if (dist < attractor_stall_dist)
                        b.setTargetScale(2f);
                    else {
                        float a = (dist - attractor_stall_dist) / (attractor_smooth_dist - attractor_stall_dist);
                        b.setTargetScale(a * .8f + .2f);
                    }
                }

                // border repulsion

                if (bx < 0 && b.speed.x < 0) {
                    b.speed.x += dt * border_repulsion;
                } else if (bx > width && b.speed.x > 0) {
                    b.speed.x -= dt * border_repulsion;
                }
                if (by < 0 && b.speed.y < 0) {
                    b.speed.y += dt * border_repulsion;
                } else if (by > height && b.speed.y > 0) {
                    b.speed.y -= dt * border_repulsion;
                }


                b.speed.x += dt * target_speed * tdx / dist;
                b.speed.y += dt * target_speed * tdy / dist;

                double edt = -Math.expm1(-BUBBLE_RETURN_TIME_LAMBDA * ddt);
                double dx = (attractor_pos.x - bx) * edt + Math.min(bubble_max_speed, b.speed.x) * dt;
                double dy = (attractor_pos.y - by) * edt + Math.min(bubble_max_speed, b.speed.y) * dt;
                // Log.w(TAG, "update dx="+dt+" dy="+dy);
                b.setPos((float) (bx + dx), (float) (by + dy));

                /*if (b.isOnBorder(width, height)) {
                    ejectBubble(b);
                }*/

                if (attractor != null && attractor_dist < attractor_dist_suck * attractor_dist_suck) {
                    boolean removeBubble = attractor.callback.onBubbleSucked(b);
                    if (removeBubble) {
                        bubbles.remove(b);
                        n--;
                    } else {
                        b.setTargetScale(1.f);
                    }

                    if (actionGrp) {
                        actions.hide();
                    }
                }
            } else {
                actionAttr = true;
            }

            b.update(dt);
        }

        if (actions != null && actions.enabled && !actionAttr) {
            actions.hide();
        }
    }

}
