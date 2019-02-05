package com.nikolay.halftoneplotter.components;

import android.content.Context;
import android.util.AttributeSet;

import com.nikolay.halftoneplotter.R;

public class ControlButton extends android.support.v7.widget.AppCompatImageButton {

    private Integer steps = 0;

    public ControlButton(Context context) {
        super(context);
        this.setBackgroundTintList(getResources().getColorStateList(R.color.colors_enable, getContext().getTheme()));
    }

    public ControlButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setBackgroundTintList(getResources().getColorStateList(R.color.colors_enable, getContext().getTheme()));
    }

    public Integer getSteps() {
        return steps;
    }

    public void setSteps(Integer steps) {
        this.steps = steps;
    }
}
