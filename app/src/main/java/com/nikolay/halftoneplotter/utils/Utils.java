package com.nikolay.halftoneplotter.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

import com.nikolay.halftoneplotter.R;
import com.nikolay.halftoneplotter.bluetooth.BluetoothCommands;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static final double THUMBNAIL_SIZE = 720;

    public static String getPathFromURI(Context context, Uri contentUri) {
        String path = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            path = cursor.getString(column_index);
        }
        cursor.close();
        return path;
    }

    public static Bitmap decodeImageFromUri(Context context, Uri uri, boolean scale) {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);

            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
            onlyBoundsOptions.inJustDecodeBounds = true;
            onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
            BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
            input.close();

            if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
                return null;
            }

            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            if(scale) {
                int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;
                double ratio = (originalSize > THUMBNAIL_SIZE) ? ((double) originalSize / THUMBNAIL_SIZE) : 1.0;
                bitmapOptions.inSampleSize = getSampleRatio(ratio);
            }
            input = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
            input.close();

            return bitmap;
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int[] getImageSizeFromUri(Context context, Uri uri) {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);

            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
            onlyBoundsOptions.inJustDecodeBounds = true;
            onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
            BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
            input.close();
            return new int[] {onlyBoundsOptions.outWidth, onlyBoundsOptions.outHeight};
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int getSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k == 0) return 1;
        else return k;
    }

    public static List<Instruction> convertImageToInstructions(Context context, Uri uri, boolean uniteBlacks) {
        List<Instruction> instructions = new ArrayList<Instruction>();

        Bitmap bitmap = decodeImageFromUri(context, uri, false);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // TODO unite black pixels??

        // -16777216 = black; -1 = white
        for(int i = 0; i < width * height; i++) {
            int pixel = pixels[i];
            if(pixel == -16777216) {
                instructions.add(new Instruction(BluetoothCommands.COMMAND_DOT, BluetoothCommands.VALUE_DOT));
            }
            if((i % width) == (width - 1)) { // last pixel on the row
                instructions.add(new Instruction(BluetoothCommands.COMMAND_UP, BluetoothCommands.VALUE_UP));
                instructions.add(new Instruction(BluetoothCommands.COMMAND_LEFT, (width - 1) * BluetoothCommands.VALUE_LEFT));
            }
            else {
                instructions.add(new Instruction(BluetoothCommands.COMMAND_RIGHT, BluetoothCommands.VALUE_RIGHT));
            }
        }

        List<Instruction> finalSequence = new ArrayList<Instruction>();
        int n = 0;
        for(int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if(instruction.getCommand() == BluetoothCommands.COMMAND_RIGHT) {
                while(instructions.get(i + n).getCommand() == BluetoothCommands.COMMAND_RIGHT) {
                    n++;
                    if((i + n) >= instructions.size()) break;
                }
                finalSequence.add(new Instruction(BluetoothCommands.COMMAND_RIGHT, BluetoothCommands.VALUE_RIGHT * n));
                i += (n - 1);
                n = 0;
            }
            else {
                finalSequence.add(instruction);
            }
        }

        return finalSequence;
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void setPreference(Context context, String key, String value) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void setPreference(Context context, String key, int value) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void clearPreferences(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(context.getString(R.string.image_uri_key));
        editor.remove(context.getString(R.string.coordinate_x_key));
        editor.remove(context.getString(R.string.coordinate_y_key));
        editor.apply();
    }
}
