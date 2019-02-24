package com.nikolay.halftoneplotter.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nikolay.halftoneplotter.R;
import com.nikolay.halftoneplotter.utils.Utils;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PERMISSION_READ_STORAGE = 123;
    private static final int RESULT_PICK = 234;

    private TextView mTextViewChooseImage;
    private ImageView mImageView;
    private TextView mTextViewImageDetails;
    private FloatingActionButton mFab;
    private int mMenu = R.menu.menu_empty;

    private Uri mImageUri = null;
    private Bitmap mBitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO !!!check if the drawing service is running and switch to drawing activity!!!

        setContentView(R.layout.activity_main);

        requestStoragePermission();

        setUpToolbars();
        loadUiElements();
        loadImageIfAvailable();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(mMenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        switch(menuItem.getItemId()) {
            case R.id.nav_open : {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, RESULT_PICK);
                break;
            }
            case R.id.nav_gallery : {
                // TODO
                break;
            }
            case R.id.nav_exit : {
                // TODO
                break;
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case RESULT_PICK : {
                if(resultCode == RESULT_OK && data != null) {
                    mImageUri = data.getData();

                    //String path = Utils.getPathFromURI(this, imageUri);
                    //BitmapFactory.Options options = new BitmapFactory.Options();
                    //options.inJustDecodeBounds = true;
                    //BitmapFactory.decodeFile(path, options);
                    loadAndSetImage();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode) {
            case PERMISSION_READ_STORAGE : {
                if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "You must grant external storage permission", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    private void requestStoragePermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_STORAGE);
        }
    }

    private void setUpToolbars() {
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                drawer,
                toolbar,
                R.string.nav_open_drawer,
                R.string.nav_close_drawer);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void loadUiElements() {
        mTextViewChooseImage = findViewById(R.id.textLoad);
        mImageView = findViewById(R.id.image);
        mTextViewImageDetails = findViewById(R.id.textImageDetails);
        mFab = findViewById(R.id.fab);
        mFab.setBackgroundTintList(getResources().getColorStateList(R.color.colors_enable, getTheme()));
        mFab.setEnabled(false);

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = MainActivity.this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.image_uri_key), mImageUri.toString());
                editor.apply();

                Intent intent = new Intent(MainActivity.this, ControlActivity.class);
                intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        });
    }

    private void loadImageIfAvailable() {
        if(mImageUri != null) {
            loadAndSetImage();
        }
    }

    private void loadAndSetImage() {
        mBitmap = Utils.decodeImageFromUri(this, mImageUri, true);
        if(mBitmap != null) {

            int[] imageSize = Utils.getImageSizeFromUri(this, mImageUri);

            mImageView.setImageBitmap(mBitmap);
            mImageView.setVisibility(View.VISIBLE);
            mTextViewChooseImage.setVisibility(View.GONE);

            String details = String.format("%dx%d", imageSize[0], imageSize[1]);
            mTextViewImageDetails.setText(details);
            mTextViewImageDetails.setVisibility(View.VISIBLE);

            mFab.setEnabled(true);
        }
    }
}
