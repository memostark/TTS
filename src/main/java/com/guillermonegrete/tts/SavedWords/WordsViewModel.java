package com.guillermonegrete.tts.SavedWords;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDAO;
import com.guillermonegrete.tts.db.WordsDatabase;

import java.util.List;

public class WordsViewModel extends AndroidViewModel {
    private WordsDAO wordsDAO;
    private LiveData<List<Words>> wordsLiveData;

    public WordsViewModel(@NonNull Application application){
        super(application);
        wordsDAO = WordsDatabase.getDatabase(application).wordsDAO();
        wordsLiveData = wordsDAO.getAllWords();
    }

    public LiveData<List<Words>> getWordsList(){
        return wordsLiveData;
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