package cx.ring.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.flipkart.chatheads.ui.ChatHead;
import com.flipkart.chatheads.ui.ChatHeadViewAdapter;
import com.flipkart.chatheads.ui.MinimizedArrangement;
import com.flipkart.chatheads.ui.container.DefaultChatHeadManager;
import com.flipkart.chatheads.ui.container.WindowManagerContainer;
import com.flipkart.circularImageView.CircularDrawable;
import com.flipkart.circularImageView.notification.CircularNotificationDrawer;
import com.flipkart.circularImageView.notification.NotificationDrawer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import cx.ring.R;

public class ChatHeadService extends Service {
    private boolean mBound = false;
    private LocalService service;

    private final IBinder mBinder = new LocalBinder();
    private WindowManagerContainer mWindowManagerContainer;
    private DefaultChatHeadManager<String> mChatHeadManager;
    private Map<String, View> mViewCache = new HashMap<>();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWindowManagerContainer.destroy();
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder s) {
            LocalService.LocalBinder binder = (LocalService.LocalBinder) s;
            service = binder.getService();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(LocalService.ACTION_INCOMING_MESSAGE);

            registerReceiver(receiver, intentFilter);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mBound = false;
        }
    };

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case LocalService.ACTION_INCOMING_MESSAGE:
                    addChatHead(intent.getStringExtra("account"));
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        mWindowManagerContainer = new WindowManagerContainer(this);
        mChatHeadManager = new DefaultChatHeadManager<>(this, mWindowManagerContainer);
        mChatHeadManager.setViewAdapter(new ChatHeadViewAdapter<String>() {
            @Override
            public View attachView(String key, ChatHead chatHead, ViewGroup parent) {
                View cachedView = mViewCache.get(key);
                if (cachedView == null) {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                    View view = inflater.inflate(R.layout.fragment_test_bubble, parent, false);
                    TextView identifier = (TextView) view.findViewById(R.id.identifier);
                    identifier.setText(key);
                    cachedView = view;
                    mViewCache.put(key, view);
                }
                parent.addView(cachedView);
                return cachedView;
            }

            @Override
            public void detachView(String key, ChatHead<? extends Serializable> chatHead, ViewGroup parent) {
                View cachedView = mViewCache.get(key);
                if (cachedView != null) {
                    parent.removeView(cachedView);
                }
            }

            @Override
            public void removeView(String key, ChatHead<? extends Serializable> chatHead, ViewGroup parent) {
                View cachedView = mViewCache.get(key);
                if (cachedView != null) {
                    mViewCache.remove(key);
                    parent.removeView(cachedView);
                }
            }

            @Override
            public Drawable getChatHeadDrawable(String key) {
                return ChatHeadService.this.getChatHeadDrawable(key);
            }
        });

        addChatHead(String.valueOf(1));
        mChatHeadManager.setArrangement(MinimizedArrangement.class, null);

        if (!mBound) {
            Intent intent = new Intent(this, LocalService.class);
            startService(intent);
            bindService(intent, mConnection, BIND_AUTO_CREATE | BIND_IMPORTANT | BIND_ABOVE_CLIENT);
        }
    }

    private Drawable getChatHeadDrawable(String key) {
        Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.ic_contact_picture);
        CircularDrawable circularDrawable = new CircularDrawable();
        circularDrawable.setBitmapOrTextOrIcon(image);
        int badgeCount = 1;
        NotificationDrawer notificationDrawer = new CircularNotificationDrawer()
                .setNotificationText(String.valueOf(badgeCount))
                .setNotificationAngle(135)
                .setNotificationColor(Color.WHITE, Color.RED);
        circularDrawable.setNotificationDrawer(notificationDrawer);
        circularDrawable.setBorder(Color.WHITE, 3);
        return circularDrawable;
    }

    public void addChatHead(String key) {
        mChatHeadManager.addChatHead(key, false, true);
        mChatHeadManager.bringToFront(mChatHeadManager.findChatHeadByKey(key));
    }

    public void removeChatHead(String key) {
        mChatHeadManager.removeChatHead(key, true);
    }

    public class LocalBinder extends Binder {
        public ChatHeadService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ChatHeadService.this;
        }
    }
}
