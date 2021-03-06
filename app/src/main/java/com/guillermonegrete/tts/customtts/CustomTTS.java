package com.guillermonegrete.tts.customtts;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import android.speech.tts.UtteranceProgressListener;

import androidx.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dagger.hilt.android.qualifiers.ApplicationContext;


@Singleton
public class CustomTTS implements TextToSpeech.OnInitListener, TTS{

    private TextToSpeech localTTS;

    private boolean localInitialized = false;
    private Boolean langInitialized;
    private Boolean isShutdown = false;

    private String language;

    private Listener listener = null;

    private final HashMap<String, String> map = new HashMap<>();
    private final Bundle params = new Bundle();

    private final Context context;

    @Inject
    public CustomTTS(@ApplicationContext Context context){
        this.context = context;

        localTTS = new TextToSpeech(this.context, this);
        langInitialized = false;
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "CustomTTSID");
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");
    }

    private void speak(String text){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            localTTS.speak(text, TextToSpeech.QUEUE_FLUSH, params,"CustomTTSID");
        } else {
            localTTS.speak(text, TextToSpeech.QUEUE_FLUSH, map);
        }
    }

    public void speak(String text, Listener listener){
        this.listener = listener;
        if(langInitialized) speak(text);
        else listener.onLanguageUnavailable();
    }

    public void stop(){
        localTTS.stop();
    }

    public void removeListener(Listener listener){
        if(listener == this.listener){
            this.listener = null;
        }
    }

     public void initializeTTS(String langCode, Listener listener) {
        boolean ttsReady = langInitialized && language.equals(langCode);

        if(!ttsReady){
            this.listener = listener;
            language = langCode;
            langInitialized = false;

            initializeGoogleLocalService(langCode);
            localTTS.setOnUtteranceProgressListener(new CustomUtteranceListener());

        } else {
            if(listener != null) listener.onEngineReady();
        }
    }

    private void initializeGoogleLocalService(String langCode){
        if(isShutdown){
            // Recreate and set language on init method
            localTTS = new TextToSpeech(context.getApplicationContext(), this);
        } else {
            if(localInitialized) setLocalLanguage(langCode);
        }
    }

    @Override
    public void onInit(int status) {
        String message;
        switch (status){
            case TextToSpeech.ERROR:
                message = "Error";
                break;
            case TextToSpeech.SUCCESS:
                message = "Success";

                // If is shutdown, it was called from initializeGoogleLocalService method, set language
                // or if language is different from null, initializeTTS() was called but onInit wasn't ready, then set language
                if(isShutdown || language != null) setLocalLanguage(language);
                isShutdown = false;
                localInitialized = true;
                break;
            default:
                message = "Unknown";
                break;
        }
        System.out.println("Local TTS Status: " + message);
    }

    private void setLocalLanguage(String langCode){

        int result = localTTS.setLanguage(new Locale(langCode.toUpperCase()));
        if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
            System.out.println("Error code: " + result);
            System.out.println("Initialize TTS Error, This Language is not supported: " + langCode);
            if(listener != null) listener.onLanguageUnavailable();

        } else {
            langInitialized = true;

            if(listener != null) listener.onEngineReady();
        }
    }

    private class CustomUtteranceListener extends UtteranceProgressListener {

        @Override
        public void onStart(String utteranceId) {
            listener.onSpeakStart();
        }

        @Override
        public void onDone(String utteranceId) {
            listener.onSpeakDone();
        }

        @Override
        public void onError(String utteranceId) {
            listener.onLanguageUnavailable();
        }

        @Override
        public void onError(String utteranceId, int errorCode) {
            super.onError(utteranceId, errorCode);
            String error;
            switch (errorCode){
                case TextToSpeech.ERROR_INVALID_REQUEST:
                    error = "ERROR_INVALID_REQUEST";
                    break;
                case TextToSpeech.ERROR_NETWORK:
                    error = "ERROR_NETWORK";
                    break;
                case TextToSpeech.ERROR_NETWORK_TIMEOUT:
                    error = "ERROR_NETWORK_TIMEOUT";
                    break;
                case TextToSpeech.ERROR_NOT_INSTALLED_YET:
                    error = "ERROR_NOT_INSTALLED_YET";
                    break;
                case TextToSpeech.ERROR_OUTPUT:
                    error = "ERROR_OUTPUT";
                    break;
                case TextToSpeech.ERROR_SERVICE:
                    error = "ERROR_SERVICE";
                    break;
                case TextToSpeech.ERROR_SYNTHESIS:
                    error = "ERROR_SYNTHESIS";
                    break;
                default:
                    error = "Unknown";
                    break;
            }
            System.out.println("Text To speech error: " + error);
        }
    }

    public String getLanguage() {
        return language;
    }

    @NonNull
    @Override
    public List<Locale> getAvailableLanguages(){
        Locale[] locales = Locale.getAvailableLocales();
        Set<String> localeSet = new HashSet<>();
        List<Locale> localeList = new ArrayList<>();

        for (Locale locale : locales) {
            int res = localTTS.isLanguageAvailable(locale);
            if (res == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                String lang = locale.getLanguage();
                if(!localeSet.contains(lang)){
                    localeSet.add(lang);
                    localeList.add(locale);
                }
            }
        }

        return localeList;
    }

    public void finishTTS(){
        System.out.println("Destroying localTTS");
        langInitialized = false;
        localInitialized = false;
        isShutdown = true;
        if(localTTS != null){
            localTTS.stop();
            localTTS.shutdown();
        }
    }

    public interface Listener{
        void onEngineReady();

        void onLanguageUnavailable();

        void onSpeakStart();

        void onSpeakDone();

        void onError();
    }
}
