package com.nikolay.halftoneplotter.bluetooth;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.nikolay.halftoneplotter.R;
import com.nikolay.halftoneplotter.activities.ControlActivity;

import java.io.IOException;

public class BluetoothConnectionService extends IntentService {

    private static final String TAG = "Lisko: " + BluetoothConnectionService.class.getName();

    private IBinder mBinder = new LocalBinder();
    private BluetoothSocket mBluetoothSocket;
    private BluetoothResponseListener mBoundActivity;

    public BluetoothConnectionService() {
        super("BluetoothConnectionService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    public void connectToHc05() {
        try {
            if(mBluetoothSocket != null) {
                mBluetoothSocket.connect();
                Log.d(TAG, "Connection successful");
                // Send broadcast that the connection is established
                Intent broadcast = new Intent(BluetoothUtils.ACTION_HC05_CONNECTED);
                sendBroadcast(broadcast);
                startForeground(1, buildForegroundNotification());
            }
        } catch (IOException e) {
            Log.d(TAG, "Connection failed");
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mBluetoothSocket != null) {
            try {
                mBluetoothSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Cannot close connection");
                e.printStackTrace();
            }
        }
        Intent broadcast = new Intent(BluetoothUtils.ACTION_HC05_DISCONNECTED);
        sendBroadcast(broadcast);
        Log.d(TAG, "StartConnectionService destroyed");
        Toast.makeText(this, "StartConnectionService destroyed", Toast.LENGTH_SHORT).show();
    }

    private Notification buildForegroundNotification() {
        Intent notificationIntent = new Intent(this, ControlActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Connected to Plotter")
                .setContentText("Hello")
                .setSmallIcon(R.drawable.fountain_pen_tip)
                .setContentIntent(pendingIntent)
                .setTicker("Ticker text")
                .build();
        return notification;
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
