package com.sondreweb.kiosk_mode_alpha;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;

/**
 * Et View som ikke kan trykkes på, som ikke gjør noe med TouchEventene det mottar.
 */

public class CustomView extends ViewGroup {

    public static final String TAG = CustomView.class.getSimpleName();

    public CustomView(Context context){
        super(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    } //Layouten skal ikke forandre seg.

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //return super.onInterceptTouchEvent(ev);
        Log.v(TAG,"***INTERCEPTET TOUCH EVENT****");
        return true;
    }

}
