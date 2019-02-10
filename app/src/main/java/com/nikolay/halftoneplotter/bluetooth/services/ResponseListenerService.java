package com.nikolay.halftoneplotter.bluetooth.services;

import android.app.IntentService;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.nikolay.halftoneplotter.bluetooth.SocketContainer;

public class ResponseListenerService extends IntentService {

    private static final String TAG = "Lisko: " + ResponseListenerService.class.getName();

    private IBinder mBinder = new LocalBinder();
    private BluetoothSocket mBluetoothSocket;

    public ResponseListenerService() {
        super("ResponseListenerService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mBluetoothSocket = SocketContainer.getBluetoothSocket();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // TODO start other service

    }

    public class LocalBinder extends Binder {
        public ResponseListenerService getService() {
            return ResponseListenerService.this;
        }
    }
}
