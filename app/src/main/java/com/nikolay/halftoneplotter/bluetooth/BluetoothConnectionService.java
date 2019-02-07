package com.nikolay.halftoneplotter.bluetooth;

import android.app.IntentService;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class BluetoothConnectionService extends IntentService {

    private IBinder mBinder = new LocalBinder();
    private BluetoothSocket mBluetoothSocket;
    private BluetoothResponseListener mBoundActivity;

    public BluetoothConnectionService() {
        super("BluetoothConnectionService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // TODO
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public BluetoothConnectionService getService() {
            return BluetoothConnectionService.this;
        }
    }

    public void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
        this.mBluetoothSocket = bluetoothSocket;
    }

    public void setBoundActivity(BluetoothResponseListener boundActivity) {
        this.mBoundActivity = boundActivity;
    }
}
