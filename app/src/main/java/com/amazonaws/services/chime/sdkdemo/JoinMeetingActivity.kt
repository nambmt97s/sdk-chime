package com.amazonaws.services.chime.sdkdemo

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.services.chime.sdk.meetings.CallingMeetingManager
import com.amazonaws.services.chime.sdk.meetings.ChimeView

class JoinMeetingActivity : AppCompatActivity() {

    private lateinit var chimeView: ChimeView
    private lateinit var micButton: ImageView
    private lateinit var videoButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_meeting)
        initView()
        handleEvent()
    }

    private fun handleEvent() {
        val callingMeetingManager = CallingMeetingManager.getInstance(chimeView)
        callingMeetingManager.updateCallingState = { callingState ->
            if (callingState.isStreaming) {
                videoButton.setImageResource(R.drawable.video_enable)
            } else {
                videoButton.setImageResource(R.drawable.video_disable)
            }
            if (callingState.isSpeaking) {
                micButton.setImageResource(R.drawable.mic_enable)
            } else {
                micButton.setImageResource(R.drawable.mic_disable)
            }
            micButton.setOnClickListener {
                callingMeetingManager.setConfigMuteChange(callingState.isSpeaking)
            }
            videoButton.setOnClickListener {
                callingMeetingManager.setConfigVideoChange(callingState.isStreaming)
            }
        }
    }

    private fun initView() {
        chimeView = findViewById(R.id.chimeView)
        micButton = findViewById(R.id.mute_action_fab)
        videoButton = findViewById(R.id.local_video_action_fab)
    }
}
