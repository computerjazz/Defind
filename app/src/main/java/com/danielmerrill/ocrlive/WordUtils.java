package com.danielmerrill.ocrlive;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by danielmerrill on 5/31/15.
 */
public class WordUtils {
    private Keys mKeys = new Keys();
    private String baseUrl = "http://api.wordnik.com/v4/word.json/";
    private final String API_KEY = mKeys.getWordnikApiKey();
    private String word = "";
    private String mUrl;
    private AsyncResponse mActivity;
    private final String TAG = getClass().getSimpleName();

    public WordUtils(AsyncResponse activity) {
        this.mActivity = activity;
    }

    public void setWord(String word) {
        this.word = word;
        makeUrl();
        AsyncTaskParseJson asyncTaskParseJson = new AsyncTaskParseJson();
        asyncTaskParseJson.delegate = mActivity;
        asyncTaskParseJson.execute();
    }

    private void makeUrl() {
        mUrl = baseUrl + word + "/definitions?api_key=" + API_KEY;
        Log.i(TAG, mUrl);
    }

    private class AsyncTaskParseJson extends AsyncTask<String, String, String> {
        final String TAG = "AsyncTaskParseJson";
        public AsyncResponse delegate = null;
        String response = "";

        // words JSONArray
        JSONArray dataJsonArr = null;

        @Override
        protected void onPreExecute() {}

        @Override
        protected String doInBackground(String... arg0) {
            try {
                // Instantiate JSON parser
                JsonParser jParser = new JsonParser();

                // Get json array from url
                dataJsonArr = jParser.getJSONFromUrl(mUrl);

                // Probably should test if we got anything back
                if (dataJsonArr != null) {
                    for (int i = 0; i < dataJsonArr.length(); i++) {
                        JSONObject c = dataJsonArr.getJSONObject(i);

                        String partOfSpeech = c.getString("partOfSpeech");
                        String text = c.getString("text");
                        Log.i(TAG, partOfSpeech + ": " + text);
                        response += partOfSpeech + ": " + text + '\n' + '\n';
                    }
                } else {
                    Log.i(TAG, "JSON Array is null");
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected  void onPostExecute(String stringFromDoInBg) {
            mActivity.processFinish(stringFromDoInBg, AsyncTypes.Type.DEFINITION);
        }
    }
}
