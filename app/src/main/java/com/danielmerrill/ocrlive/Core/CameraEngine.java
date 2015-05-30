package com.danielmerrill.ocrlive.Core;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.ImageView;

import com.danielmerrill.ocrlive.AsyncResponse;
import com.danielmerrill.ocrlive.Core.ExtraViews.FocusBoxView;
import com.danielmerrill.ocrlive.Core.TessTool.TessAsyncEngine;
import com.danielmerrill.ocrlive.MainActivity;
import com.danielmerrill.ocrlive.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Fadi on 5/11/2014.
 */
public class CameraEngine {

    static final String TAG = "DBG_" + CameraUtils.class.getName();
    Camera.Parameters params;
    Activity mActivity;
    boolean on;
    Camera camera;
    SurfaceHolder surfaceHolder;
    private byte[] yuv;
    private Camera.Size previewSize;
    private int width;
    private int height;
    private boolean cameraConfigured = false;
    private Rect previewBox;
    private AsyncResponse mDelegate;
    private TessAsyncEngine testEngine;

    private ImageView imagePreview;
    private Bitmap previewBmp;

    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {

        }
    };

    public boolean isOn() {
        return on;
    }

    public void setBox(Rect box) {
        Log.i(TAG, "setBox()");
        if (box != null) {
            previewBox = box;
            // Log.i(TAG, "setting previewBox: width = " + previewBox.width() + ", height = " + previewBox.height() + ", left = " + previewBox.left + ", top = " + previewBox.top + ", right = " + previewBox.right + ", bottom = " + previewBox.bottom);
        }
    }

    public Rect calcBox() {
        // Previewsize assumes landscape, so the math gets a little wonky
        int width = previewSize.height * 6 / 7;
        int height = previewSize.width / 9;
        int wGap = (previewSize.height - width) / 2;
        int top = (previewSize.height - width) / 2;
        int left = previewSize.width / 2;
        // Give a little more room at the bottom
        int right = left + height + (height/4);
        int bottom = top + width;
        Log.i(TAG, "calcBox(): left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom);
        return new Rect(left, top, right, bottom);
    }

    // Scale preview box from its location on the raw image size to its location on our "best" preview size
    private Rect scaleBox(Rect box) {
        float heightRatio = (float) previewSize.height / height;
        float widthRatio = (float) previewSize.width / width;

        int left = Math.round((float) box.left * widthRatio);
        int top = Math.round((float) box.top * heightRatio);
        int right = Math.round((float) box.right * widthRatio);
        int bottom = Math.round((float) box.bottom * heightRatio);
        Log.i(TAG, "HeightRatio: " + heightRatio + " widthRatio: " + widthRatio + ", dimens: (" + left + ", " + top + ", " + right + ", " + bottom);
        Rect scaledBox = new Rect(left, top, right, bottom);
        return scaledBox;
    }

    public void setImagePreview(ImageView imagePreview) {
        this.imagePreview = imagePreview;
    }

    public Bitmap getPreviewBmp() {
        return previewBmp;
    }

    private CameraEngine(SurfaceHolder surfaceHolder, Activity mActivity){
        this.mActivity = mActivity;
        this.surfaceHolder = surfaceHolder;
    }

    static public CameraEngine New(SurfaceHolder surfaceHolder, Activity mActivity){
        Log.d(TAG, "Creating camera engine");
        return  new CameraEngine(surfaceHolder, mActivity);
    }

    public void requestFocus() {
        if (camera == null)
            return;

        if (isOn()) {
            camera.autoFocus(autoFocusCallback);
        }
    }

    public void start() {

        Log.d(TAG, "Entered CameraEngine - start()");

        this.camera = CameraUtils.getCamera();
        params = camera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        Log.i(TAG, "Max zoom = " + params.getMaxZoom());

        // Maybe in the future change this to more dynamically match phone models
        params.setZoom(params.getMaxZoom()/2);

        if (!cameraConfigured) {

            Log.i(TAG, "Raw width = " + width + ", height = " + height);
            previewSize = getBestPreviewSize(width, height, params);
            Log.i(TAG, "width=" + previewSize.width + ", height= " + previewSize.height);
            if (previewSize != null) {
                params.setPreviewSize(previewSize.width,
                        previewSize.height);
                camera.setParameters(params);
                cameraConfigured = true;
            }
        } else {
            camera.setParameters(params);
        }


        if (this.camera == null)
            return;

        // Now that we have our preview size, scale box to fit
        previewBox = calcBox();


        Log.d(TAG, "Got camera hardware");

        try {

            this.camera.setPreviewDisplay(this.surfaceHolder);
            this.camera.setDisplayOrientation(90);
            this.camera.startPreview();


            // Testing new code here
            // Update data each time we get a preview frame
            yuv = new byte[getBufferSize()];
            // Initialize a box to use for capture
            final Rect captureRect = calcBox();
            // set a time delay

            camera.addCallbackBuffer(yuv);
            camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {


                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {

                        try {
                            //Log.i(TAG, "inside onPreviewFrame()");
                            //Log.i(TAG, captureRect.width() + ", " + captureRect.height());
                            previewBmp = null;
                            previewBmp = getBitmapImageFromYUV(data, captureRect, previewSize.width, previewSize.height);
                            if (previewBmp == null) {
                                Log.i(TAG, "BITMAP NULL!");
                            }
                            //Log.i(TAG, "previewBmp size: " + previewBmp.getWidth() + " x " + previewBmp.getHeight());
                            previewBmp = rotateBitmap(previewBmp, 90);
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    imagePreview.setImageBitmap(previewBmp);
                                }
                            });

                            if ((testEngine == null) || (testEngine.getStatus() != AsyncTask.Status.RUNNING)) {
                                Log.i(TAG, "Creating new tessengine");
                                TessAsyncEngine tessEngine = new TessAsyncEngine();
                                tessEngine.delegate = (AsyncResponse)mActivity;
                                tessEngine.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mActivity, previewBmp);
                                testEngine = tessEngine;
                            }



                            camera.addCallbackBuffer(yuv);



                        } catch (Exception e) {
                            Log.i(TAG, "onPreviewFrame() error: " + e.toString());
                        }
                    }
            });


            on = true;

            Log.d(TAG, "CameraEngine preview started");

        } catch (IOException e) {
            Log.e(TAG, "Error in setPreviewDisplay");
        }
    }

    public void stop(){

        if(camera != null){
            camera.release();
            camera = null;
        }

        on = false;

        Log.d(TAG, "CameraEngine Stopped");
    }

    public void takeShot(Camera.ShutterCallback shutterCallback,
                         Camera.PictureCallback rawPictureCallback,
                         Camera.PictureCallback jpegPictureCallback ){
        if(isOn()){
            camera.takePicture(shutterCallback, rawPictureCallback, jpegPictureCallback);
        }
    }

    private int getBufferSize() {
        int pixelformat = ImageFormat.getBitsPerPixel(camera.getParameters()
                .getPreviewFormat());
        int bufSize = (previewSize.width * previewSize.height * pixelformat) / 8;
        Log.i(TAG, "getBufferSize(): pixelformat = " + pixelformat + ", Previewsize Width = " + previewSize.width + ", height = " + previewSize.height + ", buffer size is " + bufSize);
        return bufSize;
    }
    /*
    * Iterates through the camera sizes and chooses the one that is smaller than the preview width and height
    *
    */

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    // sets the result to the first option we find
                    result = size;
                } else {
                    // if we find another, calculate the area of both and choose the biggest
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;
                    if (newArea > resultArea) {
                        result = size;
                    }
                }

            }
        }
        Log.i(TAG, "Best preview size is width = " + result.width + ", height = " + result.height);
        return result;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public static Bitmap getBitmapImageFromYUV(byte[] data, Rect captureRect, int imageWidth, int imageHeight) {
        // Log.i(TAG, "getBitmapImageFromYUV()");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Create a YuvImage of the entire screen
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, imageWidth , imageHeight, null);

        // Capture just the rectangle we want to analyze
        yuvimage.compressToJpeg(captureRect, 90, baos);
        BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
        bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;

        byte[] jdata = baos.toByteArray();
        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, bitmapFatoryOptions);
        yuvimage = null;
        baos = null;
        return bmp;
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }



}


