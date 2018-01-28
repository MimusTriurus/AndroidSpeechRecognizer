# AndroidSpeechRecognizer
Library of off-line speech recognizing for Android (on the basis of library pocketsphinx).
---------
The library has been created for future integration to the projects on Unity 5 by means of using wrapper (connector) UnitySpeechRecognizer ( https://github.com/MimusTriurus/UnitySpeechRecognizer ).

System requirements:
---------
1. Android SDK version 24;
2. IDE IntelliJ IDEA 2016.2.4;
3. JRE 1.8.0-91-b15amd64

Assembling:
---------
1. Clone the repository;
2. Open local.properties using text editing program. Specify the path to Android SDK in the field sdk.dir and save the file;
3. Open the project. Indicate the target directory for library in file build.gradle (AndroidSpeechRecognizer\app\build.gradle) in field into() of method task exportJar(type: Copy);
4. Run the script exportJar (AndroidSpeechRecognizer\:app\Tasks\other\exportJar).

Compiled library and files required for integration Unity are located in  <ProjectDir>/build/release
