package com.nikolay.halftoneplotter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.nikolay.halftoneplotter.bluetooth.BluetoothCommands;

public class ControlActivity extends AppCompatActivity {

    private Integer mStepsX;
    private Integer mStepsY;
    private int mCoordX;
    private int mCoordY;
    private boolean mUsingSteps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        setUpUi();

    }

    private void setUpUi() {
        setButtonsSteps();
        setButtonColors();
        enableUI(false);
        setClickListeners();
    }

    private void enableUI(boolean enable) {
        findViewById(R.id.input_steps).setEnabled(enable);
        findViewById(R.id.input_x).setEnabled(enable);
        findViewById(R.id.input_y).setEnabled(enable);
        findViewById(R.id.fab).setEnabled(enable);
        findViewById(R.id.chkbox_precise).setEnabled(enable);
        findViewById(R.id.btn_coord).setEnabled(enable);
        findViewById(R.id.btn_step_left).setEnabled(enable);
        findViewById(R.id.btn_step_up).setEnabled(enable);
        findViewById(R.id.btn_step_right).setEnabled(enable);
        findViewById(R.id.btn_step_down).setEnabled(enable);
        findViewById(R.id.btn_rev_left).setEnabled(enable);
        findViewById(R.id.btn_rev_up).setEnabled(enable);
        findViewById(R.id.btn_rev_right).setEnabled(enable);
        findViewById(R.id.btn_rev_down).setEnabled(enable);
    }

    private void setClickListeners() {
        // TODO connect button

        ((CheckBox)findViewById(R.id.chkbox_precise)).setChecked(false);
        findViewById(R.id.chkbox_precise).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(((CheckBox) v).isChecked()) {
                    findViewById(R.id.input_steps).setEnabled(true);
                    findViewById(R.id.btn_rev_left).setEnabled(false);
                    findViewById(R.id.btn_rev_up).setEnabled(false);
                    findViewById(R.id.btn_rev_right).setEnabled(false);
                    findViewById(R.id.btn_rev_down).setEnabled(false);
                    mUsingSteps = true;
                }
                else {
                    findViewById(R.id.input_steps).setEnabled(false);
                    findViewById(R.id.btn_rev_left).setEnabled(true);
                    findViewById(R.id.btn_rev_up).setEnabled(true);
                    findViewById(R.id.btn_rev_right).setEnabled(true);
                    findViewById(R.id.btn_rev_down).setEnabled(true);
                    mUsingSteps = false;
                    setButtonsSteps();
                }
            }
        });


        // TODO getCoordinates button

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = ControlActivity.this.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(getString(R.string.coordinate_x_key), mCoordX);
                editor.putInt(getString(R.string.coordinate_y_key), mCoordY);
                editor.apply();

                Intent intent = new Intent(ControlActivity.this, DrawActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setButtonsSteps() {
        ((ControlButton)findViewById(R.id.btn_step_left)).setSteps(BluetoothCommands.VALUE_LEFT);
        ((ControlButton)findViewById(R.id.btn_step_up)).setSteps(BluetoothCommands.VALUE_UP);
        ((ControlButton)findViewById(R.id.btn_step_right)).setSteps(BluetoothCommands.VALUE_RIGHT);
        ((ControlButton)findViewById(R.id.btn_step_down)).setSteps(BluetoothCommands.VALUE_DOWN);

        ((ControlButton)findViewById(R.id.btn_rev_left)).setSteps(BluetoothCommands.ROTATION_NEMA);
        ((ControlButton)findViewById(R.id.btn_rev_up)).setSteps(BluetoothCommands.ROTATION_BYJ);
        ((ControlButton)findViewById(R.id.btn_rev_right)).setSteps(BluetoothCommands.ROTATION_NEMA);
        ((ControlButton)findViewById(R.id.btn_rev_down)).setSteps(BluetoothCommands.ROTATION_BYJ);
    }

    private void setButtonColors() {
        findViewById(R.id.fab).setBackgroundTintList(getResources().getColorStateList(R.color.colors_enable, getTheme()));
        findViewById(R.id.btn_coord).setBackgroundTintList(getResources().getColorStateList(R.color.colors_enable, getTheme()));
    }

}
