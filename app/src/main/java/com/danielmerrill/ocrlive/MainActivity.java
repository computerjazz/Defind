package com.danielmerrill.ocrlive;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.danielmerrill.ocrlive.Core.CameraEngine;
import com.danielmerrill.ocrlive.Core.ExtraViews.FocusBoxView;
import com.danielmerrill.ocrlive.Core.Imaging.Tools;
import com.danielmerrill.ocrlive.Core.TessTool.TessAsyncEngine;
import com.danielmerrill.ocrlive.Core.Imaging.Utils;


public class MainActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener,
        Camera.PictureCallback, Camera.ShutterCallback, AsyncResponse {

    static final String TAG = "DBG_" + MainActivity.class.getName();

    Button shutterButton;
    FocusBoxView focusBox;
    SurfaceView cameraFrame;
    CameraEngine cameraEngine;
    TextView testText;
    ImageView previewImage;
    Rect previewBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        testText = (TextView)findViewById(R.id.text_preview);

        previewImage = (ImageView) findViewById(R.id.image_preview);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.d(TAG, "Surface Created - starting camera");

        if (cameraEngine != null && !cameraEngine.isOn()) {
            cameraEngine.start();
        }

        if (cameraEngine != null && cameraEngine.isOn()) {
            Log.d(TAG, "Camera engine already on");
            return;
        }

        cameraEngine = CameraEngine.New(holder, this);
        cameraEngine.setHeight(holder.getSurfaceFrame().height());
        cameraEngine.setWidth(holder.getSurfaceFrame().width());
        cameraEngine.setImagePreview(previewImage);
        cameraEngine.setBox(focusBox.getBox());

        cameraEngine.start();

        Log.d(TAG, "Camera engine started");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        cameraEngine.setHeight(height);
        cameraEngine.setWidth(width);
        cameraEngine.setBox(focusBox.getBox());



    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    protected void onResume() {
        super.onResume();

        cameraFrame = (SurfaceView) findViewById(R.id.camera_frame);
        focusBox = (FocusBoxView) findViewById(R.id.focus_box);
        previewBox = focusBox.getBox();
        Log.i(TAG, previewBox.left + " " + previewBox.top + " " + previewBox.width() + " " + previewBox.height());


        SurfaceHolder surfaceHolder = cameraFrame.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        cameraFrame.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (cameraEngine != null && cameraEngine.isOn()) {
            cameraEngine.stop();
        }

        SurfaceHolder surfaceHolder = cameraFrame.getHolder();
        surfaceHolder.removeCallback(this);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        Log.d(TAG, "Picture taken");

        if (data == null) {
            Log.d(TAG, "Got null data");
            return;
        }


        Bitmap bmp = Tools.getFocusedBitmap(this, camera, data, focusBox.getBox());

        Log.d(TAG, "Got bitmap");

        Log.d(TAG, "Initialization of TessBaseApi");

        new TessAsyncEngine().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, this, bmp);

    }

    @Override
    public void onShutter() {

    }

    public void setPreviewImage(Bitmap bmp) {
        if (bmp != null) {
            previewImage.setImageBitmap(bmp);
        }
    }

    public void processFinish(String s) {
        testText.setText(s);
    }

}