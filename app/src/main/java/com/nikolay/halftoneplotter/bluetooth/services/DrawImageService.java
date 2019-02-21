package com.nikolay.halftoneplotter.bluetooth.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.nikolay.halftoneplotter.R;
import com.nikolay.halftoneplotter.activities.DrawActivity;
import com.nikolay.halftoneplotter.bluetooth.BluetoothUtils;
import com.nikolay.halftoneplotter.bluetooth.SocketContainer;

import java.io.IOException;
import java.io.OutputStream;

public class DrawImageService extends IntentService {

    private static final String TAG = "Lisko: " + DrawImageService.class.getName();

    private IBinder mBinder = new LocalBinder();
    private BluetoothSocket mBluetoothSocket;
    private boolean mIsChannelOpen = true;
    private DrawListener mBoundListener;
    private int mCurrentCommandCode = -1;
    private boolean paused = false;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mBluetoothSocket = SocketContainer.getBluetoothSocket();
    }

    public DrawImageService() {
        super("DrawImageService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String uriString = sharedPref.getString(getString(R.string.image_uri_key), null);
        int x = sharedPref.getInt(getString(R.string.coordinate_x_key), -1);
        int y = sharedPref.getInt(getString(R.string.coordinate_y_key), -1);




        // TODO everything
        connectToHc05();
    }

    private void connectToHc05() {
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
        Intent notificationIntent = new Intent(this, DrawActivity.class);
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

    private void sendInstruction(int command, int value) {
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

    public void pauseDrawing() {
        // TODO implement

        if(mBoundListener != null) {
            mBoundListener.onDrawPaused();
        }
    }

    public void resumeDrawing() {
        // TODO implement

        if(mBoundListener != null) {
            mBoundListener.onDrawResumed();
        }
    }

    private void rowCompleted() {
        if(mBoundListener != null) {
            mBoundListener.onRowCompleted();
        }
    }


    public class LocalBinder extends Binder {
        public DrawImageService getService() {
            return DrawImageService.this;
        }
    }

    public void setBoundListener(DrawListener boundListener) {
        this.mBoundListener = boundListener;
    }

    public interface DrawListener {
        void onDrawStarted();
        void onDrawPaused();
        void onDrawResumed();
        void onRowCompleted();
    }

}
