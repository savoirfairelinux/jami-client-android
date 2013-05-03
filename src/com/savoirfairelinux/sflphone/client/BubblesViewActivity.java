package com.savoirfairelinux.sflphone.client;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.model.BubblesView;

public class BubblesViewActivity extends Activity 
{
    
    BubblesView view;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bubbleview_layout);
        
        Button b = (Button) findViewById(R.id.add_bubble);
        view = (BubblesView) findViewById(R.id.main_view);
        
        b.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                view.addBubble();
                
            }
        });

    }
}