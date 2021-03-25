package com.amazonaws.services.chime.sdk.meetings.data

data class CallingState(
    val name: String = "",
    var idAttendee: String = "",
    var isSpeaking: Boolean = true,
    var isStreaming: Boolean = true,
    var allAttendees: List<RosterAttendee> = emptyList()
)
