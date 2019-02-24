package com.nikolay.halftoneplotter.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nikolay.halftoneplotter.R;
import com.nikolay.halftoneplotter.bluetooth.BluetoothStateChangeReceiver;
import com.nikolay.halftoneplotter.bluetooth.BluetoothUtils;
import com.nikolay.halftoneplotter.bluetooth.SocketContainer;
import com.nikolay.halftoneplotter.bluetooth.services.BluetoothConnectionService;
import com.nikolay.halftoneplotter.bluetooth.services.DrawImageService;
import com.nikolay.halftoneplotter.utils.Utils;

public class DrawActivity extends AppCompatActivity implements DrawImageService.DrawListener {

    private static final String TAG = "Lisko: " + DrawActivity.class.getName();

    private TextView mTextBox;
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private TextView mPercentView;
    private Button mButtonStartPause;
    private Button mButtonStop;

    private boolean mExecuting = false;
    private boolean mConnected = false;
    private DrawImageService mDrawService = null;
    private int mRows;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        initUi();

        // if yes, bind to it and get all data, while it keeps drawing
        // if no, start the service and bind to it
        if(Utils.isServiceRunning(this, DrawImageService.class)) {
            Intent intent = new Intent(this, DrawImageService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
        else {
            Intent intent = new Intent(this, DrawImageService.class);
            startService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        BluetoothUtils.registerBluetoothStateReceiver(this, mBluetoothStateBroadcastReceiver);
        BluetoothUtils.registerConnectionStateReceiver(this, mConnectionStateReceiver);
    }

    private void initUi() {
        mTextBox = findViewById(R.id.textView);
        mImageView = findViewById(R.id.imageDraw);
        mProgressBar = findViewById(R.id.progressBar);
        mPercentView = findViewById(R.id.textPercent);
        mButtonStartPause = findViewById(R.id.buttonStartPause);
        mButtonStop = findViewById(R.id.buttonStop);

        mButtonStartPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if sequence is executing, pause it and vice versa
                if(mExecuting) {
                    mExecuting = false;
                    mDrawService.pauseDrawing();
                    ((Button) findViewById(R.id.buttonStartPause)).setText(getString(R.string.lbl_resume));
                }
                else {
                    mExecuting = true;
                    mDrawService.resumeDrawing();
                    ((Button) findViewById(R.id.buttonStartPause)).setText(getString(R.string.lbl_pause));
                }
            }
        });

        mButtonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExecuting = false;
                stopService(new Intent(DrawActivity.this, DrawImageService.class));
                SocketContainer.setBluetoothSocket(null);
                mTextBox.append("Sequence stopped.\n");
            }
        });

    }

    @Override
    public void onDrawStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mButtonStartPause.setText(R.string.lbl_pause);
                mTextBox.append("Drawing started.\n");
            }
        });
    }

    @Override
    public void onDrawPaused() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextBox.append("Drawing paused.\n");
            }
        });
    }

    @Override
    public void onDrawResumed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextBox.append("Drawing resumed.\n");
            }
        });
    }

    @Override
    public void onRowCompleted(final int rowIndex) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setProgress(rowIndex);
                double progress = (double)rowIndex / mRows;
                mPercentView.setText(String.format("%.2f%%", progress));
            }
        });
    }

    @Override
    public void onImageLoaded() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextBox.append("Image loaded.\n");
            }
        });
    }

    @Override
    public void onSequenceLoaded(final int sequenceSize) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextBox.append("Sequence loaded. Size: " + sequenceSize + "\n");
            }
        });
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DrawImageService.LocalBinder binder = (DrawImageService.LocalBinder) service;
            mDrawService = binder.getService();
            mDrawService.setBoundListener(DrawActivity.this);

            // load data from service
            mImageView.setImageBitmap(Utils.decodeImageFromUri(DrawActivity.this, mDrawService.getImageUri(), true));
            mRows = mDrawService.getImageHeight();

            Log.d(TAG, "bound to service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "service disconnected");
        }
    };

    BroadcastReceiver mConnectionStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothUtils.ACTION_HC05_CONNECTED: {
                    mConnected = true;
                    mTextBox.append("Plotter connected.");
                    break;
                }
                case BluetoothUtils.ACTION_HC05_DISCONNECTED: {
                    mConnected = false;
                    mTextBox.append("Plotter disconnected.");
                    break;
                }
            }
        }
    };

    BroadcastReceiver mBluetoothStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothStateChangeReceiver.ACTION_BLUETOOTH_STOPPED: {
                        Log.d(TAG, "Don't stop me now!");
                        if(mConnected) {
                            unbindService(mConnection);
                            mTextBox.append("Unbinding service.");
                        }
                        stopService(new Intent(DrawActivity.this, BluetoothConnectionService.class));
                        mTextBox.append("Stopping service.");
                    }
                }
            }
        }
    };
}
