package com.guillermonegrete.tts.data.source.remote

import com.google.gson.*
import com.guillermonegrete.tts.data.source.WordDataSource
import com.guillermonegrete.tts.db.Words
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class GooglePublicSource private constructor() : WordDataSource {

    private var googlePublicAPI: GooglePublicAPI? = null
    private val gson: Gson = GsonBuilder()
            .setLenient()
            .create()

    init {
        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        googlePublicAPI = retrofit.create(GooglePublicAPI::class.java)
    }


    override fun getWordLanguageInfo(wordText: String?, callback: WordDataSource.GetWordCallback?) {
        if (wordText != null) {
            googlePublicAPI?.getWord(wordText)?.enqueue(object : Callback<ResponseBody>{
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    callback?.onDataNotAvailable()
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                    val body = response.body()
                    if(response.isSuccessful && body != null){
                        val jsonArray  = gson.fromJson(body.string(), JsonArray::class.java)
                        val translation = jsonArray[0].asJsonArray[0].asJsonArray[0]
                        val language = jsonArray.last().asJsonArray[0].asJsonArray[0]
                        callback?.onWordLoaded(Words(wordText, language.toString(), translation.toString()))
                    }else{
                        callback?.onDataNotAvailable()
                    }
                }

            })
        }
    }

    companion object {
        const val BASE_URL = "https://translate.google.com/translate_a/"
        private var INSTANCE : GooglePublicSource? = null

        fun getInstance() =
            INSTANCE ?: synchronized(this){
                INSTANCE ?: GooglePublicSource()
            }
    }
}