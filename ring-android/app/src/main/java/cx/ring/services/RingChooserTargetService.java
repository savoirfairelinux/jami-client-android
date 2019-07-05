/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.services;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Singleton;

import cx.ring.application.RingApplication;
import cx.ring.contacts.AvatarFactory;
import cx.ring.facades.ConversationFacade;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import io.reactivex.schedulers.Schedulers;

@RequiresApi(api = Build.VERSION_CODES.M)
public class RingChooserTargetService extends ChooserTargetService {

    @Inject
    @Singleton
    ConversationFacade conversationFacade;

    private int targetSize;

    @Override
    public void onCreate() {
        super.onCreate();
        RingApplication.getInstance().startDaemon();
        RingApplication.getInstance().getRingInjectionComponent().inject(this);
        targetSize = (int) (AvatarFactory.SIZE_NOTIF * getResources().getDisplayMetrics().density);
    }

    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName componentName, IntentFilter intentFilter) {
        return conversationFacade
                .getCurrentAccountSubject()
                .firstOrError()
                .flatMap(a -> a
                        .getConversationsSubject()
                        .firstOrError()
                        .map(conversations -> {
                            List<Future<Bitmap>> futureIcons = new ArrayList<>(conversations.size());
                            for (Conversation conversation : conversations) {
                                CallContact contact = conversation.getContact();
                                futureIcons.add(AvatarFactory.getBitmapAvatar(this, contact, targetSize)
                                        .subscribeOn(Schedulers.computation())
                                        .toFuture());
                            }
                            int i=0;
                            List<ChooserTarget> choosers = new ArrayList<>(conversations.size());
                            for (Conversation conversation : conversations) {
                                CallContact contact = conversation.getContact();
                                Bundle bundle = new Bundle();
                                bundle.putString(ConversationFragment.KEY_ACCOUNT_ID, a.getAccountID());
                                bundle.putString(ConversationFragment.KEY_CONTACT_RING_ID, contact.getPrimaryNumber());
                                Icon icon = null;
                                try {
                                    icon = Icon.createWithBitmap(futureIcons.get(i).get());
                                } catch (Exception e) {
                                    Log.w("RingChooserService", "Failed to load icon", e);
                                }
                                ChooserTarget target = new ChooserTarget(contact.getDisplayName(), icon, 1.f-(i/(float)conversations.size()), componentName, bundle);
                                choosers.add(target);
                                i++;
                            }
                            return choosers;
                        }))
                .onErrorReturn(e -> new ArrayList<>())
                .blockingGet();
    }
}
