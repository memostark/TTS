package com.guillermonegrete.tts.textprocessing;

import android.content.SharedPreferences;

import com.guillermonegrete.tts.customtts.CustomTTS;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.TestThreadExecutor;
import com.guillermonegrete.tts.data.source.ExternalLinksDataSource;
import com.guillermonegrete.tts.db.ExternalLink;
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.data.source.DictionaryDataSource;
import com.guillermonegrete.tts.data.source.DictionaryRepository;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.data.source.WordRepositorySource;
import com.guillermonegrete.tts.data.source.local.DatabaseExternalLinksSource;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryItem;
import com.guillermonegrete.tts.threading.TestMainThread;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ProcessTextPresenterTest {

    @Mock private ProcessTextContract.View view;
    @Mock private WordRepository wordRepository;
    @Mock private DictionaryRepository dictionaryRepository;
    @Mock private DatabaseExternalLinksSource linksRepository;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private CustomTTS customTTS;
    @Mock private GetLangAndTranslation getTranslationInteractor;

    @Captor
    private ArgumentCaptor<WordRepositorySource.GetWordRepositoryCallback> getWordCallbackCaptor;

    @Captor
    private ArgumentCaptor<WordRepositorySource.GetTranslationCallback> getTranslationCallbackCaptor;

    @Captor
    private ArgumentCaptor<DictionaryDataSource.GetDefinitionCallback> getDefinitionCallbacCaptor;

    @Captor
    private ArgumentCaptor<ExternalLinksDataSource.Callback> getLinksCaptor;

    @Captor
    private ArgumentCaptor<CustomTTS.Listener> ttsListenerCaptor;

    private ProcessTextPresenter presenter;

    private List<WikiItem> defaultDictionaryItems = Arrays.asList(
            new WiktionaryItem("First", "First header"),
            new WiktionaryItem("Second", "Second header")
    );

    private List<ExternalLink> defaultLinksItems = Collections.singletonList(
            new ExternalLink("Dummy Site", "dummy-link", "en")
    );

    private static final String languageFrom = "auto";
    private static final String languageTo = "en";

    @Before
    public void setupPresenter(){
        MockitoAnnotations.initMocks(this);
        presenter = givenPresenter();
    }

    private ProcessTextPresenter givenPresenter(){
        MainThread mainThread = new TestMainThread();
        Executor executor = new TestThreadExecutor();
        ProcessTextPresenter presenter = new ProcessTextPresenter(executor, mainThread, wordRepository, dictionaryRepository, linksRepository, sharedPreferences, customTTS, getTranslationInteractor);
        presenter.setView(view);
        return presenter;
    }


    @Test
    public void setSavedWordLayout(){
        String test_text = "Prueba";

        presenter.getLayout(test_text, languageFrom, languageTo);

        verify(wordRepository).getWordLanguageInfo(eq(test_text), eq(languageFrom), eq(languageTo), getWordCallbackCaptor.capture());
        Words return_word = new Words(test_text,"ES", "Test");
        getWordCallbackCaptor.getValue().onLocalWordLoaded(return_word);

        verify(view).setSavedWordLayout(return_word);
    }

    @Test
    public void setSentenceLayout(){
        String test_text = "Prueba oracion";
        presenter.getLayout(test_text, languageFrom, languageTo);

        verify(wordRepository).getLanguageAndTranslation(eq(test_text), eq(languageFrom), eq(languageTo), getTranslationCallbackCaptor.capture());
        Words return_word = new Words(test_text,"ES", "Sentence test");
        getTranslationCallbackCaptor.getValue().onTranslationAndLanguage(return_word);

        verify(view).setSentenceLayout(return_word);
    }

    @Test
    public void setTranslationLayout(){
        String test_text = "Prueba";
        presenter.getLayout(test_text, languageFrom, languageTo);

        verify(wordRepository).getWordLanguageInfo(eq(test_text), eq(languageFrom), eq(languageTo), getWordCallbackCaptor.capture());
        getWordCallbackCaptor.getValue().onLocalWordNotAvailable();

        Words return_word = new Words(test_text,"ES", "Test");
        getWordCallbackCaptor.getValue().onRemoteWordLoaded(return_word);

        verify(dictionaryRepository).getDefinition(eq(test_text), getDefinitionCallbacCaptor.capture());
        getDefinitionCallbacCaptor.getValue().onDataNotAvailable();

        verify(view).setTranslationLayout(return_word);

    }

    @Test
    public void setExternalDictionaryLayout(){
        String test_text = "Prueba";
        presenter.getLayout(test_text, languageFrom, languageTo);

        verify(wordRepository).getWordLanguageInfo(eq(test_text), eq(languageFrom), eq(languageTo), getWordCallbackCaptor.capture());
        getWordCallbackCaptor.getValue().onLocalWordNotAvailable();

        Words return_word = new Words(test_text,"ES", "Sentence test");
        getWordCallbackCaptor.getValue().onRemoteWordLoaded(return_word);

        List<WikiItem> wiktionaryLanguages = new ArrayList<>();
        verify(dictionaryRepository).getDefinition(eq(test_text), getDefinitionCallbacCaptor.capture());
        getDefinitionCallbacCaptor.getValue().onDefinitionLoaded(wiktionaryLanguages);


        verify(view, never()).setTranslationLayout(return_word);
//        verify(view).setWiktionaryLayout(wiktionaryLanguages);

    }

    @Test
    public void showLanguageNotAvailable(){
        String inputText = "desconocido";
        String languageFrom = "ES";

        // Gets saved word layout
        presenter.getLayout(inputText, languageFrom, languageTo);
        verify(wordRepository).getWordLanguageInfo(eq(inputText), eq(languageFrom), eq(languageTo), getWordCallbackCaptor.capture());

        Words return_word = new Words(inputText, languageFrom, "unknown");
        getWordCallbackCaptor.getValue().onLocalWordLoaded(return_word);

        // Reproduce tts
        presenter.onClickReproduce(inputText);
        verify(customTTS).speak(eq(inputText), ttsListenerCaptor.capture());
        ttsListenerCaptor.getValue().onLanguageUnavailable();

        verify(view).showLanguageNotAvailable();
    }

    @Test
    public void new_language_available_and_previous_not_available(){
        String inputText = "desconocido";
        String unknownLanguageFrom = "ES";

        // Gets saved word layout
        presenter.getLayout(inputText, unknownLanguageFrom, languageTo);
        verify(wordRepository).getWordLanguageInfo(eq(inputText), eq(unknownLanguageFrom), eq(languageTo), getWordCallbackCaptor.capture());

        Words unknowWord = new Words(inputText, unknownLanguageFrom, "desconocido");
        getWordCallbackCaptor.getValue().onLocalWordLoaded(unknowWord);

        // Tts returns language not available
        presenter.onClickReproduce(inputText);
        verify(customTTS).speak(eq(inputText), ttsListenerCaptor.capture());
        ttsListenerCaptor.getValue().onLanguageUnavailable();
        verify(view).showLanguageNotAvailable();

        inputText = "correct";
        String languageFrom = "EN";

        when(customTTS.getLanguage()).thenReturn(unknownLanguageFrom);

        // Gets saved word layout
        presenter.getLayout(inputText, languageFrom, languageTo);
        verify(wordRepository).getWordLanguageInfo(eq(inputText), eq(languageFrom), eq(languageTo), getWordCallbackCaptor.capture());

        Words return_word = new Words(inputText, languageFrom, "correct");
        getWordCallbackCaptor.getValue().onLocalWordLoaded(return_word);

        // Reproduce tts
        presenter.onClickReproduce(inputText);
        verify(customTTS).speak(eq(inputText), any());

    }

    @Test
    public void show_error_with_word_translation_and_dictionary_available(){
        String inputText = "test_input";
        String languageFrom = "ES";

        presenter.getLayout(inputText, languageFrom, languageTo);

        // Indicate that no local word nor remote word available
        verify(wordRepository).getWordLanguageInfo(eq(inputText), eq(languageFrom), eq(languageTo), getWordCallbackCaptor.capture());
        getWordCallbackCaptor.getValue().onLocalWordNotAvailable();
        Words emptyWord = new Words(inputText, "un", "un");
        getWordCallbackCaptor.getValue().onDataNotAvailable(emptyWord);

        // Return dictionary definitions
        verify(dictionaryRepository).getDefinition(eq(inputText), getDefinitionCallbacCaptor.capture());
        getDefinitionCallbacCaptor.getValue().onDefinitionLoaded(defaultDictionaryItems);

        // Return external links
        verify(linksRepository).getLanguageLinks(eq("un"), getLinksCaptor.capture());
        getLinksCaptor.getValue().onLinksRetrieved(defaultLinksItems);

        verify(view).setWiktionaryLayout(emptyWord, defaultDictionaryItems);
        verify(view).setTranslationErrorMessage();
    }

}
