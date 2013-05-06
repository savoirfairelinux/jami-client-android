package com.savoirfairelinux.sflphone.model;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;

public class BubblesView extends SurfaceView implements SurfaceHolder.Callback, OnTouchListener
{
	private static final String TAG = BubblesView.class.getSimpleName();

	private BubblesThread thread = null;
	private BubbleModel model;

	public BubblesView(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		// create thread only; it's started in surfaceCreated()
		createThread();

		setOnTouchListener(this);
		setFocusable(true);
	}

	private void createThread() {
		if(thread != null) return;
		thread = new BubblesThread(getHolder(), getContext(), new Handler() {
			@Override
			public void handleMessage(Message m)
			{
				/*  mStatusText.setVisibility(m.getData().getInt("viz"));
				  mStatusText.setText(m.getData().getString("text"));*/
			}
		});
		if(model != null)
			thread.setModel(model);
	}

	public void setModel(BubbleModel model)
	{
		this.model = model;
		thread.setModel(model);
	}

	/*@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (!hasWindowFocus) {
			thread.pause();
		}
	}*/

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
		Log.w(TAG, "surfaceChanged");
		thread.setSurfaceSize(width, height);
	}

	/*
	 * Callback invoked when the Surface has been created and is ready to be
	 * used.
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created
		createThread();

		Log.w(TAG, "surfaceCreated");
		thread.setRunning(true);
		thread.start();
	}

	/*
	 * Callback invoked when the Surface has been destroyed and must no longer
	 * be touched. WARNING: after this method returns, the Surface/Canvas must
	 * never be touched again!
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		// we have to tell thread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode
		Log.w(TAG, "surfaceDestroyed");
		boolean retry = true;
		thread.setRunning(false);
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
		thread = null;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		Log.w(TAG, "onTouch " + event.getAction());

		int action = event.getActionMasked();

		if(action == MotionEvent.ACTION_DOWN) {
			for (Bubble b : model.listBubbles) {
				if(b.intersects(event.getX(), event.getY())) {
					b.dragged = true;
				}
			}
		} else if (action == MotionEvent.ACTION_MOVE) {
			for (Bubble b : model.listBubbles) {
				if(b.dragged) {
					b.setPos(event.getX(), event.getY());
					return true;
				}
			}
		} else if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			for (Bubble b : model.listBubbles) {
				if(b.dragged) {
					b.dragged = false;
				}
			}
		}
		return true;
	}

	class BubblesThread extends Thread
	{
		private boolean running = false;
		private SurfaceHolder surfaceHolder;

		BubbleModel model = null;

		public BubblesThread(SurfaceHolder holder, Context context, Handler handler)
		{
			surfaceHolder = holder;
		}

		public void setModel(BubbleModel model)
		{
			this.model = model;
		}

		@Override
		public void run()
		{
			while (running) {
				Canvas c = null;
				try {
					c = surfaceHolder.lockCanvas(null);
					synchronized (surfaceHolder) {

						// for the case the surface is destroyed while already in the loop
						if(c == null || model == null) continue;

						//Log.w(TAG, "Thread doDraw");
						model.update();
						doDraw(c);
					}
				} finally {
					// do this in a finally so that if an exception is thrown
					// during the above, we don't leave the Surface in an
					// inconsistent state
					if (c != null)
						surfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}

		public void setRunning(boolean b)
		{
			running = b;
		}

		public void setSurfaceSize(int width, int height)
		{
			synchronized (surfaceHolder) {
				if(model != null) {
					model.width = width;
					model.height = height;
				}

				// don't forget to resize the background image
				//  mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage, width, height, true);
			}
		}

		/*private void updatePhysics()
		{
			long now = System.currentTimeMillis();

			// Do nothing if lastUpdate is in the future.
			if (model.lastUpdate > now)
				return;

			double elapsed = (now - model.lastUpdate) / 1000.0;
		}
		 */
		private void doDraw(Canvas canvas)
		{
			canvas.drawColor(Color.WHITE);

			for(int i=0, n=model.listBubbles.size(); i<n; i++) {
				Bubble b = model.listBubbles.get(i);
				canvas.drawBitmap(b.getBitmap(), null, b.getBounds(), null);
			}
		}
	}

}
