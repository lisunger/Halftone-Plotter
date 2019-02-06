package com.nikolay.halftoneplotter.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothStateChangeReceiver extends BroadcastReceiver {

    public final static String ACTION_BLUETOOTH_STOPPED =
            "com.nikolay.halftoneplotter.ACTION_BLUETOOTH_STOPPED";
    public final static String ACTION_BLUETOOTH_STARTED =
            "com.nikolay.halftoneplotter.ACTION_BLUETOOTH_STARTED";

    @Override
    public void onReceive(Context context, Intent intent) {

        int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        String intentAction;
        switch(newState) {
            case BluetoothAdapter.STATE_ON : {
                intentAction = ACTION_BLUETOOTH_STARTED;
                Intent bluetoothIntent = new Intent(intentAction);
                context.sendBroadcast(bluetoothIntent);
                break;
            }
            case BluetoothAdapter.STATE_OFF : {
                intentAction = ACTION_BLUETOOTH_STOPPED;
                Intent bluetoothIntent = new Intent(intentAction);
                context.sendBroadcast(bluetoothIntent);
                break;
            }
            case BluetoothAdapter.ERROR : {
                // TODO
                break;
            }
        }

    }
}
