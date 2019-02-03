package com.nikolay.halftoneplotter;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ToggleButton;

public class ConnectToggle extends ToggleButton {

    private Integer steps = 0;

    public ConnectToggle(Context context) {
        super(context);
        this.setBackgroundTintList(getResources().getColorStateList(R.color.colors_toggle, getContext().getTheme()));
    }

    public ConnectToggle(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setBackgroundTintList(getResources().getColorStateList(R.color.colors_toggle, getContext().getTheme()));
    }
}
