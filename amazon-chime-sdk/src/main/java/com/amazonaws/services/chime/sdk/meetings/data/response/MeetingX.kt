package com.amazonaws.services.chime.sdk.meetings.data.response

import com.google.gson.annotations.SerializedName

data class MeetingX(
    @SerializedName("ExternalMeetingId")
    val externalMeetingId: String,
    @SerializedName("MediaPlacement")
    val mediaPlacement: MediaPlacement,
    @SerializedName("MediaRegion")
    val mediaRegion: String,
    @SerializedName("MeetingId")
    val meetingId: String
)
