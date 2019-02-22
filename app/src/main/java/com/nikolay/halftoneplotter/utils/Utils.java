package com.nikolay.halftoneplotter.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

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

            int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

            double ratio = (originalSize > THUMBNAIL_SIZE) ? ((double)originalSize / THUMBNAIL_SIZE) : 1.0;

            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inSampleSize = getSampleRatio(ratio);
            bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//
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

    public static List<Instruction> convertImageToInstructions(Context context, Uri uri) {
        List<Instruction> instructions = new ArrayList<Instruction>();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), imageId, options);
        //Log.d("Lisko", String.format("%d x %d", bitmap.getWidth(), bitmap.getHeight()));

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

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

        return instructions;
    }
}
