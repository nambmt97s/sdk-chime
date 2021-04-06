package com.amazonaws.services.chime.sdk.meetings.data.response

import com.google.gson.annotations.SerializedName

data class AttendeeX(
    @SerializedName("AttendeeId")
    val attendeeId: String,
    @SerializedName("ExternalUserId")
    val externalUserId: String,
    @SerializedName("JoinToken")
    val joinToken: String
)
