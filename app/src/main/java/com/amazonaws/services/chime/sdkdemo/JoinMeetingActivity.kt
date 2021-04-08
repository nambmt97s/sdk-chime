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
    private lateinit var localVideo: ImageView
    private val MEETING_REGION = "us-east-1"
    private var meetingId: String? = null
    private var name: String? = null
    lateinit var chime: ChimeView
    var chimeMeetConferenceOptions: ChimeMeetConferenceOptions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_meeting)
        chime = ChimeView(this)
        getData()
        initView()
        initChimeView()
        handleEvent()
    }

    private fun handleEvent() {
        var booleanName = true
        localVideo.setOnClickListener {
            chimeMeetConferenceOptions = ChimeMeetConferenceOptions.Builder()
                .setServerURL("")
                .setRoom(meetingId!!)
                .setName(name!!)
                .setRegion(MEETING_REGION)
                .setAudioMuted(false)
                .setVideoMuted(booleanName)
                .build()
            booleanName = !booleanName
            chime.join(chimeMeetConferenceOptions!!)
        }
    }

    private fun getData() {
        meetingId = intent.getStringExtra("meetingId")
        name = intent.getStringExtra("name")
    }

    private fun initChimeView() {
        chimeMeetConferenceOptions = ChimeMeetConferenceOptions.Builder()
            .setServerURL("")
            .setRoom(meetingId!!)
            .setName(name!!)
            .setRegion(MEETING_REGION)
            .setAudioMuted(false)
            .setVideoMuted(true)
            .build()
        chime.join(chimeMeetConferenceOptions!!)
        chimeView.addView(chime)
    }

    private fun initView() {
        chimeView = findViewById(R.id.chimeView)
        micButton = findViewById(R.id.mute_action_fab)
        videoButton = findViewById(R.id.local_video_action_fab)
        actionMore = findViewById(R.id.action_more)
        localVideo = findViewById(R.id.local_video_action_fab)
    }
}
