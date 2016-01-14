/*
 *  Copyright (C) 2004-2013 Savoir-faire Linux Inc.
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
 *  terms of the OpenSSL or SSLeay licenses, Savoir-faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 *
 *  Part of the library: 
 *  https://github.com/bauerca/drag-sort-listview
 *  No longer maintained
 *
 *
 */

package cx.ring.views.dragsortlv;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

/**
 * Lightweight ViewGroup that wraps list items obtained from user's
 * ListAdapter. ItemView expects a single child that has a definite
 * height (i.e. the child's layout height is not MATCH_PARENT).
 * The width of
 * ItemView will always match the width of its child (that is,
 * the width MeasureSpec given to ItemView is passed directly
 * to the child, and the ItemView measured width is set to the
 * child's measured width). The height of ItemView can be anything;
 * the 
 * 
 *
 * The purpose of this class is to optimize slide
 * shuffle animations.
 */
public class DragSortItemView extends ViewGroup {

    private int mGravity = Gravity.TOP;

    public DragSortItemView(Context context) {
        super(context);

        // always init with standard ListView layout params
        setLayoutParams(new AbsListView.LayoutParams(
                LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        //setClipChildren(true);
    }

    public void setGravity(int gravity) {
        mGravity = gravity;
    }

    public int getGravity() {
        return mGravity;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final View child = getChildAt(0);

        if (child == null) {
            return;
        }

        if (mGravity == Gravity.TOP) {
            child.layout(0, 0, getMeasuredWidth(), child.getMeasuredHeight());
        } else {
            child.layout(0, getMeasuredHeight() - child.getMeasuredHeight(), getMeasuredWidth(), getMeasuredHeight());
        }
    }

    /**
     * 
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        final View child = getChildAt(0);
        if (child == null) {
            setMeasuredDimension(0, width);
            return;
        }

        if (child.isLayoutRequested()) {
            // Always let child be as tall as it wants.
            measureChild(child, widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        }

        if (heightMode == MeasureSpec.UNSPECIFIED) {
            ViewGroup.LayoutParams lp = getLayoutParams();

            if (lp.height > 0) {
                height = lp.height;
            } else {
                height = child.getMeasuredHeight();
            }
        }

        setMeasuredDimension(width, height);
    }

}
