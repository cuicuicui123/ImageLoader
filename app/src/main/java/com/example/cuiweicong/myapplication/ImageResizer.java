package com.example.cuiweicong.myapplication;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileDescriptor;

public class ImageResizer {
    private static final String TAG = "ImageResizer";

    public Bitmap decodeSampleBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId);
    }

    public Bitmap decodeSampleBitmapFromFileDescriptor(FileDescriptor fileDescriptor, int reqWidth, int reqHeight){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = true;
        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight){
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        while (width > reqWidth || height > reqHeight) {
            width = width / 2;
            height = height / 2;
            inSampleSize = inSampleSize * 2;
        }
        return inSampleSize;
    }

}
