package com.samiuysal.keyboard.features.gif

import com.samiuysal.keyboard.BuildConfig

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GiphyManager {
    private const val BASE_URL = "https://api.giphy.com/"

    private val API_KEY = BuildConfig.GIPHY_API_KEY

    private var api: GiphyApi? = null

    init {
        val retrofit =
                Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
        api = retrofit.create(GiphyApi::class.java)
    }

    fun searchGifs(
            query: String,
            lang: String = "tr",
            countryCode: String = "US",
            onResult: (List<GiphyObject>) -> Unit
    ) {
        if (query.isEmpty()) return

        api?.searchGifs(
                        apiKey = API_KEY,
                        query = query,
                        rating = "g",
                        lang = lang,
                        countryCode = countryCode
                )
                ?.enqueue(
                        object : Callback<GiphyResponse> {
                            override fun onResponse(
                                    call: Call<GiphyResponse>,
                                    response: Response<GiphyResponse>
                            ) {
                                if (response.isSuccessful) {
                                    onResult(response.body()?.data ?: emptyList())
                                } else {
                                    onResult(emptyList())
                                }
                            }

                            override fun onFailure(call: Call<GiphyResponse>, t: Throwable) {
                                t.printStackTrace()
                                onResult(emptyList())
                            }
                        }
                )
    }

    fun getTrendingGifs(countryCode: String = "US", onResult: (List<GiphyObject>) -> Unit) {
        api?.getTrendingGifs(apiKey = API_KEY, rating = "g", countryCode = countryCode)
                ?.enqueue(
                        object : Callback<GiphyResponse> {
                            override fun onResponse(
                                    call: Call<GiphyResponse>,
                                    response: Response<GiphyResponse>
                            ) {
                                if (response.isSuccessful) {
                                    onResult(response.body()?.data ?: emptyList())
                                } else {
                                    onResult(emptyList())
                                }
                            }

                            override fun onFailure(call: Call<GiphyResponse>, t: Throwable) {
                                t.printStackTrace()
                                onResult(emptyList())
                            }
                        }
                )
    }
}
