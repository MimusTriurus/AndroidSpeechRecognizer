package com.sss.breter.voicerecognizer;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    // *********** секция взаимодествия с Unity *************
    // имя объекта принимающего callback из этой библиотеки
    private static String _recieverObjectName;
    // имя метода принимающего callback-log из этой библиотеки
    private static String _recieverMethodName;
    // имя метода принимающего callback с результатом распознавания речи
    private static String _recognitionResultMethodNameReciever;

    public static void setRecieverObjectName(String name){
        _recieverObjectName = name;
    }

    public static void setRecieverMethodName(String name){
        _recieverMethodName = name;
    }

    public static void setRecognitionResultRecieverMethod(String name)
    {
        _recognitionResultMethodNameReciever = name;
    }

    public  static void toUnityLog(String message)
    {
        UnityPlayer.UnitySendMessage(_recieverObjectName, _recieverMethodName, message);
    }
    // ******************************************************

    //private final Handler mHandler = new Handler();

    private SpeechRecognizer mRecognizer;
    private Map<String, String> _grammarFilesContainer;

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    private static final String DIGITS_SEARCH = "digits";
    private static final String MENU_SEARCH = "menu";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "oh mighty computer";

    public void runRecognizerSetup(String language) {

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    //Assets assets = new Assets(MainActivity.this);
                    UnityAssets assets = new UnityAssets(MainActivity.this, "eng");
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

        addGrammarSearch(assetsDir);

        toUnityLog("end setup recognizer");
    }

    //private void post(long delay, Runnable task) {
        //mHandler.postDelayed(task, delay);
    //}

    public MainActivity() throws IOException
    {
        _grammarFilesContainer = new HashMap<String, String>();
    }

    public void addGrammarFile(String searchName, String destination)
    {
        if (_grammarFilesContainer != null)
        {
            _grammarFilesContainer.put(searchName, destination);
            toUnityLog("add grammar:" + searchName + " destination");
        }
    }

    private void addGrammarSearch(File assetsDir)
    {
        for (Map.Entry entry : _grammarFilesContainer.entrySet()) {
            File grammar = new File(assetsDir, entry.getValue().toString());
            if (grammar.exists())
                mRecognizer.addGrammarSearch(entry.getKey().toString(), grammar);
        }
    }

    // движок услышал какой-то звук, может быть это речь (а может быть и нет)
    @Override
    public void onBeginningOfSpeech() {
        //UnityPlayer.UnitySendMessage(_recieverObjectName, _recieverMethodName, "onBeginningOfSpeech");
    }
    // звук закончился
    @Override
    public void onEndOfSpeech() {
        //UnityPlayer.UnitySendMessage(_recieverObjectName, _recieverMethodName, "onEndOfSpeech");
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
        //else
            //toUnityLog(hypothesis.getHypstr());
    }
    // конечный результат распознавания. Этот метод будет вызыван после вызова метода stop у SpeechRecognizer. Аргумент Hypothesis содержит данные о распознавании (строка и score)
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            UnityPlayer.UnitySendMessage(_recieverObjectName, _recognitionResultMethodNameReciever, "result:" + hypothesis.getHypstr());
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
