package com.sss.breter.voicerecognizer;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Scanner;

import android.os.AsyncTask;

import com.sss.breter.voicerecognizer.recognizer.UnityAssets;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import edu.cmu.pocketsphinx.*;


public class MainActivity extends UnityPlayerActivity implements RecognitionListener {

    // *********** секция взаимодествия с Unity *************
    /**
     * имя объекта принимающего callback из этой библиотеки
      */
    private static String _recieverObjectName = null;
    /**
     * имя метода принимающего callback-log из этой библиотеки
     */
    private static String _logReceiverMethodName = null;
    /**
     * имя метода принимающего callback с результатом распознавания речи
     */
    private static String _recognitionResultMethodNameReciever = null;
    /**
     * имя метода принимающего callback с промежуточным результатом распознования речи
     */
    private static String _recognitionPartialResultMethodNameReciever = null;
    /**
     * имя метода принимающего колбэк о завершении инициализиции распознователя
     */
    private static String _initializationCompleteMethodNameReciever = null;

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
     * устанавливаем имя метода Unity объекта - обработчика промежуточных результатов распознавания
     * @param name - имя метода
     */
    public static void setRecognitionPartialResultRecieverMethod(String name) { _recognitionPartialResultMethodNameReciever = name; }

    /**
     * устанавливаем имя метода Unity объекта - обработчика сообщения об окончании инициализации распознавателя
     * @param name - имя метода
     */
    public static void setInitializationCompleteMethod(String name) { _initializationCompleteMethodNameReciever = name; }

    /**
     * отправка в Unity сообщения для вывода в лог
     * @param message - сообщение
     */
    private static void toUnityLog(String message)
    {
        if ((_recieverObjectName != null) & (_logReceiverMethodName != null))
            UnityPlayer.UnitySendMessage(_recieverObjectName, _logReceiverMethodName, message);
    }

    /**
     * отправка в Unity сообщения с результатами распознавания
     * @param recognitionResult - результат распознавания в рамках последней сессии
     */
    private static void regonitionResultToUnity(String recognitionResult)
    {
        if ((_recieverObjectName != null) & (_recognitionResultMethodNameReciever != null))
            UnityPlayer.UnitySendMessage(_recieverObjectName, _recognitionResultMethodNameReciever, recognitionResult);
    }

    /**
     * отправка в Unity сообщения с промежуточными результатами распознавания
     * @param recognitionPartialResult
     */
    private static void regonitionPartialResultToUnity(String recognitionPartialResult)
    {
        if ((_recieverObjectName != null) & (_recognitionPartialResultMethodNameReciever != null))
            UnityPlayer.UnitySendMessage(_recieverObjectName, _recognitionPartialResultMethodNameReciever, recognitionPartialResult);
    }
    /**
     * отправка в Unity сообщения с результатами инициализации mRecognizer
     * @param message
     */
    private static void initializationResult(String message)
    {
        if ((_recieverObjectName != null) & (_initializationCompleteMethodNameReciever != null))
            UnityPlayer.UnitySendMessage(_recieverObjectName, _initializationCompleteMethodNameReciever, message);
    }
    // ******************************************************

    private SpeechRecognizer _mRecognizer;
    private Map<String, String> _grammarFilesContainer;

    // базовая папка для акустических моделей(диференциация по битрейту)
    private static final String ACOUSTIC_MODELS_DIR = "acousticModels/";
    // базовая папка для словарей
    private static final String DICTIONARIES_DIR = "dictionaries/";
    // базовая папка для файлов грамматики
    private static final String GRAMMAR_FILES_DIR = "grammarFiles/";

    private String _bitrate = "16000";
    private String _dictionaryName = "actualDictionary.dict";
    private String _baseGrammarName = null;
    private int _timeoutInterval = 5000;

    public void startListening()
    {
        if (_mRecognizer != null)
            _mRecognizer.startListening(_baseGrammarName, _timeoutInterval);
    }

