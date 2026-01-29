package com.samiuysal.keyboard.features.gif

import com.google.gson.annotations.SerializedName

data class GiphyResponse(@SerializedName("data") val data: List<GiphyObject>)

data class GiphyObject(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String,
        @SerializedName("images") val images: GiphyImages,
        @SerializedName("url") val url: String
)

data class GiphyImages(
        @SerializedName("fixed_height_small") val fixedHeightSmall: GiphyImageDetail,
        @SerializedName("fixed_width_downsampled") val preview: GiphyImageDetail,
        @SerializedName("downsized") val original: GiphyImageDetail
)

data class GiphyImageDetail(
        @SerializedName("url") val url: String,
        @SerializedName("width") val width: Int,
        @SerializedName("height") val height: Int
)
