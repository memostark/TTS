package com.guillermonegrete.tts.TextProcessing.domain.interactors;

import com.guillermonegrete.tts.AbstractInteractor;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.TextProcessing.ProcessTextLayoutType;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.data.source.WordRepositorySource;
import com.guillermonegrete.tts.db.Words;


public class GetLayout extends AbstractInteractor implements GetLayoutInteractor {

    private GetLayoutInteractor.Callback mCallback;
    private WordRepository mRepository;
    private String mText;

    public GetLayout(Executor threadExecutor, MainThread mainThread, Callback callback, WordRepository repository, String text){
        super(threadExecutor, mainThread);
        mCallback = callback;
        mText = text;
        mRepository = repository;
    }

    @Override
    public void run() {
        System.out.print("Get layout implementation run method");
        String[] splittedText = mText.split(" ");
        if(splittedText.length > 1){
            System.out.print("Get sentence layout");
            // Get translation, wait for callback
            mRepository.getLanguageAndTranslation(mText, new WordRepositorySource.GetTranslationCallback() {
                @Override
                public void onTranslationAndLanguage(Words word) {
                    mCallback.onLayoutDetermined(word, ProcessTextLayoutType.SENTENCE_TRANSLATION);
                }

                @Override
                public void onDataNotAvailable() {

                }
            });
        }else{
            // Search in database
            System.out.print("Request layouts");
            mRepository.getWordLanguageInfo(mText, new WordRepositorySource.GetWordRepositoryCallback() {
                @Override
                public void onLocalWordLoaded(Words word) {
                    mCallback.onLayoutDetermined(word, ProcessTextLayoutType.SAVED_WORD);
                }

                @Override
                public void onRemoteWordLoaded(Words word) {
                    mCallback.onLayoutDetermined(word, ProcessTextLayoutType.WORD_TRANSLATION);
                }

                @Override
                public void onDataNotAvailable() {

                }
            });
        }

    }
}