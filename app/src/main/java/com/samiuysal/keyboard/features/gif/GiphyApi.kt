package com.samiuysal.keyboard.features.gif

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GiphyApi {
        @GET("v1/gifs/search")
        fun searchGifs(
                @Query("api_key") apiKey: String,
                @Query("q") query: String,
                @Query("limit") limit: Int = 25,
                @Query("rating") rating: String = "g",
                @Query("lang") lang: String = "tr",
                @Query("country_code") countryCode: String = "TR"
        ): Call<GiphyResponse>

        @GET("v1/gifs/trending")
        fun getTrendingGifs(
                @Query("api_key") apiKey: String,
                @Query("limit") limit: Int = 25,
                @Query("rating") rating: String = "g",
                @Query("country_code") countryCode: String = "TR"
        ): Call<GiphyResponse>
}
