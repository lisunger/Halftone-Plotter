package com.nikolay.halftoneplotter;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

public class ControlButton extends android.support.v7.widget.AppCompatImageButton {

    public ControlButton(Context context) {
        super(context);
        this.setBackgroundTintList(getResources().getColorStateList(R.color.colors_fab, getContext().getTheme()));
    }

    public ControlButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setBackgroundTintList(getResources().getColorStateList(R.color.colors_fab, getContext().getTheme()));
    }
}
