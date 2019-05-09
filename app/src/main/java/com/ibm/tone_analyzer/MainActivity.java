package com.ibm.tone_analyzer;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneOptions;
import com.ibm.watson.developer_cloud.service.security.IamOptions;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneScore;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.ibm.mobilefirstplatform.clientsdk.android.core.internal.BaseRequest.GET;


public class MainActivity extends AppCompatActivity {


    final int SPEECHINPUT_REQUESTCODE = 10;
    boolean firstAnalysis = true;
    private ToneAnalyzer toneAnalyzerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        getActionBar();
        setSupportActionBar(toolbar);

        // Core SDK must be initialized to interact with Bluemix Mobile services.
        BMSClient.getInstance().initialize(getApplicationContext(), BMSClient.REGION_US_SOUTH);

        IamOptions.Builder iamOptionsBuilder =new IamOptions.Builder();
        iamOptionsBuilder.apiKey(getString(R.string.toneanalyzerApikey));
        toneAnalyzerService = new ToneAnalyzer("2017-09-21",
                iamOptionsBuilder.build());
        toneAnalyzerService.setEndPoint(getString(R.string.toneanalyzerUrl));
        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText entryTextView = (EditText)findViewById(R.id.entryText);
                String entryText = entryTextView.getText().toString();

                if (!entryText.isEmpty()) {
                    // Send the user's input text to the AnalyzeTask.
                    AnalyzeTask analyzeTask = new AnalyzeTask();
                    analyzeTask.execute(entryText);
                } else {
                    Snackbar.make(v, "There's no text to analyze", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        AnalyzeTask analyzeTask = new AnalyzeTask();
        analyzeTask.execute(" ");

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        System.out.println("CHECKPOINT onPause");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onDestroy() {
        // Have the fragment save its state for recreation on orientation changes.
        //resultFragment.saveData();
        super.onDestroy();
    }

    static boolean speechRecordFlag=true,masterRecordFlag=true;
    Handler handler;
    public void getSpeechInput(View view) {

        ImageButton micButton=(ImageButton) findViewById(R.id.speechButton);

        final Toast toast= Toast.makeText(this, "For some reason your device does not support speech input",
                Toast.LENGTH_SHORT);

        System.out.println("CHECKPOINT 1");

        if(speechRecordFlag && masterRecordFlag){

            System.out.println("CHECKPOINT 1-1");

            micButton.setBackgroundColor(Color.parseColor("#33a532"));

            handler = new Handler();
            final Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    try {
                        //do your code here
                        System.out.println("CHECKPOINT 1-2");

                        if (speechRecordFlag) {
                            System.out.println("CHECKPOINT 1-3");
                            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,15);
                            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,15);

                            if (intent.resolveActivity(getPackageManager()) != null) { //make sure device supports functionality
                                startActivityForResult(intent, SPEECHINPUT_REQUESTCODE);//request code is whatever we choose
                            } else {
                                toast.show();
                            }
                        }
                        else{
                            System.out.println("CHECKPOINT 1-4");
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    } finally {

                        System.out.println("CHECKPOINT 1-5");
                        //also call the same runnable to call it at regular interval
                        handler.postDelayed(this, 20000);
                    }
                }
            };
            //runnable must be execute once
            handler.post(runnable);


            masterRecordFlag=false;
        }
        else{

            System.out.println("CHECKPOINT 2");

            handler.removeCallbacksAndMessages(null);

            speechRecordFlag=true;
            masterRecordFlag=true;
            micButton.setBackgroundColor(Color.parseColor("#ffffff"));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SPEECHINPUT_REQUESTCODE:
                if (resultCode == RESULT_OK && data != null){
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    EditText entryTextView = (EditText)findViewById(R.id.entryText);
                    entryTextView.setText(result.get(0));
                    AnalyzeTask analyzeTask = new AnalyzeTask();
                    analyzeTask.execute(result.get(0));
                }
                break;
        }
    }

    /**
     * Enables communication to the Tone Analyzer Service and fetches the service's output.
     */
    private class AnalyzeTask extends AsyncTask<String, Void, ToneAnalysis> {

        @Override
        protected void onPreExecute() {
            ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected ToneAnalysis doInBackground(String... params) {
            String entryText = params[0];
            ToneAnalysis analysisResult;
            ToneOptions.Builder toneoptionsbuilder = new ToneOptions.Builder();
            toneoptionsbuilder.text(entryText);

            try {
                analysisResult = toneAnalyzerService.tone(toneoptionsbuilder.build()).execute();
            } catch (Exception ex) {
                Toast.makeText(getApplicationContext(),"Api Call Failed",Toast.LENGTH_LONG).show();
                return null;
            }

            return analysisResult;
        }

        @Override
        protected void onPostExecute(ToneAnalysis result) {
            // Turn off the loading spinner.
            ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
            progressBar.setVisibility(View.GONE);

            // If null do nothing, an alertDialog will be popping up from the catch statement earlier.
            if (result != null) {
                if (result.getDocumentTone().getTones().size() == 0 && !firstAnalysis){
                    Toast.makeText(MainActivity.this, "No Tones Found", Toast.LENGTH_SHORT).show();
                }
                else {

                    if (!firstAnalysis){

                        ImageView outputImage=(ImageView) findViewById(R.id.outputImage);

                        switch (CreateResultArrayList(result.getDocumentTone().getTones()).get(0)){
                            case "Sadness":
                                outputImage.setImageResource(R.drawable.sad_cowboy);
                                break;
                            case "Anger":
                                outputImage.setImageResource(R.drawable.angry_bird);
                                break;
                            case "Fear":
                                outputImage.setImageResource(R.drawable.scared_crow);
                                break;
                            case "Joy":
                                outputImage.setImageResource(R.drawable.party_crow);
                                break;
                            case "Analytical":
                                outputImage.setImageResource(R.drawable.thinking);
                                break;
                            case "Confident":
                                outputImage.setImageResource(R.drawable.supreme_champion);
                                break;
                            case "Tentative":
                                outputImage.setImageResource(R.drawable.tentative_crow);
                                break;

                            default:
                                System.out.println("Unexpected Error selecting image to display");
                        }
                    }

                    firstAnalysis = false;//since the first analysis doesn't need a No Tones Prompt, ignore it
                }


            }
        }

        private ArrayList<String> CreateResultArrayList(List<ToneScore> resultList){
            //int numResult = resultList.size();
            ArrayList<String> listOfTones = new ArrayList<>();
            if (resultList.size() == 0)
                return listOfTones;

            for (int i = 0; i < resultList.size(); i ++){
                listOfTones.add(resultList.get(i).getToneName());
            }
            return listOfTones;

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        System.out.println("CHECKPOINT onBackPressed");

        try {
            handler.removeCallbacksAndMessages(null);
        }
        catch (Exception e){

        }
        finally {
            finish();
        }

    }
}