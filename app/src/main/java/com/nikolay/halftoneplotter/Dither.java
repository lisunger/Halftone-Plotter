package com.nikolay.halftoneplotter;

import android.graphics.Bitmap;
import android.util.Log;

public class Dither {

    public static Bitmap grayscale(Bitmap image) {
        Log.d("Lisko", image.getByteCount() + "");
        final int width = image.getWidth();
        final int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getPixels(pixels, 0, width, 0, 0, width, height);

        for(int p = 0; p < pixels.length; p++) {

            int r = ((byte)(pixels[p] >> 16)) & 0xff;
            int g = ((byte)(pixels[p] >> 8 )) & 0xff;
            int b = ((byte)(pixels[p] >> 0 )) & 0xff;

            int value = (int)((0.30 * r) + (0.59 * g) + (0.11 * b));
            value = (value < 0) ? 0 : value;
            value = (value > 255) ? 255 : value;

            int grayPixel = 0;
            grayPixel += ((byte)0xff) << 24;
            grayPixel += ((byte)value << 16);
            grayPixel += ((byte)value << 8);
            grayPixel += ((byte)value << 0);

            pixels[p] = grayPixel;

        }

        return Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
    }

    public static Bitmap fsDither(Bitmap image) {
        return null;
    }
}
