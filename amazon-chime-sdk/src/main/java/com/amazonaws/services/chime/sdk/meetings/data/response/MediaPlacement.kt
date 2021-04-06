package com.amazonaws.services.chime.sdk.meetings.data.response

import com.google.gson.annotations.SerializedName

data class MediaPlacement(
    @SerializedName("AudioFallbackUrl")
    val audioFallbackUrl: String,
    @SerializedName("AudioHostUrl")
    val audioHostUrl: String,
    @SerializedName("ScreenDataUrl")
    val screenDataUrl: String,
    @SerializedName("ScreenSharingUrl")
    val screenSharingUrl: String,
    @SerializedName("ScreenViewingUrl")
    val screenViewingUrl: String,
    @SerializedName("SignalingUrl")
    val signalingUrl: String,
    @SerializedName("TurnControlUrl")
    val turnControlUrl: String
)
