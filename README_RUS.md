# AndroidSpeechRecognizer
Библиотека офлайн распознавания речи для Android. В основе - библиотека pocketsphinx.
---------
Библиотека создавалась в расчете на последующую интеграцию в проекты на Unity 5 за счет использования обертки(коннектора) UnitySpeechRecognizer ( https://github.com/MimusTriurus/UnitySpeechRecognizer ).

Системные требования:
1. Android SDK version 24;
2. IDE IntelliJ IDEA 2016.2.4;
3. JRE 1.8.0-91-b15amd64

Сборка:
1. Склонировать репозиторий;
2. Открыть local.properties в текстовом редакторе. Указать путь до Android SDK в поле sdk.dir и сохранить файл;
3. Открыть проект. В файле build.gradle (AndroidSpeechRecognizer\app\build.gradle) в поле into() метода task exportJar(type: Copy) указать целевую директорию для библиотеки;
4. Запустить сценарий exportJar (AndroidSpeechRecognizer\:app\Tasks\other\exportJar).

Скомпилированная библиотека и необходимые для интеграции в Unity файлы находятся в <папка с проектом>/build/release
