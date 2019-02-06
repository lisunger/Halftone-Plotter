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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.nikolay.halftoneplotter.TestService;
import com.nikolay.halftoneplotter.bluetooth.BluetoothStateChangeReceiver;
import com.nikolay.halftoneplotter.bluetooth.BluetoothUtils;
import com.nikolay.halftoneplotter.components.ControlButton;
import com.nikolay.halftoneplotter.R;
import com.nikolay.halftoneplotter.bluetooth.BluetoothCommands;

import java.io.IOException;

public class ControlActivity extends AppCompatActivity {

    private static final String TAG = "Lisko";

    private Integer mStepsX;
    private Integer mStepsY;
    private int mCoordX;
    private int mCoordY;
    private boolean mUsingSteps;

    private boolean mScanning = false;
    private boolean mScanStartedByApp = false;
    private boolean mConnected = false;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket = null;

    private BluetoothDevice mHc05device = null;
    private TestService mTestService = null;


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

        Intent intent = new Intent(this, TestService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    protected void onResume() {
        super.onResume();
        // TODO check the UI, connection etc.
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
        Log.d("Lisko", "ControlActivity destroyed");
        unregisterReceiver(mBluetoothStateBroadcastReceiver);
        unregisterReceiver(mConnectionStateReceiver);
        unregisterReceiver(mDeviceFoundReceiver);
    }

    private void connectToHc05() {
        mBluetoothAdapter.cancelDiscovery();

        try {
            mBluetoothSocket = mHc05device.createRfcommSocketToServiceRecord(BluetoothUtils.PLOTTER_UUID);
            //mService.setBluetoothSocket(mBluetoothSocket);
            //Intent intent = new Intent(this, StartConnectionService.class);
            //startService(intent);
            //bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        } catch (IOException e) {
            Log.d(TAG, "Cannot open socket.");
            e.printStackTrace();
            mConnected = false;
        }
    }

    private void setUpUi() {
        setButtonsSteps();
        setButtonColors();
        enableUI(false);
        setClickListeners();

        findViewById(R.id.btn_test).setEnabled(false);
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
        findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mConnected) { //disconnect
                    // TODO disconnect
                    Log.d(TAG, "Bluetooth unplugged");
//                    this.unbindService(mConnection);
//                    stopService(new Intent(this, StartConnectionService.class));
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
        // findViewById(R.id.btn_coord).setOnClickListener(...);

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


        findViewById(R.id.btn_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mTestService.logNumber((int)(Math.random() * 100));
                unbindService(mConnection);
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

    /* Listens to bluetooth turn on/off */
    BroadcastReceiver mBluetoothStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothStateChangeReceiver.ACTION_BLUETOOTH_STARTED: {
                        Log.d(TAG, "You start me up");
                        break;
                    }
                    case BluetoothStateChangeReceiver.ACTION_BLUETOOTH_STOPPED: {
                        Log.d(TAG, "Don't stop me now!");
                        // TODO stop service, disable button ...
                        // stopService(new Intent(context, StartConnectionService.class));
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
                    // TODO enable UI controls
                    // TODO remove load indicator
                    break;
                }
                case BluetoothUtils.ACTION_HC05_DISCONNECTED: {
                    mConnected = false;
                    // TODO disable UI controls
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
                            // TODO enable UI loading indicator
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
            TestService.LocalBinder binder = (TestService.LocalBinder) service;
            mTestService = binder.getService();
            Log.d(TAG, "bound to service");
            findViewById(R.id.btn_test).setEnabled(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "service disconnected");
        }
    };
}
