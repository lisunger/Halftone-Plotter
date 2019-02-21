package com.nikolay.halftoneplotter.activities;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.nikolay.halftoneplotter.R;
import com.nikolay.halftoneplotter.bluetooth.services.DrawImageService;

public class DrawActivity extends AppCompatActivity implements DrawImageService.DrawListener {

    private Uri mImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);


        // TODO check if service is running
        // if yes, bind to it and get all data, while it keeps drawing
        // if no, start the service and bind to it
    }

    @Override
    public void onDrawStarted() {
        // TODO
    }

    @Override
    public void onDrawPaused() {
        // TODO
    }

    @Override
    public void onDrawResumed() {
        // TODO
    }

    @Override
    public void onRowCompleted() {
        // TODO
    }
}
