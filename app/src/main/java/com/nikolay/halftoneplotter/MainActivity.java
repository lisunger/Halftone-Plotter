package com.nikolay.halftoneplotter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.content.PermissionChecker;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PERMISSION_READ_STORAGE = 123;
    private static final int RESULT_PICK = 234;

    private TextView mTextViewChooseImage;
    private ImageView mImageView;
    private TextView mTextViewImageDetails;
    private int mMenu = R.menu.menu_empty;

    private Bitmap mBitmapOriginal = null;
    private Bitmap mBitmapEdited = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestStoragePermission();

        setUpToolbars();
        loadUiElements();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(mMenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_dither_fs : {
                return true;
            }
            case R.id.action_dither_jjn : {
                return true;
            }
            case R.id.action_dither_at : {
                return true;
            }
            case R.id.action_dither_st : {
                return true;
            }
            case R.id.action_dither_grayscale : {
                mBitmapEdited = Dither.grayscale(mBitmapOriginal);
                mImageView.setImageBitmap(mBitmapEdited);
                return true;
            }
            case R.id.action_dither_original : {
                mImageView.setImageBitmap(mBitmapOriginal);
                return true;
            }
            default : {
                return super.onOptionsItemSelected(item);
            }
        }
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
                    Uri imageUri = data.getData();
                    try {
                        /* How to get a piece of the image */
//                        String path = getPathFromURI(imageUri);
//                        BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(path, false);
//                        mBitmapOriginal = decoder.decodeRegion(new Rect(200, 200, 450, 700), null);


                        /* How to rotate the image */
//                        Matrix matrix = new Matrix();
//                        matrix.postRotate(90);
//                        Bitmap rotatedBitmap = Bitmap.createBitmap(mBitmapOriginal, 0, 0, mBitmapOriginal.getWidth(), mBitmapOriginal.getHeight(), matrix, true);


                        mBitmapOriginal = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        mImageView.setImageBitmap(mBitmapOriginal);
                        mImageView.setVisibility(View.VISIBLE);
                        mTextViewChooseImage.setVisibility(View.GONE);

                        String details = String.format("%dx%d", mBitmapOriginal.getWidth(), mBitmapOriginal.getHeight());
                        mTextViewImageDetails.setText(details);
                        mTextViewImageDetails.setVisibility(View.VISIBLE);

                        mMenu = R.menu.menu_dither;
                        invalidateOptionsMenu();


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public String getPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
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
    }
}