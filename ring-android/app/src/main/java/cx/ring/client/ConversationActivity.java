/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Authors:    Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *              Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.client;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.AppBarLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.fragments.ConversationFragment;
import cx.ring.interfaces.Colorable;
import cx.ring.utils.MediaButtonsHelper;

public class ConversationActivity extends AppCompatActivity implements Colorable {
    @BindView(R.id.toolbar_layout)
    AppBarLayout mToolbarLayout;

    @BindView(R.id.main_toolbar)
    Toolbar mToolbar;

    private ConversationFragment mConversationFragment;
    private String contactUri = null;
    private String accountId = null;

    private Intent mPendingIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RingApplication.getInstance().startDaemon();

        setContentView(R.layout.activity_conversation);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayHomeAsUpEnabled(true);

        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                decorView.getSystemUiVisibility()
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        ViewCompat.setOnApplyWindowInsetsListener(mToolbarLayout, (v, insets) -> {
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mToolbarLayout.getLayoutParams();
            params.topMargin = insets.getSystemWindowInsetTop();
            mToolbarLayout.setLayoutParams(params);
            insets.consumeSystemWindowInsets();
            return insets;
        });

        Intent intent = getIntent();
        String action = intent == null ? null : intent.getAction();

        if (intent != null) {
            contactUri = intent.getStringExtra(ConversationFragment.KEY_CONTACT_RING_ID);
            accountId = intent.getStringExtra(ConversationFragment.KEY_ACCOUNT_ID);
        } else if (savedInstanceState != null) {
            contactUri = savedInstanceState.getString(ConversationFragment.KEY_CONTACT_RING_ID);
            accountId = savedInstanceState.getString(ConversationFragment.KEY_ACCOUNT_ID);
        }
        if (mConversationFragment == null) {
            Bundle bundle = new Bundle();
            bundle.putString(ConversationFragment.KEY_CONTACT_RING_ID, contactUri);
            bundle.putString(ConversationFragment.KEY_ACCOUNT_ID, accountId);

            mConversationFragment = new ConversationFragment();
            mConversationFragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_frame, mConversationFragment, null)
                    .commit();
        }
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            mPendingIntent = intent;
        }
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        mConversationFragment.updateAdapterItem();
        super.onContextMenuClosed(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mPendingIntent != null) {
            handleShareIntent(mPendingIntent);
            mPendingIntent = null;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleShareIntent(intent);
    }

    private void handleShareIntent(Intent intent) {
        if (mConversationFragment != null)
            mConversationFragment.handleShareIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(ConversationFragment.KEY_CONTACT_RING_ID, contactUri);
        outState.putString(ConversationFragment.KEY_ACCOUNT_ID, accountId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return MediaButtonsHelper.handleMediaKeyCode(keyCode, mConversationFragment)
                || super.onKeyDown(keyCode, event);
    }

    public void setColor(@ColorInt int color) {
        colouriseToolbar(mToolbar, color);
        //mToolbar.setBackground(new ColorDrawable(color));
        //getWindow().setStatusBarColor(color);
    }

    public static void colouriseToolbar(Toolbar toolbar, @ColorInt int foreground) {
        final PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(foreground, PorterDuff.Mode.MULTIPLY);

        for (int i = 0; i < toolbar.getChildCount(); i++) {
            final View view = toolbar.getChildAt(i);
            if (view instanceof ImageButton) {
                ((ImageButton)view).getDrawable().setColorFilter(colorFilter);
            } else if (view instanceof ActionMenuView) {
                for (int j = 0; j < ((ActionMenuView) view).getChildCount(); j++) {
                    final View innerView = ((ActionMenuView)view).getChildAt(j);
                    //Any ActionMenuViews - icons that are not back button, text or overflow menu
                    if (innerView instanceof ActionMenuItemView) {
                        final Drawable[] drawables = ((ActionMenuItemView)innerView).getCompoundDrawables();
                        for (int k = 0; k < drawables.length; k++) {
                            final Drawable drawable = drawables[k];
                            if (drawable != null) {
                                final int drawableIndex = k;
                                //Set the color filter in separate thread
                                //by adding it to the message queue - won't work otherwise
                                innerView.post(() -> ((ActionMenuItemView) innerView).getCompoundDrawables()[drawableIndex].setColorFilter(colorFilter));
                            }
                        }
                    }
                }
            }
        }

        Drawable overflowIcon = toolbar.getOverflowIcon();
        if (overflowIcon != null) {
            overflowIcon.setColorFilter(colorFilter);
            toolbar.setOverflowIcon(overflowIcon);
        }
    }

}
