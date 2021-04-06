package com.amazonaws.services.chime.sdk.meetings.data.response

import com.google.gson.annotations.SerializedName

data class Attendee(
    @SerializedName("Attendee")
    val attendee: AttendeeX
)
