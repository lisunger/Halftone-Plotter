package com.nikolay.halftoneplotter.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.nikolay.halftoneplotter.R;

public class ControlButton extends android.support.v7.widget.AppCompatImageButton {

    private Integer steps = 0;

    public ControlButton(Context context) {
        super(context);
        this.setBackgroundTintList(getResources().getColorStateList(R.color.colors_enable, getContext().getTheme()));
        this.setOnClickListener(new ControlButtonClickListener());
    }

    public ControlButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setBackgroundTintList(getResources().getColorStateList(R.color.colors_enable, getContext().getTheme()));
        this.setOnClickListener(new ControlButtonClickListener());
    }

    private class ControlButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // TODO send command to the service
        }
    }

    public Integer getSteps() {
        return steps;
    }

    public void setSteps(Integer steps) {
        this.steps = steps;
    }
}
