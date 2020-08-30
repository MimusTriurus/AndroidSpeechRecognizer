package com.sss.breter.voicerecognizer;

import android.content.Context;
import android.os.AsyncTask;

import com.sss.breter.voicerecognizer.recognizer.UnityAssets;
import com.unity3d.player.UnityPlayer;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class MainActivity implements RecognitionListener {
    /**
     * устанавливаем имя Unity объекта - приёмника сообщений из данной библиотеки
     * @param name                     - имя объекта
     */
    public static void setRecieverObjectName( String name ){
        toUnityLog("setRecieverObjectName " + name);
        _recieverObjectName = name;
    }
    /**
     * устанавливаем имя метода Unity объекта - метода вывода в лог Unity
     * @param name  -   имя метода
     */
    public static void setLogReceiverMethodName( String name ){
        _logReceiverMethodName = name;
    }
    /**
     * устанавливаем имя метода Unity объекта - обработчика результатов распознавания
     * @param name  -   имя метода
     */
    public static void setRecognitionResultRecieverMethod( String name ) { _recognitionResultMethodNameReciever = name; }
    /**
     * устанавливаем имя метода Unity объекта - обработчика сообщения об окончании инициализации распознавателя
     * @param name - имя метода
     */
    public static void setInitializationCompleteMethod( String name ) { _initializationCompleteMethodNameReciever = name; }
    /**
     * устанавливаем имя метода Unity объекта - обработчика сообщения об ошибке в работе библиотеки
     * @param name
     */
    public static void setCrashMessageRecieverMethod( String name ) { _crashMessRecieverMethodName = name; }
    /**
     * Начинаем распознавание речи
     */
    public void startListening( ) {
        if ( _mRecognizer == null ) {
            crashMessToUnity( "Recognizer is null" );
            return;
        }
        Boolean start = false;
        if ( _useKeyword )
            start = _mRecognizer.startListening( KEYPHRASE_SEARCH );
        else
            start = _mRecognizer.startListening( _baseGrammarName, _timeoutInterval );
    }
    /**
     * Прекращаем распознвание речи
     */
    public void stopListening( ) {
        if ( _mRecognizer != null )
            _mRecognizer.stop( );
    }
    /**
     * Конфигурируем mRecognizer
     * @param language - язык
     */
    public void runRecognizerSetup( String language ) throws IOException {
        toUnityLog( "runRecognizerSetup" );
        language = ACOUSTIC_MODELS_DIR + language;
        final String selectedLanguage = language;
        new AsyncTask<Void, Void, File>( ) {
            @Override
            protected File doInBackground( Void... params ) {
                File assetDir = null;
                try {
                    toUnityLog( "Acoustic model is being loaded..." );
                    UnityAssets assets = new UnityAssets( _context, selectedLanguage );
                    assetDir = assets.syncAssets( );
                } catch ( IOException e ) {
                    crashMessToUnity( e.getMessage( ) );
                    initializationResult( INIT_RESULT_FALSE );
                    return null;
                }
                return assetDir;
            }
            @Override
            protected void onPostExecute( File assets ) {
                if ( assets != null ) {
                    try {
                        setupRecognizer( assets );
                    } catch ( IOException e ) {
                        e.printStackTrace( );
                        crashMessToUnity( e.getMessage( ) );
                    }
                    initializationResult( INIT_RESULT_TRUE );
                } else {
                    initializationResult( INIT_RESULT_FALSE );
                    crashMessToUnity( "Failed to init speech recognizer " + assets );
                }
            }
        }.execute( );
    }
    /**
     * устанавливает временной интервал сессии распознавания
     * @param interval  -   временной интервал в м.с.
     */
    public void setTimeoutInterval( int interval ) {
        _timeoutInterval = interval;
    }
    /**
     * устанавливает базовый файл грамматики инициализируемый после распознования ключевого слова
     * @param grammarFileName - имя файла грамматики
     */
    public void setBaseGrammarFile( String grammarFileName ) {
        _baseGrammarName = grammarFileName;
    }
    /**
     * Переключаем файл грамматики для mRecognizer
     * @param searchName - ключ файла грамматики
     */
    public void switchSearch( String searchName ) {
        try {
            _mRecognizer.stop( );
            _baseGrammarName = searchName;
            _mRecognizer.startListening( searchName, _timeoutInterval );
        }
        catch ( Exception e ) {
            crashMessToUnity("crash on switch search:" + searchName);
        }
    }

    public void setupRecognizer( File assetsDir ) throws IOException {
        _mRecognizer = SpeechRecognizerSetup.defaultSetup( )
                .setAcousticModel( assetsDir )
                .setKeywordThreshold( _threshold )
                .setBoolean( "-allphone_ci", true )
                .getRecognizer( );
        _mRecognizer.addListener( this );
    }

    public MainActivity( ) throws IOException {

    }
    public MainActivity( Context context ) throws IOException {
        _context = context;
    }
    /**
     * движок услышал какой-то звук, может быть это речь (а может быть и нет)
     */
    @Override
    public void onBeginningOfSpeech( ) {
        //toUnityLog("start of speech");
    }
    /**
     * микрофон больше не фиксирует звуки
     */
    @Override
    public void onEndOfSpeech( ) {
        //toUnityLog("end of speech");
    }
    /**
     * есть промежуточные результаты распознавания. Для активационной фразы это значит, что она сработала.
     * Аргумент Hypothesis содержит данные о распознавании (строка и score)
     * @param hypothesis
     */
    @Override
    public void onPartialResult( Hypothesis hypothesis ) {
        if ( hypothesis == null )
            return;
        String partialResult = hypothesis.getHypstr();
        if ( !_useKeyword ) return;
        String keyword = _mRecognizer.getDecoder().getKws(KEYPHRASE_SEARCH);
        if ( keyword.equals( partialResult ) )
            regonitionResultToUnity( partialResult );
    }
    /**
     * Конечный результат распознавания. Этот метод будет вызыван после вызова метода stop у SpeechRecognizer.
     * Аргумент Hypothesis содержит данные о распознавании (строка и score).
     * @param hypothesis    - строка с распознанными значениями за сессию
     */
    @Override
    public void onResult( Hypothesis hypothesis ) {
        if ( hypothesis != null ) {
            regonitionResultToUnity( hypothesis.getHypstr( ) );
        }
    }
    @Override
    public void onError( Exception e ) {
        crashMessToUnity( e.getMessage( ) );
    }
    @Override
    public void onTimeout( ) {
        switchSearch( _baseGrammarName );
    }
    /**
     * добавляем слово в словарь
     * @param word     слово
     * @param phones   транскрипция
     * @return          рузультат добавления
     */
    public boolean addWordIntoDictionary( String word, String phones ) {
        try {
            if (_mRecognizer.getDecoder( ) != null)
                _mRecognizer.getDecoder( ).addWord( word, phones, 1 );
            else
                crashMessToUnity( "addWordIntoDictionary. Decoder not found" );
        }
        catch ( Exception e ) {
            crashMessToUnity( "crash on add word:" + e.getMessage( ) );
            return false;
        }
        return true;
    }
    /**
     * добавляем формализованную строку с грамматикой
     * @param grammarName      имя грамматики
     * @param grammarString    формализованная строка грамматики
     * @return                  результат добавления
     */
    public boolean addGrammarString( String grammarName, String grammarString ) {
        try {
            if ( _baseGrammarName == null ) _baseGrammarName = grammarName;
            _mRecognizer.addGrammarSearch( grammarName, grammarString );
        }
        catch ( Exception e ) {
            crashMessToUnity( "crash on add grammar string:" + e.getMessage( ) );
            return false;
        }
        return true;
    }
    /**
     * устанавливаем ключевое слово для поиска
     * @param keyword ключевое слово
     * @return         результат добавления
     */
    public boolean setKeyword( String keyword ) {
        _useKeyword = true;
        try {
            //toUnityLog("try set keyword:" + pKeyword);
            _mRecognizer.addKeyphraseSearch( KEYPHRASE_SEARCH, keyword );
        }
        catch ( Exception e ) {
            crashMessToUnity( "crash on set keyword:" + keyword );
            return false;
        }
        return true;
    }
    /**
     * инициируем поиск ключевого слова
     * @return успешно\нет
     */
    public boolean setSearchKeyword( ) {
        try {
            toUnityLog( "try set search keyword" );
            if ( _useKeyword ) {
                _mRecognizer.stop( );
                _mRecognizer.startListening( KEYPHRASE_SEARCH );
            }
        }
        catch ( Exception e ) {
            crashMessToUnity( "crash on set search keyword" );
            return false;
        }
        return true;
    }
    /**
     * устанавливаем порог срабатывания для ключевой фразы
     * @param threshold порог срабатывания
     */
    public void setThreshold( float threshold ) {
        _threshold = threshold;
    }
    /**
     * отправка в Unity сообщения для вывода в лог
     * @param message - сообщение
     */
    private static void toUnityLog( String message ) {
        if ( ( _recieverObjectName != null ) && ( _logReceiverMethodName != null ) )
            UnityPlayer.UnitySendMessage( _recieverObjectName, _logReceiverMethodName, message );
    }
    /**
     * отправка в Unity сообщения с результатами распознавания
     * @param recognitionResult - результат распознавания в рамках последней сессии
     */
    private static void regonitionResultToUnity( String recognitionResult ) {
        if ( ( _recieverObjectName != null ) && ( _recognitionResultMethodNameReciever != null ) )
            UnityPlayer.UnitySendMessage( _recieverObjectName, _recognitionResultMethodNameReciever, recognitionResult );
    }
    /**
     * отправка в Unity сообщения с результатами инициализации mRecognizer
     * @param message
     */
    private static void initializationResult( String message ) {
        if ( ( _recieverObjectName != null ) && ( _initializationCompleteMethodNameReciever != null ) )
            UnityPlayer.UnitySendMessage( _recieverObjectName, _initializationCompleteMethodNameReciever, message );
    }
    /**
     * отправка в UNity сообщения об ошибке в работе библиотеке
     * @param crash
     */
    private static void crashMessToUnity( String crash ) {
        if ( ( _recieverObjectName != null ) && ( _crashMessRecieverMethodName != null ) )
            UnityPlayer.UnitySendMessage( _recieverObjectName, _crashMessRecieverMethodName, crash );
    }

    private SpeechRecognizer _mRecognizer;

    private static final String ACOUSTIC_MODELS_DIR = "acousticModels/";
    private static final String KEYPHRASE_SEARCH    = "keyphrase_search";
    private static final String INIT_RESULT_TRUE    = "true";
    private static final String INIT_RESULT_FALSE    = "false";

    private String _baseGrammarName = null;
    private int _timeoutInterval = 1000;
    private boolean _useKeyword = false;
    private float _threshold = 1e-45f;

    private Context _context;
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
     * имя метода принимающего колбэк с сообщением об ошибках
     */
    private static String _crashMessRecieverMethodName = null;
}
