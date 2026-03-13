package edu.nd.pmcburne.hwapp.one.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ScoreApiClient {
    val api: ScoreAPI by lazy {
        Retrofit.Builder()
            .baseUrl("https://ncaa-api.henrygd.me/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ScoreAPI::class.java)
    }
}