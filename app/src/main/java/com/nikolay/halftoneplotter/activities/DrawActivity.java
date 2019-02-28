package com.nikolay.halftoneplotter.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nikolay.halftoneplotter.R;
import com.nikolay.halftoneplotter.bluetooth.services.BluetoothConnectionService;
import com.nikolay.halftoneplotter.utils.Utils;

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
        mButtonPauseResume.setText(R.string.lbl_start);
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

        mButtonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDrawService != null) {
                    mDrawService.setStop(true);
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
                mButtonStop.setEnabled(true);
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

    @Override
    public void onLostConnection() {
        // TODO
        Toast.makeText(this, "Lost connection with plotter.", Toast.LENGTH_LONG).show();
        unbindService(mConnection);

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothConnectionService.LocalBinder binder = (BluetoothConnectionService.LocalBinder) service;
            mDrawService = binder.getService();
            mDrawService.setDrawListener(DrawActivity.this);

            mConnected = true;

            mButtonPauseResume.setEnabled(true);
            Log.d(TAG, "bound to service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mConnected = false;
            Log.d(TAG, "service disconnected");
        }
    };
}
