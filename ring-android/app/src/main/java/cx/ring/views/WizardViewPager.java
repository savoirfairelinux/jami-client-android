package cx.ring.views;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by abonnet on 16-11-17.
 */

public class WizardViewPager extends ViewPager {

    public WizardViewPager(Context context){
        super(context);
    }

    public WizardViewPager(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event){
        return false;
    }
}
