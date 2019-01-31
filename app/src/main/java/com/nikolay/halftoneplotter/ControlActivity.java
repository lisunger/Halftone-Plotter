package com.nikolay.halftoneplotter;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.nikolay.halftoneplotter.bluetooth.BluetoothCommands;

public class ControlActivity extends AppCompatActivity {

    // TODO init
    private static Integer STEP_LEFT = BluetoothCommands.VALUE_LEFT;
    private static Integer STEP_UP = BluetoothCommands.VALUE_UP;
    private static Integer STEP_RIGHT = BluetoothCommands.VALUE_RIGHT;
    private static Integer STEP_DOWN = BluetoothCommands.VALUE_DOWN;
    private static Integer REV_LEFT = BluetoothCommands.ROTATION_NEMA;
    private static Integer REV_UP = BluetoothCommands.ROTATION_BYJ;
    private static Integer REV_RIGHT = BluetoothCommands.ROTATION_NEMA;
    private static Integer REV_DOWN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        setUpUi();

    }

    private void setUpUi() {
        ((ControlButton)findViewById(R.id.btn_step_left)).setSteps(STEP_LEFT);
        ((ControlButton)findViewById(R.id.btn_step_up)).setSteps(STEP_UP);
        ((ControlButton)findViewById(R.id.btn_step_right)).setSteps(STEP_RIGHT);
        ((ControlButton)findViewById(R.id.btn_step_down)).setSteps(STEP_DOWN);

        ((ControlButton)findViewById(R.id.btn_rev_left)).setSteps(REV_LEFT);
        ((ControlButton)findViewById(R.id.btn_rev_up)).setSteps(REV_UP);
        ((ControlButton)findViewById(R.id.btn_rev_right)).setSteps(REV_RIGHT);
        ((ControlButton)findViewById(R.id.btn_rev_down)).setSteps(REV_DOWN);
    }

}
