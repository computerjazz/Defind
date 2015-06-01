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
import android.widget.Switch;
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

    TextView wordText;
    TextView definitionText;
    TextView statusText;

    ImageView previewImage;
    Rect previewBox;
    String tempWord;
    String foundWord;
    WordUtils wordUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        wordUtils = new WordUtils(this);

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
        Log.i(TAG, "onResume()");
        super.onResume();

        cameraFrame = (SurfaceView) findViewById(R.id.camera_frame);
        focusBox = (FocusBoxView) findViewById(R.id.focus_box);
        previewBox = focusBox.getBox();
        wordText = (TextView)findViewById(R.id.word_text);
        definitionText = (TextView)findViewById(R.id.definition_text);
        statusText = (TextView)findViewById(R.id.status_text);
        previewImage = (ImageView) findViewById(R.id.image_preview);


        SurfaceHolder surfaceHolder = cameraFrame.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        cameraFrame.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause()");
        super.onPause();

        if (cameraEngine != null && cameraEngine.isOn()) {
            Log.i(TAG, "stopping camera");
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


    }

    @Override
    public void onShutter() {

    }

    public void setPreviewImage(Bitmap bmp) {
        if (bmp != null) {
            previewImage.setImageBitmap(bmp);
        }
    }

    public void processFinish(String s, AsyncTypes.Type type) {
        Log.i(TAG, "onProcessFinish() - " + s);
        if (s.length() > 0) {
            switch (type) {
                case WORD:
                    foundWord = StringUtils.parseLines(s);
                    if (foundWord.length() > 1) {
                        wordUtils.setWord(foundWord);
                    }
                    break;

                case DEFINITION:
                    if (s.length() > 0) {
                        wordText.setText(foundWord);
                        definitionText.setText(s);
                    }
                    break;


            }
        }



    }

}