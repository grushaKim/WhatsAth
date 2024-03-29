package com.example.mywhatsath.utils.retrofit

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private var instance: Retrofit? = null
    private val gson = GsonBuilder().setLenient().create() // load gson w setting converter

    fun getInstance(baseUrl: String?): Retrofit {
        // create logger
        val interceptor = HttpLoggingInterceptor()
        interceptor.apply{
            interceptor.level = HttpLoggingInterceptor.Level.BODY
        }

        val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        if(instance == null) {

            instance = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client) // set logger
                .addConverterFactory(GsonConverterFactory.create(gson)) // convert json to data class
                .build()

        }

        return instance!!
    }
}