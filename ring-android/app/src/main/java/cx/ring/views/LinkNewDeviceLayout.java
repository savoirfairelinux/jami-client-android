/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Loïc Siret <loic.siret@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.LinearLayout;

import cx.ring.account.RingAccountSummaryFragment;

public class LinkNewDeviceLayout extends LinearLayout {

    RingAccountSummaryFragment container;

    public LinkNewDeviceLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinkNewDeviceLayout(Context context) {
        super(context);
    }

    /**
     * Overrides the handling of the back key to move back to
     * dismiss the view, instead of only dismissing the input method.
     */
    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        if (container != null &&
                event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            KeyEvent.DispatcherState state = getKeyDispatcherState();
            if (state != null) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getRepeatCount() == 0) {
                    state.startTracking(event, this);
                    return true;
                } else if (event.getAction() == KeyEvent.ACTION_UP
                        && !event.isCanceled() && state.isTracking(event)) {
                    container.onBackPressed();
                    return true;
                }
            }
        }

        return super.dispatchKeyEventPreIme(event);
    }

    public void setContainer(RingAccountSummaryFragment container) {
        this.container = container;
    }
}
