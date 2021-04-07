package com.amazonaws.services.chime.sdk.meetings

import android.os.Bundle
import com.amazonaws.services.chime.sdk.meetings.data.CallingState

class CallingMeetingManager(private val chimeView: ChimeView) {

    var updateCallingState: ((CallingState) -> Unit?)? = null

    init {
        chimeView.updateCallingState = {
            updateCallingState?.invoke(it)
        }
    }

    fun setConfigChange(bundle: Bundle) {
//        chimeView.join(bundle)
    }

    fun setConfigMuteChange(check: Boolean) {
        chimeView.setConfigMuteChange(check)
    }

    fun setConfigVideoChange(check: Boolean) {
        chimeView.setConfigVideoChange(check)
    }

    fun sendEmojiPath(path: String) {
        chimeView.getEmojiPath(path)
    }

    companion object {
        private var instance: CallingMeetingManager? = null

        fun getInstance(chimeView: ChimeView): CallingMeetingManager {
            if (instance == null) {
                instance = CallingMeetingManager(chimeView)
            }
            return instance!!
        }
    }
}
