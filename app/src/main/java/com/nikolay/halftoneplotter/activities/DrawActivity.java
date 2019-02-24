package com.nikolay.halftoneplotter.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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
import android.widget.Toast;

import com.nikolay.halftoneplotter.R;
import com.nikolay.halftoneplotter.bluetooth.BluetoothStateChangeReceiver;
import com.nikolay.halftoneplotter.bluetooth.BluetoothUtils;
import com.nikolay.halftoneplotter.bluetooth.SocketContainer;
import com.nikolay.halftoneplotter.bluetooth.services.BluetoothConnectionService;
import com.nikolay.halftoneplotter.bluetooth.services.DrawImageService;
import com.nikolay.halftoneplotter.utils.Utils;

import java.io.IOException;

public class DrawActivity extends AppCompatActivity
        implements BluetoothConnectionService.BluetoothResponseListener, BluetoothConnectionService.DrawListener {

    private static final String TAG = "Lisko: " + DrawActivity.class.getName();

    private TextView mTextBox;
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private TextView mPercentView;
    private Button mButtonPauseResume;
    private Button mButtonStop;

    private int mRows;
    private boolean mExecuting = false;
    private boolean mConnected = false;

    private BluetoothConnectionService mDrawService = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        initUi();

        // if true, bind to service and get all data, while it keeps drawing
        // if no, start the service and bind to it
        if(Utils.isServiceRunning(this, BluetoothConnectionService.class)) {
            Intent intent = new Intent(this, BluetoothConnectionService.class);
            bindService(intent, mConnection, BIND_IMPORTANT);
        }
        else {
            // TODO BT connection must be made again and the service must be started
        }

    }

    private void initUi() {
        mTextBox = findViewById(R.id.textView);
        mImageView = findViewById(R.id.imageDraw);
        mProgressBar = findViewById(R.id.progressBar);
        mPercentView = findViewById(R.id.textPercent);
        mButtonPauseResume = findViewById(R.id.buttonPauseResume);
        mButtonStop = findViewById(R.id.buttonStop);

        mProgressBar.setProgress(0);
        mPercentView.setText("0%");
        mButtonPauseResume.setEnabled(false);
        mButtonStop.setEnabled(false);

        mButtonPauseResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDrawService != null) {
                    if(mDrawService.isStarted()) {
                        if(mDrawService.isPaused()) {
                            mDrawService.resumeDrawing();
                        }
                        else {
                            mDrawService.pauseDrawing();
                        }
                    } else {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                mDrawService.startDrawing();
                            }
                        }).start();

                    }
                }
            }
        });
    }

    @Override
    public void onDrawStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mButtonPauseResume.setText(R.string.lbl_pause);
                mTextBox.append("Drawing started.\n");
                mExecuting = true;
            }
        });
    }

    @Override
    public void onDrawPaused() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextBox.append("Drawing paused.\n");
                mButtonPauseResume.setText(R.string.lbl_resume);
                mExecuting = false;
            }
        });
    }

    @Override
    public void onDrawResumed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextBox.append("Drawing resumed.\n");
                mButtonPauseResume.setText(R.string.lbl_pause);
                mExecuting = true;
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
    public void onImageLoaded(final Uri uri, final int width, final int height) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mImageView.setImageBitmap(Utils.decodeImageFromUri(DrawActivity.this, uri, true));
                mRows = height;
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

    @Override
    public void onInstructionExecuted(int commandCode, byte[] response) {
        // TODO do smth
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothConnectionService.LocalBinder binder = (BluetoothConnectionService.LocalBinder) service;
            mDrawService = binder.getService();
            mDrawService.setDrawListener(DrawActivity.this);

            mConnected = true;

            mButtonPauseResume.setEnabled(true);
            mButtonStop.setEnabled(true);
            Log.d(TAG, "bound to service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mConnected = false;
            Log.d(TAG, "service disconnected");
        }
    };
}
