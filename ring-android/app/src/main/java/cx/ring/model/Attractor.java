/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Adrien Beraud <adrien.beraud@gmail.com>
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.RectF;

public class Attractor {

    public interface Callback {

		/**
		 * Called when a bubble is on the "active" zone of the attractor.
		 * 
		 * @param b The bubble that is on the attractor.
		 * @return true if the bubble should be removed from the model, false otherwise.
		 */
		public boolean onBubbleSucked(Bubble b);
	}

    public enum Type {
        POINT, BORDER
    }

    final Callback callback;
    final Type type;

    private final RectF bounds = new RectF();
    private final RectF boundsScaled = new RectF();
    final PointF pos = new PointF();
	final float radius;
    private final Bitmap img;
    final String name;

	public Attractor(PointF pos, float size, Callback callback, Bitmap img) {
        this.type = Type.POINT;
        this.callback = callback;
        this.pos.set(pos);
		this.radius = size/2;
		this.img = img;
        this.name = null;
        setBounds();
	}

	public Attractor(PointF pos, float radius, Callback callback, Context c, int resId) {
		this(pos, radius, callback, BitmapFactory.decodeResource(c.getResources(), resId));
	}

    public Attractor(String name, float size, Callback callback, Bitmap img) {
        this.type = Type.POINT;
        this.name = name;
        this.callback = callback;
        this.radius = size/2;
        this.img = img;
        setBounds();
    }

    public void setSize(float w, float h)
    {
        if (type != Type.BORDER)
            return;
        pos.set(w, h);
        setBounds();
    }

    public void setPos(float x, float y) {
        pos.set(x, y);
        setBounds();
    }

    private void setBounds() {
        bounds.set(pos.x - radius, pos.y - radius, pos.x + radius, pos.y + radius);
    }

	public RectF getBounds() {
		return bounds;
	}

    public RectF getBounds(float scale) {
        float r = radius * scale;
        boundsScaled.set(pos.x - r, pos.y - r, pos.x + r, pos.y + r);
        return boundsScaled;
    }

    public RectF getBounds(float scale, PointF start, float d) {
        float r = radius * scale;
        float md = 1.f - d;
        float x = pos.x * d + start.x * md;
        float y = pos.y * d + start.y * md;
        boundsScaled.set(x - r, y - r, x + r, y + r);
        return boundsScaled;
    }

	public Bitmap getBitmap() {
		return img;
	}

}
