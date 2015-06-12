package com.danielmerrill.ocrlive;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;

import com.danielmerrill.ocrlive.Core.CameraEngine;
import com.danielmerrill.ocrlive.Core.ExtraViews.FocusBoxView;
import com.danielmerrill.ocrlive.Core.Imaging.Tools;
import com.danielmerrill.ocrlive.Core.TessTool.TessAsyncEngine;
import com.danielmerrill.ocrlive.Core.Imaging.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener,
        Camera.PictureCallback, Camera.ShutterCallback, AsyncResponse {

    static final String TAG = "DBG_" + MainActivity.class.getName();
    private CameraEngine cameraEngine;
    private WordUtils wordUtils;

    private FocusBoxView focusBoxView;
    private SurfaceView cameraFrame;
    private TextView wordText;
    private TextView statusText;
    private ImageView previewImage;
    private ListView definitionListView;
    private FrameLayout frameLayout;
    private View fadeView;

    private String foundWord;
    private ArrayList<HashMap<String, String>> definitionList = new ArrayList<>();
    private final String WORD = "word";
    private final String PART_OF_SPEECH = "partOfSpeech";
    private final String TEXT = "text";


    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        wordUtils = new WordUtils(this);
        progressDialog = new ProgressDialog(this);

        frameLayout = (FrameLayout) findViewById(R.id.frame_layout);
        cameraFrame = (SurfaceView) findViewById(R.id.camera_frame);
        focusBoxView = (FocusBoxView) findViewById(R.id.focus_box);
        wordText = (TextView)findViewById(R.id.word_text);
        statusText = (TextView)findViewById(R.id.status_text);
        previewImage = (ImageView) findViewById(R.id.image_preview);
        previewImage.setVisibility(View.INVISIBLE);
        definitionListView = (ListView)findViewById(R.id.definition_list);


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated()");

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        Log.i(TAG, "surfaceChanged()");

        if (cameraEngine != null && !cameraEngine.isOn()) {
            Log.d(TAG, "Starting camera...");
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

        cameraEngine.start();

        Log.i(TAG, "Camera Frame width= " + cameraFrame.getWidth() + " height= " + cameraFrame.getHeight());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed()");

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                Log.i(TAG, "About menu");
        }
        return true;
    }



    @Override
    protected void onResume() {
        Log.i(TAG, "onResume()");
        super.onResume();

        SurfaceHolder surfaceHolder = cameraFrame.getHolder();
        Log.i(TAG, "Surfaceholder: height: " + surfaceHolder.getSurfaceFrame().height());
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        cameraFrame.setVisibility(View.VISIBLE);


       // cameraFrame.setOnClickListener(this);

    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause()");
        super.onPause();
        //ObjectAnimator colorFade = ObjectAnimator.ofObject(blackView, "backgroundColor", new ArgbEvaluator(), Color.argb(255, 255, 255, 255), 0xff000000);
        //colorFade.setDuration(250);
        //colorFade.start();

        if (cameraEngine != null && cameraEngine.isOn()) {
            Log.i(TAG, "stopping camera");
            cameraEngine.stop();
        }

        SurfaceHolder surfaceHolder = cameraFrame.getHolder();
        surfaceHolder.removeCallback(this);
        cameraFrame.setVisibility(View.GONE);

    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
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
                    foundWord = StringUtils.parseLines(s).replaceAll("\\s+", "");
                    if (foundWord.length() > 1) {
                        wordUtils.setWord(foundWord);
                    }
                    break;

                case DEFINITION:
                    if (s.length() > 0) {
                        wordText.setText(foundWord);
                    }
                    break;
            }
        }
    }

    public void setListAdapter(JSONArray jsonArray) {
        // Reinitialize words and definitions to null;
        foundWord = "";
        definitionList.clear();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject c = jsonArray.getJSONObject(i);
                if (foundWord == "") {
                    foundWord = c.getString(WORD);
                }

                // Store returned values
                String partOfSpeech = c.getString(PART_OF_SPEECH);
                String text = c.getString(TEXT);
                Log.i(TAG, partOfSpeech + ": " + text);

                // Add to a HashMap and stash it in the definition list
                HashMap<String, String> map = new HashMap<>();
                map.put(PART_OF_SPEECH, partOfSpeech);
                map.put(TEXT, text);

                definitionList.add(map);

            }
            Log.i(TAG, "Definition list is this long: " + definitionList.size());
            definitionListView = (ListView)findViewById(R.id.definition_list);

            ListAdapter adapter = new SimpleAdapter(MainActivity.this, definitionList, R.layout.listview_definitions,
                    new String[]{PART_OF_SPEECH, TEXT}, new int[] {R.id.part_of_speech, R.id.definition_text});
            definitionListView.setAdapter(adapter);
            definitionListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                   // Toast.makeText(MainActivity.this, "You Clicked at " + oslist.get(+position).get("name"), Toast.LENGTH_SHORT).show();

                }
            });


            wordText.setText(foundWord);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}