package com.nikolay.halftoneplotter.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import java.util.UUID;

public class BluetoothUtils {

    public static final int REQUEST_PERMISSION_LOCATION = 1;
    public static final int REQUEST_ENABLE_BT = 2;
    public static final String ACTION_HC05_CONNECTED =      "com.nikolay.plottercontroller.action.CONNECTED";
    public static final String ACTION_HC05_DISCONNECTED =   "com.nikolay.plottercontroller.action.DISCONNECTED";
    public static final String HC05_MAC_ADDRESS = "00:18:E4:00:78:F9";
    public static final UUID PLOTTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    public static void requestLocationPermission(Activity context) {
        context.requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_PERMISSION_LOCATION);
    }

    public static void registerBluetoothStateReceiver(Context context, BroadcastReceiver broadcastReceiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothStateChangeReceiver.ACTION_BLUETOOTH_STARTED);
        intentFilter.addAction(BluetoothStateChangeReceiver.ACTION_BLUETOOTH_STOPPED);
        context.registerReceiver(broadcastReceiver, intentFilter);
    }

    public static void registerConnectionStateReceiver(Context context, BroadcastReceiver broadcastReceiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_HC05_CONNECTED);
        intentFilter.addAction(ACTION_HC05_DISCONNECTED);
        context.registerReceiver(broadcastReceiver, intentFilter);
    }

    public static void registerBluetoothDeviceReceiver(Context context, BroadcastReceiver broadcastReceiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(broadcastReceiver, intentFilter);
    }
}
