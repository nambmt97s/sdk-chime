package com.amazonaws.services.chime.sdk.meetings.data.response

import com.google.gson.annotations.SerializedName

data class JoinInfo(
    @SerializedName("Attendee")
    val attendee: Attendee,
    @SerializedName("Meeting")
    val meeting: Meeting
)
