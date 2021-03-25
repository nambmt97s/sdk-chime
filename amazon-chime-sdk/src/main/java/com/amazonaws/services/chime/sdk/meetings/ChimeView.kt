package com.amazonaws.services.chime.sdk.meetings

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdk.R
import com.amazonaws.services.chime.sdk.meetings.adapter.UserJoinedAdapter
import com.amazonaws.services.chime.sdk.meetings.audiovideo.* // ktlint-disable
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultEglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.customview.SlideCustomView
import com.amazonaws.services.chime.sdk.meetings.data.CallingState
import com.amazonaws.services.chime.sdk.meetings.data.RosterAttendee
import com.amazonaws.services.chime.sdk.meetings.data.VideoCollectionTile
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.dialog.AudioSelectDialog
import com.amazonaws.services.chime.sdk.meetings.dialog.OnItemBottomClickListener
import com.amazonaws.services.chime.sdk.meetings.internal.utils.CpuVideoProcessor
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSession
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.utils.Convert
import com.amazonaws.services.chime.sdk.meetings.utils.DefaultModality
import com.amazonaws.services.chime.sdk.meetings.utils.ModalityType
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.nam.demochime.utils.GpuVideoProcessor

class ChimeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle), VideoTileObserver, RealtimeObserver,
    AudioVideoObserver, OnItemBottomClickListener {

    private lateinit var chimeLayout: View
    private lateinit var audioVideo: AudioVideoFacade
    private val currentRoster = mutableMapOf<String, RosterAttendee>()
    private val remoteVideoTileStates = mutableListOf<VideoCollectionTile>()
    private val currentRosters = mutableListOf<RosterAttendee>()
    private val eglCoreFactory: EglCoreFactory = DefaultEglCoreFactory()
    private lateinit var gpuVideoProcessor: GpuVideoProcessor
    private lateinit var cpuVideoProcessor: CpuVideoProcessor
    private var attendeeLocal: VideoCollectionTile? = null
    private var compareVideo: VideoCollectionTile? = null
    private var meetingSession: MeetingSession? = null
    private var userJoinedAdapter: UserJoinedAdapter? = null
    private lateinit var callingState: CallingState
    private lateinit var audioDevices: List<MediaDevice>
    private lateinit var audioSelectDialog: AudioSelectDialog
    private lateinit var setUpAudio: SetUpAudio

    private val logger = ConsoleLogger(LogLevel.DEBUG)
    private val TAG: String = "XXXXX"

    //    private val VIDEO_ASPECT_RATIO_16_9 = 0.5625
    private lateinit var chimeContainer: RelativeLayout
    private lateinit var localVideo: RelativeLayout
    private lateinit var renderViewLayout: View
    private lateinit var renderViewLocalLayout: View
    private lateinit var icMoveView: ImageView
    private lateinit var slideCustomView: SlideCustomView
    private lateinit var localRenderView: DefaultVideoRenderView
    private lateinit var rcUsersJoined: RecyclerView
    private lateinit var userJoinedView: View
    private lateinit var btnSpeaker: ImageView
    private lateinit var swipeCamera: ImageView

    var updateCallingState: ((CallingState) -> Unit)? = null
    var notificationUserJoin: ((List<RosterAttendee>) -> Unit)? = null

    //    private val currentApiVersion = Build.VERSION.SDK_INT
    private lateinit var cameraCaptureSource: CameraCaptureSource
    private var isOpenListUser = false

    init {
        inflateView()
        setUpBegin()
        handleMoveView()
        handleChooseAudio()
        handleSwipeCamera()
    }

    private fun setUpBegin() {
        setUpAudio = SetUpAudio.getInstance()
        meetingSession = setUpAudio.meetingSession()
        audioVideo = meetingSession!!.audioVideo
        cameraCaptureSource = setUpAudio.getCameraCaptureSource()
        callingState = CallingState()
        audioDevices = audioVideo.listAudioDevices()
        audioSelectDialog = AudioSelectDialog(audioDevices, this)
        setUpConfig()
        subscribeListener()
        setUpLocalVideo()
    }

    private fun handleSwipeCamera() {
        swipeCamera.setOnClickListener {
            cameraCaptureSource.switchCamera()
        }
    }

    private fun handleChooseAudio() {
        btnSpeaker.setOnClickListener {
            try {
                val fragmentManager: FragmentManager =
                    (context as FragmentActivity).supportFragmentManager
                audioSelectDialog.show(fragmentManager, "BottomDialog")
                Log.e(TAG, "Can get fragment manager $fragmentManager")
            } catch (e: ClassCastException) {
                Log.e(TAG, "Can't get fragment manager")
            }
        }
    }

    private fun handleMoveView() {
        slideCustomView.alpha = 0.5F
        userJoinedAdapter = UserJoinedAdapter(currentRosters)
        rcUsersJoined.adapter = userJoinedAdapter
        notificationUserJoin = {
            userJoinedAdapter!!.notifyDataSetChanged()
        }
        icMoveView.setOnClickListener {
            val displayMetrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val height = displayMetrics.heightPixels
            val width = displayMetrics.widthPixels
            val moveViewXSize: Int = icMoveView.measuredWidth
            val slideXSize = slideCustomView.measuredWidth
            if (isOpenListUser) {
                slideCustomView.animate().x(width.toFloat() - Convert.convertDpToPixel(2f, context))
                    .duration = 300
                icMoveView.animate()
                    .x(width - moveViewXSize.toFloat() - Convert.convertDpToPixel(2f, context))
                    .duration = 300
                isOpenListUser = false
                icMoveView.setImageResource(R.drawable.ic_baseline_keyboard_arrow_right_24)
            } else {
                icMoveView.animate().x(width - moveViewXSize - slideXSize.toFloat()).duration = 300
                slideCustomView.animate().x((width - slideXSize).toFloat()).duration = 300
                isOpenListUser = true
                icMoveView.setImageResource(R.drawable.ic_baseline_keyboard_arrow_left_24)
            }
        }
    }

    private fun setUpLocalVideo() {
        gpuVideoProcessor = GpuVideoProcessor(logger, eglCoreFactory)
        cpuVideoProcessor = CpuVideoProcessor(logger, eglCoreFactory)
        audioVideo.startLocalVideo(cameraCaptureSource)
        cameraCaptureSource.start()
    }

    fun setConfigMuteChange(check: Boolean) {
        with(meetingSession!!.audioVideo) {
            if (check)
                realtimeLocalUnmute() // start mic
            else
                realtimeLocalMute() // stop mic
        }
        callingState.isSpeaking = !check
        updateCallingState?.invoke(callingState)
    }

    fun setConfigVideoChange(check: Boolean) {
        with(meetingSession!!.audioVideo) {
            if (!check) {
                startLocalVideo(cameraCaptureSource) // start camera
                cameraCaptureSource.start()
                localVideo.visibility = View.VISIBLE
                localRenderView.visibility = View.VISIBLE
            } else {
                cameraCaptureSource.stop() // stop camera
                stopLocalVideo()
                localVideo.visibility = View.GONE
                localRenderView.visibility = View.GONE
            }
        }
        callingState.isStreaming = !check
        updateCallingState?.invoke(callingState)
    }

    fun join(bundle: Bundle) {
        val isSpeaking = bundle.getBoolean("isSpeaking")
        val isStreaming = bundle.getBoolean("isStreaming")
        with(meetingSession!!.audioVideo) {
            if (isSpeaking) realtimeLocalUnmute() // start mic
            else realtimeLocalMute() // stop mic
            if (isStreaming) {
                startLocalVideo(cameraCaptureSource) // start camera
                cameraCaptureSource.start()
                localVideo.visibility = View.VISIBLE
                localRenderView.visibility = View.VISIBLE
            } else {
                cameraCaptureSource.stop() // stop camera
                stopLocalVideo()
                localVideo.visibility = View.GONE
                localRenderView.visibility = View.GONE
            }
        }
        updateCallingState?.invoke(callingState)
    }
    private fun subscribeListener() {
        audioVideo.addRealtimeObserver(this)
        audioVideo.addVideoTileObserver(this)
        audioVideo.addAudioVideoObserver(this)
        audioVideo.start()
        audioVideo.startRemoteVideo()
    }

    private fun createVideoCollectionTile(tileState: VideoTileState): VideoCollectionTile {
        val attendeeId = tileState.attendeeId
        val attendeeName = currentRoster[attendeeId]?.attendeeName ?: ""
        return VideoCollectionTile(attendeeName, tileState)
    }
    override fun onVideoTileAdded(tileState: VideoTileState) {
        val videoCollectionTile = createVideoCollectionTile(tileState)
        val attendeeBefore = remoteVideoTileStates.find {
            it.videoTileState.attendeeId == videoCollectionTile.videoTileState.attendeeId
        }
        if (remoteVideoTileStates.isNotEmpty()) {
            attendeeLocal = remoteVideoTileStates[0]
        }
        if (attendeeBefore == null) {
            remoteVideoTileStates.add(videoCollectionTile)
        } else {
            remoteVideoTileStates.forEach {
                if (it.videoTileState.attendeeId == attendeeBefore.videoTileState.attendeeId) {
                    remoteVideoTileStates.replace(videoCollectionTile) { value ->
                        value == attendeeBefore
                    }
                }
            }
        }

        Log.d(TAG, "yyyy " + tileState.isLocalTile)
        Log.d(TAG, "zzzz " + remoteVideoTileStates.size)
        if (remoteVideoTileStates.size == 1) {
            showLocal()
        }
        if (videoCollectionTile.videoTileState.isLocalTile && callingState.idAttendee != videoCollectionTile.videoTileState.attendeeId) {
            callingState = CallingState(
                name = videoCollectionTile.attendeeName,
                idAttendee = videoCollectionTile.videoTileState.attendeeId
            )
        }
        if (videoCollectionTile.videoTileState.isLocalTile) {
            callingState.isStreaming = true
        }
        if (remoteVideoTileStates.size == 2) {
            initChime()
            currentRoster.entries.forEach {
                Log.d(
                    TAG, "CurrentRoster:  " +
                            "attendeeId: ${it.value.attendeeId} " +
                            "isActiveSpeaker: ${it.value.isActiveSpeaker} " +
                            "attendeeName ${it.value.attendeeName} " +
                            "signalStrength ${it.value.signalStrength} " +
                            "volumeLevel ${it.value.volumeLevel} "
                )
            }
        }
    }

    private fun <T> List<T>.replace(newValue: T, block: (T) -> Boolean): List<T> {
        return map {
            if (block(it)) newValue else it
        }
    }

    private fun showLocal() {
        bind(remoteVideoTileStates[0], renderViewLayout.findViewById(R.id.chime))
    }

    private fun initChime() {
        if (compareVideo == null) {
            compareVideo = remoteVideoTileStates[0]
        }
        bind(remoteVideoTileStates[1], renderViewLayout.findViewById(R.id.chime))
        bind(remoteVideoTileStates[0], renderViewLocalLayout.findViewById(R.id.chime_2))
    }

    private fun bind(
        videoCollectionTile: VideoCollectionTile,
        videoRenderView: DefaultVideoRenderView
    ) {
        videoCollectionTile.videoRenderView = videoRenderView
        audioVideo.bindVideoView(videoRenderView, videoCollectionTile.videoTileState.tileId)
    }

    override fun onVideoTilePaused(tileState: VideoTileState) {
        Log.d(TAG, "onVideoTilePaused: ")
    }

    override fun onVideoTileRemoved(tileState: VideoTileState) {
        if (tileState.attendeeId == callingState.idAttendee) {
            callingState.isStreaming = false
        }
        remoteVideoTileStates.forEach {
            if (it.videoTileState.attendeeId == tileState.attendeeId) {
                remoteVideoTileStates.remove(it)
                return@forEach
            }
        }
        Log.d(TAG, "onVideoTileRemoved: ")
    }

    override fun onVideoTileResumed(tileState: VideoTileState) {
        Log.d(TAG, "onVideoTileResumed: ")
    }

    override fun onVideoTileSizeChanged(tileState: VideoTileState) {
        Log.d(TAG, "onVideoTileSizeChanged: ")
    }

    override fun onAttendeesDropped(attendeeInfo: Array<AttendeeInfo>) {
        Log.d(TAG, "onAttendeesDropped: ")
    }

    override fun onAttendeesJoined(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach { (attendeeId, externalUserId) ->
            currentRoster.getOrPut(attendeeId,
                { RosterAttendee(attendeeId, getAttendeeName(attendeeId, externalUserId)) })
        }
        currentRosters.clear()
        currentRoster.forEach { (_, roster) ->
            Log.d(TAG, "xxxxxxxxx " + roster.attendeeName)
            currentRosters.add(roster)
        }
        // Here
        callingState.allAttendees = currentRoster.values.toList()
        updateCallingState?.invoke(callingState)
        Log.d(TAG, "onAttendeesJoined" + callingState.toString() + currentRoster.size)
        notificationUserJoin?.invoke(currentRosters)
    }

    private val CONTENT_NAME_SUFFIX = "<<Content>>"

    private fun getAttendeeName(attendeeId: String, externalUserId: String): String {
        val attendeeName = externalUserId.split('#')[1]
        return if (DefaultModality(attendeeId).hasModality(ModalityType.Content)) {
            "$attendeeName $CONTENT_NAME_SUFFIX"
        } else {
            attendeeName
        }
    }

    override fun onAttendeesLeft(attendeeInfo: Array<AttendeeInfo>) {
        Log.d(TAG, "onAttendeesLeft: ")
        attendeeInfo.forEach { (attendeeId, _) ->
            currentRoster.remove(attendeeId)
        }
        currentRosters.clear()
        currentRoster.forEach { (_, roster) ->
            Log.d(TAG, "xxxxxxxxx " + roster.attendeeName)
            currentRosters.add(roster)
        }
        notificationUserJoin?.invoke(currentRosters)
    }

    override fun onAttendeesMuted(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach {
            if (it.attendeeId == callingState.idAttendee) {
                callingState.isSpeaking = callingState.isSpeaking.not()
                updateCallingState?.invoke(callingState)
                Log.d(TAG, "onAttendeesMuted: $callingState")
            }
        }
        Log.d(TAG, "onAttendeesMuted: ")
    }

    override fun onAttendeesUnmuted(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach {
            if (it.attendeeId == callingState.idAttendee) {
                callingState.isSpeaking = callingState.isSpeaking.not()
                updateCallingState?.invoke(callingState)
                Log.d(TAG, "onAttendeesUnmuted: $callingState")
            }
        }
        Log.d(TAG, "onAttendeesUnmuted: ")
    }

    override fun onSignalStrengthChanged(signalUpdates: Array<SignalUpdate>) {
//        Log.d(TAG, "onSignalStrengthChanged: ")
    }

    override fun onVolumeChanged(volumeUpdates: Array<VolumeUpdate>) {
//        Log.d(TAG, "onVolumeChanged: ")
    }

    override fun onAudioSessionCancelledReconnect() {
        Log.d(TAG, "onAudioSessionCancelledReconnect: ")
    }

    override fun onAudioSessionDropped() {
        Log.d(TAG, "onAudioSessionDropped: ")
    }

    override fun onAudioSessionStarted(reconnecting: Boolean) {
        Log.d(TAG, "onAudioSessionStarted: ")
    }

    override fun onAudioSessionStartedConnecting(reconnecting: Boolean) {
        Log.d(TAG, "onAudioSessionStartedConnecting: ")
    }

    override fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus) {
        Log.d(TAG, "onAudioSessionStopped: ")
    }

    override fun onConnectionBecamePoor() {
        Log.d(TAG, "onConnectionBecamePoor: ")
    }

    override fun onConnectionRecovered() {
        Log.d(TAG, "onConnectionRecovered: ")
    }

    override fun onVideoSessionStarted(sessionStatus: MeetingSessionStatus) {
        Log.d(TAG, "onVideoSessionStarted: ")
        val audioDevices = meetingSession!!.audioVideo.listAudioDevices()
        audioDevices.forEach {
            Log.d(TAG, "Device type: ${it.type}, label: ${it.label}")
        }
        val myAudioDevice = meetingSession!!.audioVideo.listAudioDevices().filter {
            it.type != MediaDeviceType.OTHER
        }
        val device = myAudioDevice[0]
        meetingSession!!.audioVideo.chooseAudioDevice(device)
    }

    override fun onVideoSessionStartedConnecting() {
        Log.d(TAG, "onVideoSessionStartedConnecting: ")
    }

    override fun onVideoSessionStopped(sessionStatus: MeetingSessionStatus) {
        Log.d(TAG, "onVideoSessionStopped: ")
    }

    private fun inflateView() {
        val inflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        chimeLayout = inflater.inflate(R.layout.chime_layout, this, true)
        // findViewByID
        chimeContainer = chimeLayout.findViewById(R.id.chime_meeting_room_container)
        localVideo = chimeLayout.findViewById(R.id.localVideo)
        icMoveView = chimeLayout.findViewById(R.id.icMoveView)
        slideCustomView = chimeLayout.findViewById(R.id.slideCustomView)
        btnSpeaker = chimeLayout.findViewById(R.id.btnSpeaker)
        swipeCamera = chimeLayout.findViewById(R.id.swipe_camera)

        renderViewLayout = inflater.inflate(R.layout.chime_view, chimeContainer, true)
        renderViewLocalLayout = inflater.inflate(R.layout.chime_view_2, localVideo, true)
        localRenderView = renderViewLocalLayout.findViewById(R.id.chime_2)

        // inflater customView slider add ViewSlide
        userJoinedView = inflater.inflate(R.layout.view_users_joined, slideCustomView, true)
        rcUsersJoined = userJoinedView.findViewById(R.id.rcUsersJoined)
    }

    private fun setUpConfig() {
        gpuVideoProcessor = GpuVideoProcessor(logger, eglCoreFactory)
        cpuVideoProcessor = CpuVideoProcessor(logger, eglCoreFactory)
    }

    override fun onClickListener(position: Int) {
//        Toast.makeText(context  ,position, Toast.LENGTH_SHORT).show()
    }
}
