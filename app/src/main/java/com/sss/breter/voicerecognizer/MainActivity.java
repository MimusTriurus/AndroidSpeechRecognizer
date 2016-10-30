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
    private static String _logReceiverMethodName;
    // имя метода принимающего callback с результатом распознавания речи
    private static String _recognitionResultMethodNameReciever;

    /**
     * устанавливаем имя Unity объекта - приёмника сообщений из данной библиотеки
     * @param name                     - имя объекта
     */
    public static void setRecieverObjectName(String name){
        _recieverObjectName = name;
    }

    /**
     * устанавливаем имя метода Unity объекта - метода вывода в лог Unity
     * @param name  -   имя метода
     */
    public static void setLogReceiverMethodName(String name){
        _logReceiverMethodName = name;
    }

    /**
     * устанавливаем имя метода Unity объекта - обработчика результатов распознавания
     * @param name  -   имя метода
     */
    public static void setRecognitionResultRecieverMethod(String name)
    {
        _recognitionResultMethodNameReciever = name;
    }

    /**
     * отправка в Unity сообщения для вывода в лог
     * @param message - сообщение
     */
    public  static void toUnityLog(String message)
    {
        UnityPlayer.UnitySendMessage(_recieverObjectName, _logReceiverMethodName, message);
    }

    /**
     * отправка в Unity сообщения с результатами распознавания
     * @param recognitionResult - результат распознавания в рамках последней сессии
     */
    public  static void regonitionResultToUnity(String recognitionResult)
    {
        UnityPlayer.UnitySendMessage(_recieverObjectName, _recognitionResultMethodNameReciever, recognitionResult);
    }
    // ******************************************************

    private SpeechRecognizer _mRecognizer;
    private Map<String, String> _grammarFilesContainer;

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    private static final String DIGITS_SEARCH = "digits";
    private static final String MENU_SEARCH = "menu";

    // базовая папка для акустических моделей(диференциация по битрейту)
    private static final String ACOUSTIC_MODELS_DIR = "acousticModels/";
    // базовая папка для словарей
    private static final String DICTIONARIES_DIR = "dictionaries/";
    // базовая папка для файлов грамматики
    private static final String GRAMMAR_FILES_DIR = "grammarFiles/";

    private String _language = "eng";
    private String _bitrate = "16000";
    private String _dictionaryName = "cmudict-en-us.dict";
    private String _keyword = "computer";
    private String _baseGrammarName = "menu";
    private int _timeoutInterval = 10000;

    /**
     * Конфигурируем mRecognizer
     * @param language - язык
     */
    public void runRecognizerSetup(String language) {

        final String selectedLanguage = language;

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    UnityAssets assets = new UnityAssets(MainActivity.this, selectedLanguage);
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

    /**
     * Конфигурируем mRecognizer
     * @param language      - язык
     * @param bitrate       - битрейт языковой модели
     * @param dictionary    - рабочий словарь с фонемами
     * @param keyword       - ключевое слово
     */
    public void runRecognizerSetup(String language, String bitrate ,String dictionary, String keyword) {

        _language = language;
        _bitrate = bitrate;
        _dictionaryName = dictionary;
        _keyword = keyword;

        runRecognizerSetup(_language);
    }

    /**
     * устанавливает временной интервал сессии распознавания
     * @param interval  -   временной интервал в м.с.
     */
    public void setTimeoutInterval(int interval)
    {
        _timeoutInterval = interval;
    }

    /**
     * устанавливает базовый файл грамматики инициализируемый после распознования ключевого слова
     * @param grammarFileName - имя файла грамматики
     */
    public void setBaseGrammarFile(String grammarFileName)
    {
        _baseGrammarName = grammarFileName;
    }

    /**
     * Переключаем файл грамматики для mRecognizer
     * @param searchName - ключ файла грамматики
     */
    private void switchSearch(String searchName) {
        _mRecognizer.stop();
        if (searchName.equals(KWS_SEARCH))
            _mRecognizer.startListening(searchName);
        else
            _mRecognizer.startListening(searchName, _timeoutInterval);
    }

    private void setupRecognizer(File assetsDir) throws IOException {

        toUnityLog("start setup recognizer");
        _mRecognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, ACOUSTIC_MODELS_DIR + _bitrate))
                .setDictionary(new File(assetsDir, DICTIONARIES_DIR + _dictionaryName))

                //.setRawLogDir(assetsDir) // для включения лога
                .setKeywordThreshold(1e-45f)
                .setBoolean("-allphone_ci", true)


                .getRecognizer();
        _mRecognizer.addListener(this);

        _mRecognizer.addKeyphraseSearch(KWS_SEARCH, _keyword);

        addGrammarSearch(assetsDir);

        toUnityLog("end setup recognizer");
    }

    public MainActivity() throws IOException
    {
        _grammarFilesContainer = new HashMap<String, String>();
    }

    /**
     * метод добавления в словарь пути до файла грамматики
     * @param searchName  - ключ по которому хранится местоположение файла грамматики,
     *                    ключ по которому инициализируется файл грамматики для mRecognizer
     * @param destination - местоположение файла грамматики в externalStorage
     */
    public void addGrammarFile(String searchName, String destination)
    {
        if (_grammarFilesContainer != null)
        {
            _grammarFilesContainer.put(searchName, GRAMMAR_FILES_DIR + destination);
            toUnityLog("add grammar:" + searchName + " destination");
        }
    }

    /**
     * добавляем в mRecognizer все файлы грамматики из словаря
     * @param assetsDir - место хранения скопированых ассетов в externalStorage
     */
    private void addGrammarSearch(File assetsDir)
    {
        for (Map.Entry entry : _grammarFilesContainer.entrySet()) {
            File grammar = new File(assetsDir, entry.getValue().toString());
            if (grammar.exists())
                _mRecognizer.addGrammarSearch(entry.getKey().toString(), grammar);
        }
    }

    /**
     * движок услышал какой-то звук, может быть это речь (а может быть и нет)
     */
    @Override
    public void onBeginningOfSpeech() {
        //UnityPlayer.UnitySendMessage(_recieverObjectName, _recieverMethodName, "onBeginningOfSpeech");
    }

    /**
     * микрофон больше не фиксирует звуки
     */
    @Override
    public void onEndOfSpeech() {
        //UnityPlayer.UnitySendMessage(_recieverObjectName, _recieverMethodName, "onEndOfSpeech");
    }

    /**
     * есть промежуточные результаты распознавания. Для активационной фразы это значит, что она сработала. Аргумент Hypothesis содержит данные о распознавании (строка и score)
     * @param hypothesis
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(_keyword))
            switchSearch(_baseGrammarName);
        else
        {
            if (_grammarFilesContainer.containsKey(text))
            {
                switchSearch(text);
            }
        }
            //if (text.equals(DIGITS_SEARCH))
            //switchSearch(DIGITS_SEARCH);
        //else
            //toUnityLog(hypothesis.getHypstr());
    }
    //

    /**
     * Конечный результат распознавания. Этот метод будет вызыван после вызова метода stop у SpeechRecognizer.
     * Аргумент Hypothesis содержит данные о распознавании (строка и score).
     * @param hypothesis    - строка с распознанными значениями за сессию
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            //UnityPlayer.UnitySendMessage(_recieverObjectName, _recognitionResultMethodNameReciever, "result:" + hypothesis.getHypstr());
            regonitionResultToUnity("result:" + hypothesis.getHypstr());
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
