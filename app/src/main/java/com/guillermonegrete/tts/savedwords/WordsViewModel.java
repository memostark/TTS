package com.guillermonegrete.tts.savedwords;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDAO;
import com.guillermonegrete.tts.db.WordsDatabase;

import java.util.List;

public class WordsViewModel extends AndroidViewModel {
    private WordsDAO wordsDAO;
    private LiveData<List<Words>> wordsLiveData;
    private LiveData<List<String>> languagesLiveData;

    public WordsViewModel(@NonNull Application application){
        super(application);
        wordsDAO = WordsDatabase.getDatabase(application).wordsDAO();
        wordsLiveData = wordsDAO.getAllWords();
        languagesLiveData = wordsDAO.getLanguagesISO();
    }

    public LiveData<List<Words>> getWordsList(){
        return wordsLiveData;
    }

    public LiveData<List<String>> getLanguagesList(){
        return languagesLiveData;
    }

    public void insert(Words... words){
        wordsDAO.insert(words);
    }

    public void update(Words word){
        wordsDAO.update(word);
    }

    public void deleteAll(){
        wordsDAO.deleteAll();
    }
}
