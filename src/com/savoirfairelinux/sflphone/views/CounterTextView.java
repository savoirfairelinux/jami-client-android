package com.savoirfairelinux.sflphone.views;

import java.util.Observable;
import java.util.Observer;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class CounterTextView extends TextView implements Observer {

    public CounterTextView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public CounterTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public CounterTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    @Override
    public void update(Observable observable, Object data) {
        Log.i("TextView", "updating");

    }

}
