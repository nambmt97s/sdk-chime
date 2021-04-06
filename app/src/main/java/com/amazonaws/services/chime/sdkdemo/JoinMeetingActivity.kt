package com.amazonaws.services.chime.sdkdemo

import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.services.chime.sdk.meetings.ChimeMeetConferenceOptions
import com.amazonaws.services.chime.sdk.meetings.ChimeView

class JoinMeetingActivity : AppCompatActivity() {

    private lateinit var chimeView: RelativeLayout
    private lateinit var micButton: ImageView
    private lateinit var videoButton: ImageView
    private lateinit var actionMore: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_meeting)
        initView()
        initChimeView()
    }

    private fun initChimeView() {
        val chime = ChimeView(this)
        val chimeMeetConferenceOptions = ChimeMeetConferenceOptions.Builder()
            .setServerURL("")
            .setRoom("")
            .setAudioMuted(false)
            .setVideoMuted(true)
            .build()
        chime.join(chimeMeetConferenceOptions)
        chimeView.addView(chime)


    }

    private fun initView() {
        chimeView = findViewById(R.id.chimeView)
        micButton = findViewById(R.id.mute_action_fab)
        videoButton = findViewById(R.id.local_video_action_fab)
        actionMore = findViewById(R.id.action_more)
    }
}
