package com.amazonaws.services.chime.sdk.meetings

class ChimeMeetConferenceOptions private constructor(
    val serverURL: String?,
    val room: String?,
    val name: String?,
    val region: String?,
    val audioOnly: Boolean?,
    val videoMuted: Boolean?
) {

    data class Builder(
        var serverURL: String? = null,
        var room: String? = null,
        var name: String? = null,
        var region: String? = null,
        var audioOnly: Boolean? = false,
        var videoMuted: Boolean? = false

    ) {
        fun setServerURL(serverURL: String) = apply { this.serverURL = serverURL }
        fun setRoom(room: String) = apply { this.room = room }
        fun setAudioMuted(audioOnly: Boolean) = apply { this.audioOnly = audioOnly }
        fun setVideoMuted(videoMuted: Boolean) = apply { this.videoMuted = videoMuted }
        fun setName(name: String) = apply { this.name = name }
        fun setRegion(region: String) = apply { this.region = region }
        fun build() =
            ChimeMeetConferenceOptions(serverURL, room, name, region, audioOnly, videoMuted)
    }
}
