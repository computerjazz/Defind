package com.danielmerrill.defind;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.util.Log;
import android.view.ScaleGestureDetector;

import com.danielmerrill.defind.Core.CameraUtils;

/**
 * Created by danielmerrill on 5/29/17.
 */
public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    private Activity mActivity;
    private int maxZoom = -1;
    private Camera camera;
    private Camera.Parameters params;
    private SharedPreferences preferences;

    public ScaleListener(Activity activity, Camera camera) {
        mActivity = activity;
        this.camera = camera;
        preferences = activity.getPreferences(Context.MODE_PRIVATE);
        maxZoom = preferences.getInt("maxZoom", 0);

    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        params = camera.getParameters();
        int currentZoom = preferences.getInt("zoomLevel", 10);
        int difference = (int)((detector.getScaleFactor() - 1) * maxZoom);

        int nextZoom = currentZoom + difference;
        if (nextZoom < 0) {
            nextZoom = 0;
        } else if (nextZoom > maxZoom) {
            nextZoom = maxZoom;
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("zoomLevel", nextZoom);
        editor.commit();

        params.setZoom(nextZoom);
        camera.setParameters(params);
        return true;
    }
}
