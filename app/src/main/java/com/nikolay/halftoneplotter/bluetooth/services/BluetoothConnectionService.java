package com.nikolay.halftoneplotter.bluetooth.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.nikolay.halftoneplotter.R;
import com.nikolay.halftoneplotter.activities.ControlActivity;
import com.nikolay.halftoneplotter.bluetooth.BluetoothCommands;
import com.nikolay.halftoneplotter.bluetooth.BluetoothUtils;
import com.nikolay.halftoneplotter.bluetooth.SocketContainer;
import com.nikolay.halftoneplotter.utils.Instruction;
import com.nikolay.halftoneplotter.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class BluetoothConnectionService extends IntentService {

    private static final String TAG = "Lisko: " + BluetoothConnectionService.class.getName();

    private IBinder mBinder = new LocalBinder();
    private BluetoothSocket mBluetoothSocket;
    private BluetoothResponseListener mControlListener;
    private DrawListener mDrawListener;
    private boolean mIsChannelOpen = true;
    private boolean mStarted = false;
    private boolean  mPaused = false;
    private final int mDelay = 100;
    private final int mLongDelay = 1000;
    private int mCurrentCommandCode = -1;
    private Uri mImageUri;
    int[] mImageSize;
    private boolean mCalledHello = false;
    private boolean mHelloResponded = false;

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
        Log.d(TAG, "onHandleIntent thread: " + Thread.currentThread().getName());
        connectToHc05();

        InputStream readStream;
        try {
            readStream = mBluetoothSocket.getInputStream();
        } catch (IOException e) {
            Log.d(TAG, "Could not open input stream");
            mStarted = false;
            stopSelf();
            e.printStackTrace();
            return;
        }

        // Do nothing while connection is active
        while(mBluetoothSocket.isConnected()) {
            try {
                while(isPaused()) {
                    Thread.sleep(mLongDelay);
                }

                while(readStream.available() < 4) {
                    Thread.sleep(mDelay);
                }
                byte[] response = new byte[4];
                readStream.read(response, 0, 4);
                if(mCalledHello) {
                    mHelloResponded = true;
                }

                if(mControlListener != null) {
                    mControlListener.onInstructionExecuted(mCurrentCommandCode, response);
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
        Utils.clearPreferences(this);
        Intent broadcast = new Intent(BluetoothUtils.ACTION_HC05_DISCONNECTED);
        sendBroadcast(broadcast);
        Log.d(TAG, "StartConnectionService destroyed");
        Toast.makeText(this, "StartConnectionService destroyed", Toast.LENGTH_SHORT).show();
    }

    public void goForeground() {
        startForeground(1, buildForegroundNotification());
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

    public void startDrawing() {
        mStarted = true;
        goForeground();
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        mImageUri = Uri.parse(sharedPref.getString(getString(R.string.image_uri_key), null));
        mImageSize = Utils.getImageSizeFromUri(this, mImageUri);
        if(mDrawListener != null) {
            mDrawListener.onImageLoaded(mImageUri, mImageSize[0], mImageSize[1]);
        }

        List<Instruction> sequence = Utils.convertImageToInstructions(this, mImageUri, false);
        Log.d(TAG, "Loaded sequence size: " + sequence.size());
        if(mDrawListener != null) {
            mDrawListener.onSequenceLoaded(sequence.size());
        }

        try {
            while(!isChannelOpen()) {
                Thread.sleep(mDelay);
            }
            goToCoords();

            while(!isChannelOpen()) {
                Thread.sleep(mDelay);
            }
            executeSequence(sequence);
        } catch(InterruptedException e) {
            Log.d(TAG, "Bluetooth connection error");
            stopSelf();
            e.printStackTrace();
        }

        stopForeground(true);
        stopSelf();
    }

    public void sendInstruction(int command, int value) {
        Log.d(TAG, "sendInstruction thread: " + Thread.currentThread().getName());
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

    private void goToCoords() {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        int x = sharedPref.getInt(getString(R.string.coordinate_x_key), -1);
        int y = sharedPref.getInt(getString(R.string.coordinate_y_key), -1);

        int value = 0;
        value |= x << 16;
        value |= y;

        sendInstruction(BluetoothCommands.COMMAND_GOTO, value);
    }

    private void executeSequence(List<Instruction> sequence) {

        if(mDrawListener != null) {
            mDrawListener.onDrawStarted();
        }

        int rowIndex = 0;
        for (int index = 0; index < sequence.size(); index++) {

            try {
                while(isPaused()) {
                    Thread.sleep(mLongDelay);
                }
                while (!isChannelOpen()) {
                    Thread.sleep(mDelay);
                }

                if(index - 1 >= 0 && sequence.get(index - 1).getCommand() == BluetoothCommands.COMMAND_UP) {
                    if (mDrawListener != null) {
                        mDrawListener.onRowCompleted(rowIndex);
                    }
                    rowIndex++;
                }

                sendInstruction(sequence.get(index).getCommand(), sequence.get(index).getValue());

            } catch (InterruptedException e) {
                Log.d(TAG, "Bluetooth connection error");
                mStarted = false;
                stopSelf();
                e.printStackTrace();
            }
        }
    }

    private boolean callHello() {
        mCalledHello = true;

        for(int i = 0; i < 10; i++) {
            if(mHelloResponded) {
                mCalledHello = false;
                mHelloResponded = false;
                return true;
            }
            try {
                Thread.sleep(mLongDelay);
            } catch(InterruptedException e) {
                Log.d(TAG, "Bluetooth connection error");
                e.printStackTrace();
                stopSelf();
            }
        }
        return false;
    }

    public void pauseDrawing() {
        setPaused(true);

        if (mDrawListener != null) {
            mDrawListener.onDrawPaused();
        }
    }

    public void resumeDrawing() {
        if(callHello()) {
            setPaused(false);
        }
        else {
            Log.d(TAG, getString(R.string.lbl_not_responding));
            if(mDrawListener != null) {
                mDrawListener.onLostConnection();
            }
            stopForeground(true);
            stopSelf();
        }

        if (mDrawListener != null) {
            mDrawListener.onDrawResumed();
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

    public void setControlListener(BluetoothResponseListener controlListener) {
        this.mControlListener = controlListener;
    }

    public void setDrawListener(DrawListener drawListener) {
        this.mDrawListener = drawListener;
    }

    public boolean isChannelOpen() {
        return mIsChannelOpen;
    }

    public boolean isStarted() {
        return mStarted;
    }

    public synchronized boolean isPaused() {
        return mPaused;
    }

    public synchronized void setPaused(boolean paused) {
        this.mPaused = paused;
    }

    public interface BluetoothResponseListener {
        void onInstructionExecuted(int commandCode, byte[] response);
    }

    public interface DrawListener {
        void onDrawStarted();
        void onDrawPaused();
        void onDrawResumed();
        void onRowCompleted(int rowIndex);
        void onImageLoaded(Uri uri, int width, int height);
        void onSequenceLoaded(int sequenceSize);
        void onLostConnection();
    }
}
