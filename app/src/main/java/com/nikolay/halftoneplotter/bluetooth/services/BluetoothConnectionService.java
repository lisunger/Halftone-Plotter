package com.nikolay.halftoneplotter.bluetooth.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.nikolay.halftoneplotter.R;
import com.nikolay.halftoneplotter.activities.ControlActivity;
import com.nikolay.halftoneplotter.bluetooth.BluetoothUtils;
import com.nikolay.halftoneplotter.bluetooth.SocketContainer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothConnectionService extends IntentService {

    private static final String TAG = "Lisko: " + BluetoothConnectionService.class.getName();

    private IBinder mBinder = new LocalBinder();
    private BluetoothSocket mBluetoothSocket;
    private BluetoothResponseListener mBoundListener;
    private boolean mIsChannelOpen = true;
    private final int mDelay = 100;
    private int mCurrentCommandCode = -1;

    public BluetoothConnectionService() {
        super("BluetoothConnectionService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mBluetoothSocket = SocketContainer.getBluetoothSocket();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        connectToHc05();

        InputStream readStream;
        try {
            readStream = mBluetoothSocket.getInputStream();
        } catch (IOException e) {
            Log.d(TAG, "Could not open input stream");
            stopSelf();
            e.printStackTrace();
            return;
        }
        // Do nothing while connection is active
        while(mBluetoothSocket.isConnected()) {
            try {
                while(readStream.available() < 4) {
                    Thread.sleep(mDelay);
                }
                byte[] response = new byte[4];
                readStream.read(response, 0, 4);
                if(mBoundListener != null) {
                    mBoundListener.onInstructionExecuted(mCurrentCommandCode, response);
                }
                mIsChannelOpen = true;

            } catch (IOException e) {
                Log.d(TAG, "Could not open input stream");
                stopSelf();
                e.printStackTrace();
            } catch (InterruptedException e) {
                Log.d(TAG, "Bluetooth connection error");
                stopSelf();
                e.printStackTrace();
            }
        }
    }

    private void connectToHc05() {
        try {
            if(mBluetoothSocket != null) {
                mBluetoothSocket.connect();
                Log.d(TAG, "Connection successful");
                // Send broadcast that the connection is established
                Intent broadcast = new Intent(BluetoothUtils.ACTION_HC05_CONNECTED);
                sendBroadcast(broadcast);
                //startForeground(1, buildForegroundNotification());
            }
        } catch (IOException e) {
            Log.d(TAG, "Connection failed");
            e.printStackTrace();
            stopSelf();
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

    public void sendInstruction(int command, int value) {
        if(mIsChannelOpen) {
            try {
                mIsChannelOpen = false;
                OutputStream writeStream = mBluetoothSocket.getOutputStream();
                mCurrentCommandCode = command;

                // instruction beginning (four bytes)
                writeStream.write(new byte[]{'n', 'i', 'k', 'i'});

                // command (one byte)
                writeStream.write(Integer.valueOf(command).byteValue());

                // value (four bytes)
                writeStream.write((value >> 24) & 0b11111111);
                writeStream.write((value >> 16) & 0b11111111);
                writeStream.write((value >> 8) & 0b11111111);
                writeStream.write(value & 0b11111111);

            } catch (IOException e) {
                Log.d(TAG, "Could not send command");
                // TODO scan if connected device is no longer there (powered off)
                e.printStackTrace();
                mIsChannelOpen = true;
            }
        }
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

    public void setBoundListener(BluetoothResponseListener boundListener) {
        this.mBoundListener = boundListener;
    }

    public boolean isChannelOpen() {
        return mIsChannelOpen;
    }

    public interface BluetoothResponseListener {
        void onInstructionExecuted(int commandCode, byte[] response);
    }
}
