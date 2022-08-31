package com.example.map.di

import com.example.map.Constants
import com.example.map.retrofit.IMapApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object NetworkModule {
    fun providesRetrofit(): Retrofit {
        val okhttp = OkHttpClient.Builder()
            .callTimeout(5, TimeUnit.SECONDS)

        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okhttp.build())
            .build()
    }

    fun providesDirectionAPI(retrofit: Retrofit): IMapApi {
        return retrofit.create(IMapApi::class.java)
    }
}