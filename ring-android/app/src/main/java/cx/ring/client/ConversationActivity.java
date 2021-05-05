/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.ActivityConversationBinding;
import cx.ring.fragments.ConversationFragment;
import cx.ring.interfaces.Colorable;
import cx.ring.services.NotificationServiceImpl;
import cx.ring.utils.ConversationPath;

public class ConversationActivity extends AppCompatActivity implements Colorable {

    private ConversationFragment mConversationFragment;
    private ConversationPath conversationPath = null;

    private Intent mPendingIntent = null;
    private ActivityConversationBinding binding;

    private boolean mIsBubble;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent == null ? null : intent.getAction();

        if (intent != null) {
            conversationPath = ConversationPath.fromIntent(intent);
        } else if (savedInstanceState != null) {
            conversationPath = ConversationPath.fromBundle(savedInstanceState);
        }
        if (conversationPath == null) {
            finish();
            return;
        }
        mIsBubble = getIntent().getBooleanExtra(NotificationServiceImpl.EXTRA_BUBBLE, false);

        JamiApplication.getInstance().startDaemon();
        binding = ActivityConversationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.mainToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayHomeAsUpEnabled(true);

        if (mConversationFragment == null) {
            Bundle bundle = conversationPath.toBundle();
            bundle.putBoolean(NotificationServiceImpl.EXTRA_BUBBLE, mIsBubble);

            mConversationFragment = new ConversationFragment();
            mConversationFragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_frame, mConversationFragment, null)
                    .commit();
        }
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            mPendingIntent = intent;
        }
    }

    @Override
    public void onContextMenuClosed(@NonNull Menu menu) {
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        conversationPath.toBundle(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.isCtrlPressed()) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (mConversationFragment != null)
                    mConversationFragment.sendMessageText();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public void setColor(@ColorInt int color) {
        //colouriseToolbar(binding.mainToolbar, color);
        //mToolbar.setBackground(new ColorDrawable(color));
        //getWindow().setStatusBarColor(color);
    }

}
