package com.sss.breter.voicerecognizer;

import java.io.*;

import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import android.widget.TextView;
import com.sss.breter.voicerecognizer.recognizer.UnityAssets;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import edu.cmu.pocketsphinx.*;

import org.apache.commons.io.FileUtils;

import  com.sss.breter.voicerecognizer.recognizer.Grammar;
import  com.sss.breter.voicerecognizer.recognizer.PhonMapper;
import  com.sss.breter.voicerecognizer.recognizer.DataFiles;

import android.os.Environment;



public class MainActivity extends UnityPlayerActivity implements RecognitionListener {


    private static final String TAG = "Recognizer";
    private static final String COMMAND_SEARCH = "command";
    //private static final String KWS_SEARCH = "hotword";

    // start test area
    private static String _recieverObjectName;
    private static String _recieverMethodName;
    private static String _filesDirectory;

    public static void setRecieverObjectName(String name){
        _recieverObjectName = name;
    }

    public static void setRecieverMethodName(String name){
        _recieverMethodName = name;
    }

    public  static void toUnityLog(String message)
    {
        UnityPlayer.UnitySendMessage(_recieverObjectName, _recieverMethodName, message);
    }
    // end test area

    private final Handler mHandler = new Handler();

    private SpeechRecognizer mRecognizer;

    private final String ACOUSTIC_MODEL_DIR_NAME = "acoustic-models";
    private final String DICTIONARYS_DIR_NAME = "dictionaries";
    private final String GRAMMARS_DIR_NAME = "grammars";

    private String _assetDirOnSdCard;
    private String _language;
    private String _sampleRate;
    private String _dictionary;
    private String _grammar;
    private String _hotword;

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    private static final String DIGITS_SEARCH = "digits";
    private static final String MENU_SEARCH = "menu";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "oh mighty computer";

    public void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    //Assets assets = new Assets(MainActivity.this);
                    UnityAssets assets = new UnityAssets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    toUnityLog(e.getMessage());
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null)
                {
                   toUnityLog("Failed to init recognizer " + result);
                } else {
                    switchSearch(KWS_SEARCH);
                    toUnityLog(KWS_SEARCH);
                }
            }
        }.execute();
    }

    private void switchSearch(String searchName) {
        mRecognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            mRecognizer.startListening(searchName);
        else
            mRecognizer.startListening(searchName, 10000);
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them
        toUnityLog("start setup recognizer");
        mRecognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "acousticModels/16000"))
                .setDictionary(new File(assetsDir, "dictionaries/cmudict-en-us.dict"))

                //.setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .setKeywordThreshold(1e-45f) // Threshold to tune for keyphrase to balance between false alarms and misses
                .setBoolean("-allphone_ci", true)  // Use context-independent phonetic search, context-dependent is too slow for mobile


                .getRecognizer();
        mRecognizer.addListener(this);

        // Create keyword-activation search.
        mRecognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        // Create grammar-based search for selection between demos
        File menuGrammar = new File(assetsDir, "grammarFiles/menu.gram");
        mRecognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

        // Create grammar-based search for digit recognition
        File digitsGrammar = new File(assetsDir, "grammarFiles/digits.gram");
        mRecognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);

        toUnityLog("end setup recognizer");
    }

    private void post(long delay, Runnable task) {
        mHandler.postDelayed(task, delay);
    }

    public MainActivity() throws IOException
    {

    }
    // движок услышал какой-то звук, может быть это речь (а может быть и нет)
    @Override
    public void onBeginningOfSpeech() {
        UnityPlayer.UnitySendMessage(_recieverObjectName, _recieverMethodName, "onBeginningOfSpeech");
    }
    // звук закончился
    @Override
    public void onEndOfSpeech() {
        UnityPlayer.UnitySendMessage(_recieverObjectName, _recieverMethodName, "onEndOfSpeech");
    }
    // есть промежуточные результаты распознавания. Для активационной фразы это значит, что она сработала. Аргумент Hypothesis содержит данные о распознавании (строка и score)
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
            switchSearch(MENU_SEARCH);
        else if (text.equals(DIGITS_SEARCH))
            switchSearch(DIGITS_SEARCH);
        else
            toUnityLog(hypothesis.getHypstr());
    }
    // конечный результат распознавания. Этот метод будет вызыван после вызова метода stop у SpeechRecognizer. Аргумент Hypothesis содержит данные о распознавании (строка и score)
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            UnityPlayer.UnitySendMessage(_recieverObjectName, _recieverMethodName, "onResult:" + hypothesis.getHypstr());
        }
    }

    @Override
    public void onError(Exception e) {
        toUnityLog(e.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }
}
