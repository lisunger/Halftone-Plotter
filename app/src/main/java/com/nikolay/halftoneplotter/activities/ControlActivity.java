package com.nikolay.halftoneplotter.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.nikolay.halftoneplotter.R;
import com.nikolay.halftoneplotter.bluetooth.BluetoothCommands;
import com.nikolay.halftoneplotter.bluetooth.BluetoothStateChangeReceiver;
import com.nikolay.halftoneplotter.bluetooth.BluetoothUtils;
import com.nikolay.halftoneplotter.bluetooth.SocketContainer;
import com.nikolay.halftoneplotter.bluetooth.services.BluetoothConnectionService;
import com.nikolay.halftoneplotter.components.ControlButton;

import java.io.IOException;

public class ControlActivity extends AppCompatActivity implements BluetoothConnectionService.BluetoothResponseListener {

    private static final String TAG = "Lisko: " + ControlActivity.class.getName();

    private Integer mStepsX;
    private Integer mStepsY;
    private int mCoordX = 100;
    private int mCoordY = 100;
    private boolean mUsingPixels;

    private boolean mScanning = false;
    private boolean mScanStartedByApp = false;
    private boolean mConnected = false;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mIsExecuting = false;

    private BluetoothDevice mHc05device = null;
    private BluetoothConnectionService mBluetoothService = null;
    private BluetoothSocket mSocket;

    private EditText mInputX;
    private EditText mInputY;
    private FloatingActionButton mFab;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        setUpUi();

