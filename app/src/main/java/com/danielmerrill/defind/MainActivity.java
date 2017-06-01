package com.danielmerrill.defind;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.danielmerrill.defind.Core.CameraEngine;
import com.danielmerrill.defind.Core.ExtraViews.FocusBoxView;
import com.danielmerrill.defind.Core.TessTool.TessAsyncEngine;
import com.danielmerrill.defind.Core.TessTool.TessEngine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener,
        Camera.PictureCallback, Camera.ShutterCallback, AsyncResponse, InstructionsFragment.OnFragmentInteractionListener {

    static final String TAG = "DBG_" + MainActivity.class.getName();
    private CameraEngine cameraEngine;
    private WordUtils wordUtils;
    private FocusBoxView focusBoxView;
    private SurfaceView cameraFrame;
    private TextView wordText;
    private Bitmap wordBmp;
    private ScaleGestureDetector scaleGestureDetector;
    private ScaleListener scaleListener;
    private boolean paused;
    private boolean debug;

    // Testing tools
    private ImageView previewImage;
    private int ocrImageHeight;
    private long timestamp;

    private ListView definitionListView;
    private TextRegionDetector mDetector;
    private DrawerLayout mDrawer;
    private ImageView mLogo;
    private ImageView mDylanContact;
    private ImageView mDanContact;
    private ImageView mWordnik;
    private String WORDNIK_BASE_URL = "http://www.wordnik.com/words/";
    private String DAN_URL = "http://www.danielmerrill.com";
    private String DAN_EMAIL = "hi@danielmerrill.com";
    private String DYLAN_EMAIL = "djw.mckeever@gmail.com";
    private String wordnikUrl;

    private Fragment instructionsFrag;

    private String foundWord;
    private String previousWord;
    private String searchedWord;
    private ArrayList<HashMap<String, String>> definitionList = new ArrayList<>();
    private final String WORD = "word";
    private final String PART_OF_SPEECH = "partOfSpeech";
    private final String TEXT = "text";

    // Set up some TessAsyncEngines
    private int NUMBER_OF_ENGINES = 1;
    private int OCR_IMAGE_HEIGHT = 75;
    private TessAsyncEngine[] asyncEngineArray;
    private TessEngine[] tessEngineArray;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        wordUtils = new WordUtils(this);

        cameraFrame = (SurfaceView) findViewById(R.id.camera_frame);
        focusBoxView = (FocusBoxView) findViewById(R.id.focus_box);
        wordText = (TextView) findViewById(R.id.word_text);

        ocrImageHeight = OCR_IMAGE_HEIGHT;

        // Testing tools init
        debug = false;
        previewImage = (ImageView) findViewById(R.id.image_preview);
        if (!debug) {
            ((ViewGroup) previewImage.getParent()).removeView(previewImage);
        }

        //Tess engine init
        asyncEngineArray = new TessAsyncEngine[NUMBER_OF_ENGINES];
        tessEngineArray = new TessEngine[NUMBER_OF_ENGINES];
        for (int i = 0; i < NUMBER_OF_ENGINES; i++) {
            tessEngineArray[i] = TessEngine.Generate(this);
        }


        definitionListView = (ListView) findViewById(R.id.definition_list);

        mDanContact = (ImageView) findViewById(R.id.danContact);
        mDylanContact = (ImageView) findViewById(R.id.dylanContact);

        mWordnik = (ImageView) findViewById(R.id.navWordnik);

        mDetector = new TextRegionDetector(this);
        paused = false;

        // Pause if the user touches screen
        definitionListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    paused = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    paused = false;
                }
                mDetector.setPaused(paused);
                return false;
            }
        });

        setWordnikUrl("word");
        mWordnik.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(wordnikUrl));
                startActivity(intent);
            }
        });

        mDanContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent send = new Intent(Intent.ACTION_SENDTO);
                String uriText = "mailto:" + Uri.encode(DAN_EMAIL);
                Uri uri = Uri.parse(uriText);

                send.setData(uri);
                startActivity(Intent.createChooser(send, "Send mail..."));
            }
        });

        mDylanContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent send = new Intent(Intent.ACTION_SENDTO);
                String uriText = "mailto:" + Uri.encode(DYLAN_EMAIL);
                Uri uri = Uri.parse(uriText);

                send.setData(uri);
                startActivity(Intent.createChooser(send, "Send mail..."));
            }
        });

        if (isFirstTime()) {
            // Show instruction screen the first time in
            instructionsFrag = new InstructionsFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.frame_layout, instructionsFrag)
                    .commit();
        }

        View drawerView = findViewById(R.id.drawer_layout);
        if (drawerView != null && drawerView instanceof DrawerLayout) {
            mDrawer = (DrawerLayout)drawerView;
            mDrawer.setDrawerListener(new DrawerLayout.DrawerListener() {
                @Override
                public void onDrawerSlide(View view, float v) {

                }

                @Override
                public void onDrawerOpened(View view) {
                    paused = true;
                    mDetector.setPaused(paused);

                }

                @Override
                public void onDrawerClosed(View view) {
                    paused = false;
                    mDetector.setPaused(paused);
                }

                @Override
                public void onDrawerStateChanged(int i) {

                }
            });
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        super.dispatchTouchEvent(e);
        scaleGestureDetector.onTouchEvent(e);
        return true;
    }

    private boolean isFirstTime() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean ranBefore = preferences.getBoolean("RanBefore", false);
        if (!ranBefore) {
            // first time
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("RanBefore", true);
            editor.commit();
        }
        return !ranBefore;
    }

    private boolean networkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        int zoom = preferences.getInt("zoomLevel", 10);
        if (cameraEngine != null && !cameraEngine.isOn()) {
            cameraEngine.start(zoom);
        }

        if (cameraEngine != null && cameraEngine.isOn()) {
            initScaleListener(cameraEngine.getCamera());
            return;
        }

        cameraEngine = CameraEngine.New(holder, this);
        cameraEngine.setHeight(holder.getSurfaceFrame().height());
        cameraEngine.setWidth(holder.getSurfaceFrame().width());
        cameraEngine.setImagePreview(previewImage);
        cameraEngine.start(zoom);
        initScaleListener(cameraEngine.getCamera());
    }

    private void initScaleListener(Camera camera) {
        if (scaleListener == null ) {
            scaleListener = new ScaleListener(this, camera);
        } else {
            scaleListener.setCamera(camera);
        }
        if (scaleGestureDetector == null) {
            scaleGestureDetector = new ScaleGestureDetector(this, scaleListener);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
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
        }
        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }



        SurfaceHolder surfaceHolder = cameraFrame.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        cameraFrame.setVisibility(View.VISIBLE);

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (cameraEngine != null && cameraEngine.isOn()) {
            cameraEngine.stop();
        }

        SurfaceHolder surfaceHolder = cameraFrame.getHolder();
        surfaceHolder.removeCallback(this);
        cameraFrame.setVisibility(View.GONE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
    }

    @Override
    public void onShutter() {

    }

    public void updateTextRect(Rect r) {
        focusBoxView.setTextBox(r);
        focusBoxView.invalidate();

    }

    public Rect getFrameSize() {
        return focusBoxView.getBox();
    }

    public void setPreviewImage(Bitmap bmp) {
        if (bmp != null && debug) {
            previewImage.setImageBitmap(bmp);
        }
    }

    private void setWordnikUrl(String word) {
        wordnikUrl = WORDNIK_BASE_URL + word;
    }

    public void setPreviewSize(int w, int h) {
        focusBoxView.setPreviewSize(w, h);
    }


    public void getWordBmpFromFullImage(Bitmap bmp) {
        wordBmp = mDetector.process(bmp);
        if (wordBmp != null) {
            int imageWidth = (ocrImageHeight * wordBmp.getWidth()) / wordBmp.getHeight();

            // Scale bitmap for faster processing
            wordBmp = Bitmap.createScaledBitmap(wordBmp, imageWidth, ocrImageHeight, false);
            runOCR(wordBmp);
        }
        //setPreviewImage(wordBmp);
    }


    public void runOCR(Bitmap bmp) {
        boolean engineFree = false;
        int engineIndex = 0;

        // Tests to see if we have a free engine and only runs OCR if we do
        for (int i = 0; i < NUMBER_OF_ENGINES; i++) {
            if (asyncEngineArray[i] == null) {
                engineFree = true;
                engineIndex = i;
                break;
            }
        }
        if (engineFree) {
            TessAsyncEngine tessAsyncEngine = new TessAsyncEngine();
            tessAsyncEngine.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this, bmp, this, engineIndex, tessEngineArray[engineIndex]);
            asyncEngineArray[engineIndex] = tessAsyncEngine;
        }
    }

    public void releaseTask(int engineIndex, long timestamp) {
        asyncEngineArray[engineIndex] = null;
    }


    public boolean isPaused() {
        return paused;
    }

    public void processFinish(String s, AsyncTypes.Type type) {
        Log.i(TAG, "onProcessFinish() - " + s);
        if (s.length() > 0 && !paused) {
            switch (type) {
                // Words come back from OCR, definitions come back from API calls to WordNik
                case WORD:
                    foundWord = StringUtils.parseLines(s).replaceAll("\\s+", "");
                    timestamp = System.currentTimeMillis();
                    if (!networkAvailable()) {
                        Toast.makeText(getApplicationContext(), "No network connection", Toast.LENGTH_LONG).show();
                    }
                    else if ((foundWord.length() > 1) && !foundWord.equals(previousWord)) {
                        wordUtils.setWord(foundWord);
                        previousWord = foundWord;

                    } else if ((foundWord.length() > 1) && foundWord.equals(previousWord) && !foundWord.equals(searchedWord)) {
                        Toast.makeText(getApplicationContext(), "\"" + foundWord + "\" not found",
                                Toast.LENGTH_SHORT).show();
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

    public void setSearchedWord(String word) {
        searchedWord = word;
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
                //Log.i(TAG, partOfSpeech + ": " + text);

                // Add to a HashMap and stash it in the definition list
                HashMap<String, String> map = new HashMap<>();
                map.put(PART_OF_SPEECH, partOfSpeech);
                map.put(TEXT, text);

                definitionList.add(map);

            }
            //Log.i(TAG, "Definition list is this long: " + definitionList.size());
            definitionListView = (ListView) findViewById(R.id.definition_list);

            ListAdapter adapter = new SimpleAdapter(MainActivity.this, definitionList, R.layout.listview_definitions,
                    new String[]{PART_OF_SPEECH, TEXT}, new int[]{R.id.part_of_speech, R.id.definition_text});
            definitionListView.setAdapter(adapter);
            definitionListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    // Toast.makeText(MainActivity.this, "You Clicked at " + oslist.get(+position).get("name"), Toast.LENGTH_SHORT).show();

                }
            });


            wordText.setText(foundWord);
            setWordnikUrl(foundWord);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Gets called from the instructions screen and kills the instructions fragment
    public void onFragmentInteraction(Uri uri) {
        getFragmentManager().beginTransaction()
                .remove(instructionsFrag)
                .commit();
        //mDrawer.openDrawer(Gravity.LEFT);
    }
}