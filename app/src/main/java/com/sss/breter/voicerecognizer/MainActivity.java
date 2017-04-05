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

    // базовая папка для акустических моделей(диференциация по битрейту)
    private static final String ACOUSTIC_MODELS_DIR = "acousticModels/";
    private static final String KEYPHRASE_SEARCH = "keyphrase_search";

    private String _bitrate = "16000";
    private String _baseGrammarName = null;
    private int _timeoutInterval = 1000;
    private boolean _useKeyword = false;
    private float _threshold = 1e-45f;

    public void startListening()
    {
        if (_mRecognizer == null) return;
        toUnityLog("try start listening");
        if (_useKeyword)
            _mRecognizer.startListening(KEYPHRASE_SEARCH);
        else
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
                    initializationResult("initComplete");
                }
            }
        }.execute();
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
    public void switchSearch(String searchName)
    {
        try
        {
            //toUnityLog("try switch search to:" + searchName);
            _mRecognizer.stop();
            //_mRecognizer.getDecoder().setSearch(searchName);
            _baseGrammarName = searchName;
            _mRecognizer.startListening(searchName, _timeoutInterval);
        }
        catch (Exception e)
        {
            toUnityLog("crash on switch search:" + searchName);
        }
    }

    private void setupRecognizer(File assetsDir) throws IOException {

        toUnityLog("start setup recognizer");
        _mRecognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, ACOUSTIC_MODELS_DIR + _bitrate))
                .setKeywordThreshold(_threshold)
                .setBoolean("-allphone_ci", true)
                .getRecognizer();
        toUnityLog("before add listener");
        _mRecognizer.addListener(this);

        toUnityLog("end setup recognizer");
    }

    public MainActivity() throws IOException
    {

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

        String keyword = _mRecognizer.getDecoder().getKws(KEYPHRASE_SEARCH);
        if (keyword.equals(partialResult))
            regonitionResultToUnity(partialResult);
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
        }
    }

    @Override
    public void onError(Exception e) {
        toUnityLog(e.getMessage());
    }

    @Override
    public void onTimeout() {
        //toUnityLog("timeout");
        switchSearch(_baseGrammarName);
    }
    /**
     * добавляем слово в словарь
     * @param pWord     слово
     * @param pPhones   транскрипция
     * @return          рузультат добавления
     */
    public boolean addWordIntoDictionary(String pWord, String pPhones)
    {
        try
        {
            toUnityLog("try add word:" + pWord + " phones:" + pPhones);
            if (_mRecognizer.getDecoder() != null)
                _mRecognizer.getDecoder().addWord(pWord, pPhones, 1);
            else
                toUnityLog("Decoder not found");
        }
        catch (Exception e)
        {
            toUnityLog("crash on add word:" + e.getMessage());
            return false;
        }
        return true;
    }
    /**
     * добавляем формализованную строку с грамматикой
     * @param pGrammarName      имя грамматики
     * @param pGrammarString    формализованная строка грамматики
     * @return                  результат добавления
     */
    public boolean addGrammarString(String pGrammarName, String pGrammarString)
    {
        try
        {
            toUnityLog("try add grammar string:" + pGrammarString);
            if (_baseGrammarName == null) _baseGrammarName = pGrammarName;
            _mRecognizer.addGrammarSearch(pGrammarName, pGrammarString);
        }
        catch (Exception e)
        {
            toUnityLog("crash on add grammar string:" + e.getMessage());
            return false;
        }
        return true;
    }
    /**
     * устанавливаем ключевое слово для поиска
     * @param pKeyword ключевое слово
     * @return         результат добавления
     */
    public boolean setKeyword(String pKeyword)
    {
        _useKeyword = true;
        try
        {
            //toUnityLog("try set keyword:" + pKeyword);
            _mRecognizer.addKeyphraseSearch(KEYPHRASE_SEARCH, pKeyword);
        }
        catch (Exception e)
        {
            toUnityLog("crash on set keyword:" + pKeyword);
            return false;
        }

        return true;
    }
    /**
     * инициируем поиск ключевого слова
     * @return успешно\нет
     */
    public boolean setSearchKeyword()
    {
        try
        {
            toUnityLog("try set search keyword");
            //_mRecognizer.getDecoder().setSearch(KEYPHRASE_SEARCH);
            if (_useKeyword) {
                _mRecognizer.stop();
                _mRecognizer.startListening(KEYPHRASE_SEARCH);
            }
        }
        catch (Exception e)
        {
            toUnityLog("crash on set search keyword");
            return false;
        }
        return true;
    }
    /**
     * устанавливаем порог срабатывания для ключевой фразы
     * @param pThreshold порог срабатывания
     */
    public void setThreshold(float pThreshold)
    {
        _threshold = pThreshold;
    }
}