    public void stopListening()
    {
        if (_mRecognizer != null)
            _mRecognizer.stop();
    }

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
                    if (_baseGrammarName == null)
                        getBaseGrammarName();
                    //switchSearch(KWS_SEARCH);
                    //switchSearch(_baseGrammarName);
                    toUnityLog(_baseGrammarName);
                    initializationResult("init complete");
                }
            }
        }.execute();
    }

    private void getBaseGrammarName()
    {
        Set<String> mapKeys = _grammarFilesContainer.keySet();
        // получаем первый файл грамматики из списка
        if (mapKeys.size() != 0)
            _baseGrammarName = mapKeys.toArray()[0].toString();
        else
            toUnityLog("grammarFilesContainer is empty!!!");
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
    public void switchSearch(String searchName) {
        try
        {
            _mRecognizer.stop();
        }
        catch (Exception e)
        {
            toUnityLog("error on stop listening:" + e.getMessage());
            return;
        }
        if (_grammarFilesContainer.containsKey(searchName))
            _baseGrammarName = searchName;
        try
        {
            _mRecognizer.startListening(_baseGrammarName, _timeoutInterval);
        }
        catch (Exception e)
        {
            toUnityLog("error on start listening:" + e.getMessage());
            return;
        }
    }

    private void setupRecognizer(File assetsDir) throws IOException {

        toUnityLog("start setup recognizer");
        //readFile(assetsDir + "/" + DICTIONARIES_DIR + _dictionaryName);
        _mRecognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, ACOUSTIC_MODELS_DIR + _bitrate))
                .setDictionary(new File(assetsDir, DICTIONARIES_DIR + _dictionaryName))
                //.setRawLogDir(assetsDir) // для включения лога
                .setKeywordThreshold(1e-45f)
                .setBoolean("-allphone_ci", true)

                .getRecognizer();
        _mRecognizer.addListener(this);

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
            //toUnityLog("add grammar:" + searchName);
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
                try {
                    //readFile(grammar.getAbsolutePath());
                    _mRecognizer.addGrammarSearch(entry.getKey().toString(), grammar);
                }
                catch (Exception e)
                {
                    toUnityLog("error on add .gram:" + e.getMessage());
                    return;
                }
        }
    }

    /**
     * движок услышал какой-то звук, может быть это речь (а может быть и нет)
     */
    @Override
    public void onBeginningOfSpeech() {
        //UnityPlayer.UnitySendMessage(_recieverObjectName, _recieverMethodName, "onBeginningOfSpeech");
        //toUnityLog("start of speech");
    }

    /**
     * микрофон больше не фиксирует звуки
     */
    @Override
    public void onEndOfSpeech() {
        //UnityPlayer.UnitySendMessage(_recieverObjectName, _recieverMethodName, "onEndOfSpeech");
        //toUnityLog("end of speech");
    }

    /**
     * есть промежуточные результаты распознавания. Для активационной фразы это значит, что она сработала. Аргумент Hypothesis содержит данные о распознавании (строка и score)
     * @param hypothesis
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {

        if (hypothesis == null)
            return;

        String partialResult = hypothesis.getHypstr();

        regonitionPartialResultToUnity(partialResult);
    }
    /**
     * Конечный результат распознавания. Этот метод будет вызыван после вызова метода stop у SpeechRecognizer.
     * Аргумент Hypothesis содержит данные о распознавании (строка и score).
     * @param hypothesis    - строка с распознанными значениями за сессию
     */
    @Override
    public void onResult(Hypothesis hypothesis) {

        if (hypothesis != null) {
            regonitionResultToUnity(hypothesis.getHypstr());
            switchSearch(_baseGrammarName);
        }
    }

    @Override
    public void onError(Exception e) {
        toUnityLog(e.getMessage());
    }

    @Override
    public void onTimeout() {
        toUnityLog("timeout");
        switchSearch(_baseGrammarName);
    }

    public static void readFile(String destination) throws FileNotFoundException {
        String [] rows = new Scanner(new File(destination)).useDelimiter("\\Z").next().split("\n");
        for ( String s : rows )
            toUnityLog(s);
    }
}
