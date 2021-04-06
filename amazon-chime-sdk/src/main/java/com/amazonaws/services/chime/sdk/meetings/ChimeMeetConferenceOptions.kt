package com.amazonaws.services.chime.sdk.meetings

class ChimeMeetConferenceOptions private constructor(
    val serverURL: String?,
    val room: String?,
    val audioOnly: Boolean?,
    val videoMuted: Boolean?
) {

    data class Builder(
        var serverURL: String? = null,
        var room: String? = null,
        var audioOnly: Boolean? = null,
        var videoMuted: Boolean? = null
    ) {
        fun setServerURL(bread: String) = apply { this.serverURL = serverURL }
        fun setRoom(condiments: String) = apply { this.room = room }
        fun setAudioMuted(audioOnly: Boolean) = apply { this.audioOnly = audioOnly }
        fun setVideoMuted(videoMuted: Boolean) = apply { this.videoMuted = videoMuted }
        fun build() =
            ChimeMeetConferenceOptions(
                serverURL,
                room,
                audioOnly,
                videoMuted
            )
    }
}
