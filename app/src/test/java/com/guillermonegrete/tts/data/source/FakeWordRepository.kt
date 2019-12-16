package com.guillermonegrete.tts.data.source

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.guillermonegrete.tts.db.Words
import java.util.LinkedHashMap

class FakeWordRepository: WordRepositorySource {

    var wordsServiceData: LinkedHashMap<Int, Words> = LinkedHashMap()

    var languagesData: MutableSet<String> = mutableSetOf()

    override fun getWords(): MutableList<Words> {
        return wordsServiceData.values.toMutableList()
    }

    override fun getWordsStream(): LiveData<MutableList<Words>> {
        return MutableLiveData(wordsServiceData.values.toMutableList())
    }

    override fun getLanguagesISO(): MutableList<String> {
        return languagesData.toMutableList()
    }

    override fun getWordLanguageInfo(wordText: String?, callback: WordRepositorySource.GetWordRepositoryCallback?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getWordLanguageInfo(
        wordText: String?,
        languageFrom: String?,
        languageTo: String?,
        callback: WordRepositorySource.GetWordRepositoryCallback?
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLanguageAndTranslation(text: String?, callback: WordRepositorySource.GetTranslationCallback?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLanguageAndTranslation(
        text: String?,
        languageFrom: String?,
        languageTo: String?,
        callback: WordRepositorySource.GetTranslationCallback?
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteWord(word: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteWord(word: Words?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(vararg words: Words?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun insert(vararg words: Words?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @VisibleForTesting
    fun addWords(vararg words: Words) {
        for (word in words) {
            wordsServiceData[word.id] = word
            languagesData.add(word.lang)
        }
    }
}