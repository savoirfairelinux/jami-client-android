package com.savoirfairelinux.sflphone.client;


import android.app.Activity;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.model.Bubble;
import com.savoirfairelinux.sflphone.model.BubbleModel;
import com.savoirfairelinux.sflphone.model.BubblesView;

public class BubblesViewActivity extends Activity
{
	private static final String TAG = BubblesViewActivity.class.getSimpleName();

	BubblesView view;

	BubbleModel model;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bubbleview_layout);

		model = new BubbleModel();
		model.listBubbles.add(new Bubble(this, 200, 300, 150, R.drawable.me));
		model.listBubbles.add(new Bubble(this, 200, 700, 150, R.drawable.callee));

		Button b = (Button) findViewById(R.id.add_bubble);
		view = (BubblesView) findViewById(R.id.main_view);
		view.setModel(model);

		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				addBubble();
			}
		});

	}

	public void addBubble()
	{
		/*Bubble.Builder builder = new Bubble.Builder(getContext());
		builder.setRadiusPixels(200).setX(200).setY(300);*/
		Bubble b = new Bubble(this, 200, 300, 200, -1);
		b.attractor = new PointF(b.getPosX(), b.getPosY());
		model.listBubbles.add(b);
		//listBubbles.get(listBubbles.size() - 1).setRegion(width, height);
	}

}