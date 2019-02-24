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
import com.nikolay.halftoneplotter.activities.DrawActivity;
import com.nikolay.halftoneplotter.bluetooth.BluetoothCommands;
import com.nikolay.halftoneplotter.bluetooth.BluetoothUtils;
import com.nikolay.halftoneplotter.bluetooth.SocketContainer;
import com.nikolay.halftoneplotter.utils.Instruction;
import com.nikolay.halftoneplotter.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

public class DrawImageService extends IntentService {

    private static final String TAG = "Lisko: " + DrawImageService.class.getName();

    private IBinder mBinder = new LocalBinder();
    private BluetoothSocket mBluetoothSocket;
    private DrawListener mBoundListener;
    private Uri mImageUri;
    private boolean mPaused = false;
    private boolean mStopListening = false;
    private final int mDelay = 100;
    int[] mImageSize;

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
        mImageUri = Uri.parse(sharedPref.getString(getString(R.string.image_uri_key), null));
        mImageSize = Utils.getImageSizeFromUri(this, mImageUri);
        if(mBoundListener != null) {
            mBoundListener.onImageLoaded();
        }
        List<Instruction> sequence = Utils.convertImageToInstructions(this, mImageUri, false);
        Log.d(TAG, "Loaded sequence size: " + sequence.size());
        if(mBoundListener != null) {
            mBoundListener.onSequenceLoaded(sequence.size());
        }
        connectToHc05();
        goToCoords();
        executeSequence(sequence);

        stopSelf();
    }

    private void connectToHc05() {
        try {
            if (mBluetoothSocket != null) {
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

    private void goToCoords() {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        int x = sharedPref.getInt(getString(R.string.coordinate_x_key), -1);
        int y = sharedPref.getInt(getString(R.string.coordinate_y_key), -1);

        InputStream readStream = getBluetoothStream();

        int value = 0;
        value |= x << 16;
        value |= y;

        sendInstruction(BluetoothCommands.COMMAND_GOTO, value);

        try {
            while (readStream.available() < 4 && !mStopListening) {
                Thread.sleep(mDelay);
            }
            for (int i = 0; i < 4; i++) {
                readStream.read();
            }
            readStream.close();

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

    private void executeSequence(List<Instruction> sequence) {

        if(mBoundListener != null) {
            mBoundListener.onDrawStarted();
        }

        InputStream readStream = getBluetoothStream();

        int rowIndex = 0;
        for (int index = 0; index < sequence.size(); index++) {

            sendInstruction(sequence.get(index).getCommand(), sequence.get(index).getValue());

            try {
                while ((readStream.available() < 4 && !mStopListening) || mPaused) {
                    Thread.sleep(mDelay);
                }

                // read all four bytes to free the stream
                for (int i = 0; i < 4; i++) {
                    readStream.read();
                }

                if (mBoundListener != null) {
                    if (sequence.get(index).getCommand() == BluetoothCommands.COMMAND_UP)
                        mBoundListener.onRowCompleted(rowIndex);
                    rowIndex++;
                }

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

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mBluetoothSocket != null) {
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
        try {
            OutputStream writeStream = mBluetoothSocket.getOutputStream();

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
        }
    }

    private InputStream getBluetoothStream() {
        try {
            return mBluetoothSocket.getInputStream();
        } catch (IOException e) {
            Log.d(TAG, "Could not open input stream");
            stopSelf();
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void pauseDrawing() {
        mPaused = true;

        if (mBoundListener != null) {
            mBoundListener.onDrawPaused();
        }
    }

    public void resumeDrawing() {
        mPaused = false;

        if (mBoundListener != null) {
            mBoundListener.onDrawResumed();
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

    public int getImageWidth() {
        return mImageSize[0];
    }

    public int getImageHeight() {
        return mImageSize[1];
    }

    public void setStopListening(boolean mStopListening) {
        this.mStopListening = mStopListening;
    }

    public Uri getImageUri() {
        return mImageUri;
    }

    public interface DrawListener {
        void onDrawStarted();
        void onDrawPaused();
        void onDrawResumed();
        void onRowCompleted(int rowIndex);
        void onImageLoaded();
        void onSequenceLoaded(int sequenceSize);
    }

}