        BluetoothUtils.requestLocationPermission(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "Bluetooth is not supported");
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
            finish();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, BluetoothUtils.REQUEST_ENABLE_BT);
            findViewById(R.id.btn_connect).setEnabled(false);
        }

        BluetoothUtils.registerBluetoothStateReceiver(this, mBluetoothStateBroadcastReceiver);
        BluetoothUtils.registerConnectionStateReceiver(this, mConnectionStateReceiver);
        BluetoothUtils.registerBluetoothDeviceReceiver(this, mDeviceFoundReceiver);

        SocketContainer.setBluetoothSocket(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO check the UI, connection etc.
        if(mConnected) {
            enableUI(true);
        }
        else {
            enableUI(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BluetoothUtils.REQUEST_ENABLE_BT: {
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Bluetooth started");
                    findViewById(R.id.btn_connect).setEnabled(true);
                } else {
                    Log.d(TAG, "Bluetooth canceled");
                    finish();
                }
                break;
            }
            case BluetoothUtils.REQUEST_PERMISSION_LOCATION: {
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Permission granted");
                } else if (resultCode == RESULT_CANCELED) {
                    Log.d(TAG, "Permission refused");
                    finish();
                }
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ControlActivity destroyed");
        unregisterReceiver(mBluetoothStateBroadcastReceiver);
        unregisterReceiver(mConnectionStateReceiver);
        unregisterReceiver(mDeviceFoundReceiver);
    }

    private void connectToHc05() {
        mBluetoothAdapter.cancelDiscovery();
        try {
            mSocket = mHc05device.createRfcommSocketToServiceRecord(BluetoothUtils.PLOTTER_UUID);
            SocketContainer.setBluetoothSocket(mSocket);
            Intent intent = new Intent(this, BluetoothConnectionService.class);
            startService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } catch (IOException e) {
            Log.d(TAG, "Cannot open socket.");
            e.printStackTrace();
            mConnected = false;
        }
    }

    private void setUpUi() {
        mInputX = findViewById(R.id.input_x);
        mInputY = findViewById(R.id.input_y);
        mFab = findViewById(R.id.fab);

        setButtonCommands();
        setButtonSteps();
        setButtonListeners();
        setButtonColors();
        enableUI(false);
        setClickListeners();
        findViewById(R.id.overlay).setVisibility(View.GONE);
    }

    private void enableUI(boolean enable) {
        findViewById(R.id.input_layout_steps).setEnabled(enable);
        mInputX.setEnabled(enable);
        mInputY.setEnabled(enable);
        mFab.setEnabled(enable);
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
        findViewById(R.id.btn_dot).setEnabled(enable);
    }

    private void setClickListeners() {
        // Connect button
        findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mConnected) { //disconnect
                    Log.d(TAG, "Bluetooth unplugged");
                    unbindService(mConnection);
                    stopService(new Intent(ControlActivity.this, BluetoothConnectionService.class));
                    Intent broadcast = new Intent(BluetoothUtils.ACTION_HC05_DISCONNECTED);
                    sendBroadcast(broadcast);
                }
                else { //start connecting
                    if (mBluetoothAdapter.isEnabled()) {
                        if (!mScanning) {
                            Log.d(TAG, "Should I scan or should I go now?");
                            mScanStartedByApp = true;
                            mBluetoothAdapter.startDiscovery();
                        }
                    } else {
                        Toast.makeText(ControlActivity.this, "Enable bluetooth before you scan", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Checkbox to use precise number of steps (pixels)
        ((CheckBox)findViewById(R.id.chkbox_precise)).setChecked(false);
        findViewById(R.id.chkbox_precise).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(((CheckBox) v).isChecked()) {
                    findViewById(R.id.input_layout_steps).setEnabled(true);
                    findViewById(R.id.btn_rev_left).setEnabled(false);
                    findViewById(R.id.btn_rev_up).setEnabled(false);
                    findViewById(R.id.btn_rev_right).setEnabled(false);
                    findViewById(R.id.btn_rev_down).setEnabled(false);
                    mUsingPixels = true;
                }
                else {
                    findViewById(R.id.input_layout_steps).setEnabled(false);
                    findViewById(R.id.btn_rev_left).setEnabled(true);
                    findViewById(R.id.btn_rev_up).setEnabled(true);
                    findViewById(R.id.btn_rev_right).setEnabled(true);
                    findViewById(R.id.btn_rev_down).setEnabled(true);
                    mUsingPixels = false;
                    setButtonSteps();
                }
            }
        });


        findViewById(R.id.btn_coord).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableUI(false);
                mBluetoothService.sendInstruction(BluetoothCommands.COMMAND_COORD, BluetoothCommands.VALUE_DOT);
            }
        });

        mFab.setOnClickListener(new View.OnClickListener() {
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

    private void setButtonCommands() {
        ((ControlButton)findViewById(R.id.btn_step_left)).setCommand(BluetoothCommands.COMMAND_LEFT);
        ((ControlButton)findViewById(R.id.btn_step_up)).setCommand(BluetoothCommands.COMMAND_UP);
        ((ControlButton)findViewById(R.id.btn_step_right)).setCommand(BluetoothCommands.COMMAND_RIGHT);
        ((ControlButton)findViewById(R.id.btn_step_down)).setCommand(BluetoothCommands.COMMAND_DOWN);

        ((ControlButton)findViewById(R.id.btn_rev_left)).setCommand(BluetoothCommands.COMMAND_LEFT);
        ((ControlButton)findViewById(R.id.btn_rev_up)).setCommand(BluetoothCommands.COMMAND_UP);
        ((ControlButton)findViewById(R.id.btn_rev_right)).setCommand(BluetoothCommands.COMMAND_RIGHT);
        ((ControlButton)findViewById(R.id.btn_rev_down)).setCommand(BluetoothCommands.COMMAND_DOWN);

        ((ControlButton)findViewById(R.id.btn_dot)).setCommand(BluetoothCommands.COMMAND_DOT);
    }

    private void setButtonSteps() {
        ((ControlButton)findViewById(R.id.btn_step_left)).setSteps(BluetoothCommands.VALUE_LEFT);
        ((ControlButton)findViewById(R.id.btn_step_up)).setSteps(BluetoothCommands.VALUE_UP);
        ((ControlButton)findViewById(R.id.btn_step_right)).setSteps(BluetoothCommands.VALUE_RIGHT);
        ((ControlButton)findViewById(R.id.btn_step_down)).setSteps(BluetoothCommands.VALUE_DOWN);

        ((ControlButton)findViewById(R.id.btn_rev_left)).setSteps(BluetoothCommands.ROTATION_NEMA);
        ((ControlButton)findViewById(R.id.btn_rev_up)).setSteps(BluetoothCommands.ROTATION_BYJ);
        ((ControlButton)findViewById(R.id.btn_rev_right)).setSteps(BluetoothCommands.ROTATION_NEMA);
        ((ControlButton)findViewById(R.id.btn_rev_down)).setSteps(BluetoothCommands.ROTATION_BYJ);

        ((ControlButton)findViewById(R.id.btn_dot)).setSteps(0);
    }

    private void setButtonListeners() {
        (findViewById(R.id.btn_step_left)).setOnClickListener(new ControlButtonClickListener());
        (findViewById(R.id.btn_step_up)).setOnClickListener(new ControlButtonClickListener());
        (findViewById(R.id.btn_step_right)).setOnClickListener(new ControlButtonClickListener());
        (findViewById(R.id.btn_step_down)).setOnClickListener(new ControlButtonClickListener());

        (findViewById(R.id.btn_rev_left)).setOnClickListener(new ControlButtonClickListener());
        (findViewById(R.id.btn_rev_up)).setOnClickListener(new ControlButtonClickListener());
        (findViewById(R.id.btn_rev_right)).setOnClickListener(new ControlButtonClickListener());
        (findViewById(R.id.btn_rev_down)).setOnClickListener(new ControlButtonClickListener());

        (findViewById(R.id.btn_dot)).setOnClickListener(new ControlButtonClickListener());

        // TODO fix
        mInputX.addTextChangedListener(new MyTextWatcher());
        mInputY.addTextChangedListener(new MyTextWatcher());
    }

    private void setButtonColors() {
        mFab.setBackgroundTintList(getResources().getColorStateList(R.color.colors_enable, getTheme()));
        findViewById(R.id.btn_coord).setBackgroundTintList(getResources().getColorStateList(R.color.colors_enable, getTheme()));
    }

    @Override
    public void onInstructionExecuted(int commandCode, byte[] response) {
        mIsExecuting = false;

        if(commandCode == BluetoothCommands.COMMAND_COORD) {
            // TODO parse correctly
            mCoordX = 0;
            mCoordY = 0;
            mCoordX += (((int)response[0]) << 8);
            mCoordX += response[1] & 0b11111111;
            mCoordY += (((int)response[2]) << 8);
            mCoordY += response[3] & 0b11111111;
            mCoordY = (int) Math.round((double)mCoordY / BluetoothCommands.VALUE_DOWN);
            Log.d(TAG, mCoordX + ", " + mCoordY);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mInputX.setText(String.valueOf(mCoordX));
                    mInputY.setText(String.valueOf(mCoordY));
                    if(mCoordX < 0 && mCoordY > 0) {
                        mFab.setEnabled(true);
                    }
                    enableUI(true);
                }
            });

        }
        else {
            // ignore, it's only 0s
        }
    }

    /* Listens to bluetooth turn on/off */
    BroadcastReceiver mBluetoothStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothStateChangeReceiver.ACTION_BLUETOOTH_STARTED: {
                        Log.d(TAG, "You start me up");
                        findViewById(R.id.btn_connect).setEnabled(true);
                        break;
                    }
                    case BluetoothStateChangeReceiver.ACTION_BLUETOOTH_STOPPED: {
                        Log.d(TAG, "Don't stop me now!");
                        if(mConnected) {
                            unbindService(mConnection);
                        }
                        stopService(new Intent(ControlActivity.this, BluetoothConnectionService.class));
                        enableUI(false);
                        findViewById(R.id.btn_connect).setEnabled(false);
                        break;
                    }
                }
            }
        }
    };

    /* Intercepts when phone connects and disconnects from the plotter */
    BroadcastReceiver mConnectionStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothUtils.ACTION_HC05_CONNECTED: {
                    mConnected = true;
                    enableUI(true);
                    ControlActivity.this.findViewById(R.id.overlay).setVisibility(View.GONE);
                    findViewById(R.id.btn_connect).setActivated(true);
                    break;
                }
                case BluetoothUtils.ACTION_HC05_DISCONNECTED: {
                    mConnected = false;
                    enableUI(false);
                    findViewById(R.id.btn_connect).setActivated(false);
                    break;
                }
            }
        }
    };

    /* Listens to bluetooth scanning and finding devices */
    BroadcastReceiver mDeviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothDevice.ACTION_FOUND: { // Device found
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device.getAddress().equals(BluetoothUtils.HC05_MAC_ADDRESS)) {
                            Log.d(TAG, "Found HC-05!");
                            mHc05device = device;
                            connectToHc05();
                        }
                        break;
                    }
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED: { // Scan started
                        /* I want to connect to the module only if I clicked the icon,
                         * not when the user turns on the bluetooth (automatic discovery starts)
                         */
                        if(mScanStartedByApp) {
                            Log.d(TAG, "Scan started");
                            mScanning = true;
                            findViewById(R.id.overlay).setVisibility(View.VISIBLE);
                        }
                        else {
                            mBluetoothAdapter.cancelDiscovery();
                        }
                        break;
                    }
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: { // Scan stopped
                        Log.d(TAG, "Scan stopped");
                        /* Two cases - search stopped with/without finding the device:
                            If found, it automatically connects, no need to do anything here;
                            If not found - remove UI loading indicator;
                         */
                        mScanning = false;
                        mScanStartedByApp = false;
                        findViewById(R.id.overlay).setVisibility(View.GONE);
                        break;
                    }
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED: { /* A device paired/unpaired */ }
                }
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothConnectionService.LocalBinder binder = (BluetoothConnectionService.LocalBinder) service;
            mBluetoothService = binder.getService();
            mBluetoothService.setBoundListener(ControlActivity.this);
            Log.d(TAG, "bound to service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "service disconnected");
        }
    };

    public class ControlButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            if (!mBluetoothService.isChannelOpen()) {
                return;
            }

            int steps;
            if (mUsingPixels) {
                EditText input = findViewById(R.id.input_steps);

                if (input.getText() == null || input.getText().toString().trim().equals("")) {
                    input.setError("Set number between 0 and 32767");
                    return;
                }

                try {
                    steps = Integer.parseInt(input.getText().toString());
                } catch (NumberFormatException e) {
                    input.setError("Set number between 0 and 32767");
                    return;
                }
                if (steps < 0 || steps > 32767) {
                    input.setError("Set number between 0 and 32767");
                    return;
                }
            }
            else {
                steps = ((ControlButton) v).getSteps();
            }
            mBluetoothService.sendInstruction(((ControlButton) v ).getCommand(), steps);
        }
    }

    public class MyTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void afterTextChanged(Editable s) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            try {
                int x = Integer.parseInt(mInputX.getText().toString());
                int y = Integer.parseInt(mInputY.getText().toString());

                if(x > 0 && y > 0 && x < 5000 && y < 5000) {
                    mFab.setEnabled(true);
                }
                else {
                    mFab.setEnabled(false);
                }
            } catch(NumberFormatException e) {
                mFab.setEnabled(false);
            }
        }
    }
}
