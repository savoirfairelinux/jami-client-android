package com.savoirfairelinux.sflphone.client;

import android.app.Activity;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.model.Bubble;
import com.savoirfairelinux.sflphone.model.BubbleModel;
import com.savoirfairelinux.sflphone.model.BubblesView;

public class BubblesViewActivity extends Activity {
    private static final String TAG = BubblesViewActivity.class.getSimpleName();

    BubblesView view;

    PointF screenCenter;
    int radiusCalls;
    int angle_part;

    BubbleModel model;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bubbleview_layout);

        model = new BubbleModel();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenCenter = new PointF(metrics.widthPixels / 2, metrics.heightPixels / 3);
        radiusCalls = metrics.widthPixels / 2 - 150;
        // model.listBubbles.add(new Bubble(this, metrics.widthPixels / 2, metrics.heightPixels / 4, 150, R.drawable.me));
        // model.listBubbles.add(new Bubble(this, metrics.widthPixels / 2, metrics.heightPixels / 4 * 3, 150, R.drawable.callee));

        view = (BubblesView) findViewById(R.id.main_view);
        view.setModel(model);

        ((Button) findViewById(R.id.add_bubble)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addBubble();
            }
        });

        ((Button) findViewById(R.id.remove_bubble)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                removeBubble();
            }
        });

    }

    public void addBubble() {
        /*
         * Bubble.Builder builder = new Bubble.Builder(getContext()); builder.setRadiusPixels(200).setX(200).setY(300);
         */
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Bubble b = new Bubble(this, metrics.widthPixels / 3, metrics.heightPixels / 4 * 3, 150, -1);
        model.listBubbles.add(b);

        angle_part = 360 / model.listBubbles.size();

        double dX = 0;
        double dY = 0;
        for (int i = 0; i < model.listBubbles.size(); ++i) {
            dX = Math.cos(Math.toRadians(angle_part * i)) * radiusCalls;
            dY = Math.sin(Math.toRadians(angle_part * i)) * radiusCalls;
            Log.i(TAG, "dX " + dX + " dY " + dY);
            model.listBubbles.get(i).setAttractor(new PointF((int) dX + screenCenter.x, (int) dY + screenCenter.y));
        }

        // listBubbles.get(listBubbles.size() - 1).setRegion(width, height);
    }

    public void removeBubble() {
        
        if (model.listBubbles.isEmpty()) {
            return;
        }
        /*
         * Bubble.Builder builder = new Bubble.Builder(getContext()); builder.setRadiusPixels(200).setX(200).setY(300);
         */
        // DisplayMetrics metrics = getResources().getDisplayMetrics();
        // Bubble b = new Bubble(this, metrics.widthPixels / 3, metrics.heightPixels / 4 * 3, 150, -1);
        synchronized (model) {
            model.listBubbles.remove(model.listBubbles.size() - 1);
        }

        if (model.listBubbles.isEmpty()) {
            return;
        }

        angle_part = 360 / model.listBubbles.size();

        Log.i(TAG, "Angle:" + angle_part);
        double dX = 0;
        double dY = 0;
        for (int i = 0; i < model.listBubbles.size(); ++i) {
            dX = Math.cos(Math.toRadians(angle_part * i)) * radiusCalls;
            dY = Math.sin(Math.toRadians(angle_part * i)) * radiusCalls;
            Log.i(TAG, "dX " + dX + " dY " + dY);
            model.listBubbles.get(i).setAttractor(new PointF((int) dX + screenCenter.x, (int) dY + screenCenter.y));
        }

        // listBubbles.get(listBubbles.size() - 1).setRegion(width, height);
    }
}