package com.amazonaws.services.chime.sdk.meetings

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.children
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
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultCameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultSurfaceTextureCaptureSourceFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultEglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.data.CallingState
import com.amazonaws.services.chime.sdk.meetings.data.RosterAttendee
import com.amazonaws.services.chime.sdk.meetings.data.VideoCollectionTile
import com.amazonaws.services.chime.sdk.meetings.data.response.Response
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.dialog.AudioSelectDialog
import com.amazonaws.services.chime.sdk.meetings.dialog.OnItemBottomClickListener
import com.amazonaws.services.chime.sdk.meetings.internal.utils.CpuVideoProcessor
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import com.amazonaws.services.chime.sdk.meetings.session.* // ktlint-disable
import com.amazonaws.services.chime.sdk.meetings.sharepreference.SharePreferenceKey
import com.amazonaws.services.chime.sdk.meetings.sharepreference.SharedPreferencesManager
import com.amazonaws.services.chime.sdk.meetings.utils.Convert
import com.amazonaws.services.chime.sdk.meetings.utils.DefaultModality
import com.amazonaws.services.chime.sdk.meetings.utils.ModalityType
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.google.gson.Gson
import com.nam.chime_sdk.data.JoinMeetingResponse
import com.nam.demochime.utils.GpuVideoProcessor
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.* // ktlint-disable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private var currentRosters = mutableListOf<RosterAttendee>()
    private val eglCoreFactory: EglCoreFactory = DefaultEglCoreFactory()
    private lateinit var gpuVideoProcessor: GpuVideoProcessor
    private lateinit var cpuVideoProcessor: CpuVideoProcessor
    private var attendeeLocal: VideoCollectionTile? = null
    private var compareVideo: VideoCollectionTile? = null
    private var meetingSession: MeetingSession? = null
    private var userJoinedAdapter: UserJoinedAdapter? = null
    private var callingState: CallingState? = null
    private lateinit var audioDevices: List<MediaDevice>
    private lateinit var audioSelectDialog: AudioSelectDialog
    private var STATUS_DISPLAY = 1

    //    private lateinit var setUpAudio: SetUpAudio
    private var memberJoined = 0
    private var channelData: ChannelData? = null
    private var focusAttendee: RosterAttendee? = null
    private var initFocusVideo = true
    private var inforMeeting: Response? = null
    private val gson = Gson()

    private val logger = ConsoleLogger(LogLevel.DEBUG)
    private val TAG: String = "XXXXX"

    //    private val VIDEO_ASPECT_RATIO_16_9 = 0.5625
    private lateinit var chimeContainer: RelativeLayout
    private lateinit var localVideo: RelativeLayout
    private var renderViewLayout: View? = null
    private lateinit var renderViewLocalLayout: View
    private lateinit var icMoveView: ImageView
    private lateinit var viewCorner: View

    //    private lateinit var slideCustomView: SlideCustomView
    private lateinit var localRenderView: DefaultVideoRenderView

    //    private lateinit var rcUsersJoined: RecyclerView
    private lateinit var userJoinedView: View
    private lateinit var btnSpeaker: ImageView
    private lateinit var swipeCamera: ImageView
    private lateinit var icClose: ImageView
    private lateinit var emoji: ImageView
    private lateinit var emojiCurrentUser: ImageView
    private lateinit var emojiCurrentUserWhenTurnOffCamera: ImageView
    private lateinit var containerLocalVideo: RelativeLayout
    private lateinit var invisibleCamera: RelativeLayout
    private lateinit var mainChimeLayout: RelativeLayout
    private lateinit var test: RelativeLayout
    private val layoutParamsMaxParentRelativeLayout = RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.MATCH_PARENT,
        RelativeLayout.LayoutParams.MATCH_PARENT
    )

    //    private lateinit var constraintLayoutMove: ConstraintLayout
    private lateinit var rcSlide: RecyclerView

    var updateCallingState: ((CallingState) -> Unit)? = null
    var notificationUserJoin: ((List<RosterAttendee>) -> Unit)? = null

    //    private val currentApiVersion = Build.VERSION.SDK_INT
    private lateinit var cameraCaptureSource: CameraCaptureSource
    private var isOpenListUser = false
    private var isOpenRelativeLayoutLocalVideo = false
    private var positionEmojiX: Float? = null
    private var positionEmojiY: Float? = null
    private var firstTimeSet = true
    private var firstTimeJoin = true

    init {
        EmojiManager.install(GoogleEmojiProvider())
        inflateView()
    }

    private fun handleClickUser() {
        userJoinedAdapter?.userClickListener = {
            // unbind current attendee to bind attendee new
            getVideoTileStateFromRosterAttendee(focusAttendee!!)?.let { videoCollectionTile ->
                unbind(videoCollectionTile)
            }
            focusAttendee = it
            updateView()
        }
    }

    private fun getVideoTileStateFromRosterAttendee(rosterAttendee: RosterAttendee): VideoCollectionTile? {
        remoteVideoTileStates.forEach {
            if (it.videoTileState.attendeeId == rosterAttendee.attendeeId)
                return it
        }
        return null
    }

    private fun updateView() {
        var isHaveRemoteVideo = false
        remoteVideoTileStates.forEach {
            if (it.videoTileState.attendeeId == focusAttendee?.attendeeId) {
                isHaveRemoteVideo = true
                bind(it, renderViewLayout!!.findViewById(R.id.chime))
            }
        }
        if (!isHaveRemoteVideo) {
            invisibleCamera.width
            invisibleCamera.height
            invisibleCamera.layoutParams
            invisibleCamera.visibility = View.VISIBLE
        } else {
            invisibleCamera.visibility = View.GONE
        }
    }

    private fun delayShow() {
        val handler = Handler()
        handler.postDelayed({
            showContainerLocalMiniSize()
        }, 50)
    }

    private var dataEmoji: String = ""
    private var updateEmoji: (() -> Unit)? = null

    fun updateEmoji() {
        updateEmoji = {
            sendReaction(dataEmoji)
        }
    }

    fun sendReaction(data: String) {
        resetReaction()
        dataEmoji = data
        when (STATUS_DISPLAY) {
            ONLY_LOCAL -> displayEmoji(imageView = emoji, data = data)
            BOTH_MINI_SIZE_LOCAL_TURN_OFF_AND_REMOTE -> displayEmoji(
                imageView = emojiCurrentUserWhenTurnOffCamera,
                data = data
            )
            BOTH_FULL_SIZE_LOCAL_AND_REMOTE_ -> displayEmoji(imageView = emoji, data = data)
            BOTH_MINI_SIZE_LOCAL_AND_REMOTE_ -> displayEmoji(
                imageView = emojiCurrentUser,
                data = data
            )
        }
    }

    private fun resetReaction() {
        emoji.setImageResource(0)
        emojiCurrentUserWhenTurnOffCamera.setImageResource(0)
        emojiCurrentUser.setImageResource(0)
    }

    private fun View.setAllEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (this is ViewGroup) children.forEach { child -> child.setAllEnabled(enabled) }
    }

    private fun getParams(view: View) =
        listOfParams(
            width = view.width,
            height = view.height,
            x = view.x,
            y = view.y,
            right = view.right,
            left = view.left,
            top = view.top,
            bottom = view.bottom
        )

    private fun listOfParams(
        width: Int,
        height: Int,
        x: Float,
        y: Float,
        right: Int,
        left: Int,
        top: Int,
        bottom: Int
    ) =
        listOf(width, height, x, y, right, left, top, bottom)

    operator fun <T> List<T>.component6(): T = get(5)
    operator fun <T> List<T>.component7(): T = get(6)
    operator fun <T> List<T>.component8(): T = get(7)

    private fun handleClickLocalVideo() {
        val (emojiCurrentWidth, emojiCurrentHeight, emojiCurrentX, emojiCurrentY, emojiCurrentRight, emojiCurrentLeft, emojiCurrentTop, emojiCurrentBottom) =
                getParams(emojiCurrentUser)
        val (emojiWidth, emojiHeight, emojiX, emojiY, emojiRight, emojiLeft, emojiTop, emojiBottom) =
                getParams(emojiCurrentUser)
        val lp = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        lp.setMargins(0, 0, 0, 0)
        val layoutParamsContainerLocalVideo = containerLocalVideo.layoutParams

        val vlp = emojiCurrentUser.layoutParams as MarginLayoutParams
        val layoutParamsViewCorner = localRenderView.layoutParams
        val layoutParamsEmoji = emojiCurrentUser.layoutParams
        containerLocalVideo.setOnClickListener {
            STATUS_DISPLAY = BOTH_FULL_SIZE_LOCAL_AND_REMOTE_
            if (!isOpenRelativeLayoutLocalVideo) {
                hideContainerLocalMiniSize()
                containerLocalVideo.layoutParams = layoutParamsMaxParentRelativeLayout
                localVideo.layoutParams = layoutParamsMaxParentRelativeLayout
                localRenderView.layoutParams = lp // set lại layoutParams cho localLayout Params
                emojiCurrentUser.x = emojiLeft.toString().toFloat()
                emojiCurrentUser.y = emojiTop.toString().toFloat()
                emojiCurrentUser.layoutParams.width = emojiWidth.toString().toInt()
                emojiCurrentUser.layoutParams.height = emojiHeight.toString().toInt()
                containerLocalVideo.setAllEnabled(false) // hủy sự kiện click localVideo
                updateEmoji?.invoke() // update emoji
            }
            icClose.visibility = View.VISIBLE
            viewCorner.visibility = View.GONE
            isOpenRelativeLayoutLocalVideo = !isOpenRelativeLayoutLocalVideo
            delayShow()
        }
        icClose.setOnClickListener {
            hideContainerLocalMiniSize()
            if (isOpenRelativeLayoutLocalVideo) {
                resetTranslationZ() // reset lại translationZ khi containerLocal matchParent và đã ngừng hiển thị localVideo
                emojiCurrentUser.layoutParams.width =
                    emojiCurrentWidth.toString().toFloat().toInt() // set lại emojiCurrent params
                emojiCurrentUser.layoutParams.height =
                    emojiCurrentHeight.toString().toFloat().toInt()
                emojiCurrentUser.x = emojiCurrentX.toString().toFloat()
                emojiCurrentUser.y = emojiCurrentY.toString().toFloat()
                localRenderView.layoutParams =
                    layoutParamsViewCorner // reset lại layoutParams margin cho localRenderView
                containerLocalVideo.layoutParams = layoutParamsContainerLocalVideo
                if (!channelData!!.openCamera) {
                    STATUS_DISPLAY = BOTH_MINI_SIZE_LOCAL_TURN_OFF_AND_REMOTE
                    showChime() // reshow chime bởi vì videoLocal đã tắt => remoteVideoTile thay đổi
                    hideContainerLocalMiniSize()
                } else {
                    STATUS_DISPLAY = BOTH_MINI_SIZE_LOCAL_AND_REMOTE_
                    delayShow()
                }
            }
            if (hasRemoteVideo(focusAttendee!!)) { // gone invisibleCamera khi tắt camera và has remoteVideo
                invisibleCamera.visibility = View.GONE
            }
            containerLocalVideo.setAllEnabled(true) // kích hoạt cho phép click localVideo
            emojiCurrentUser.layoutParams = layoutParamsEmoji
            icClose.visibility = View.GONE
            viewCorner.visibility = View.VISIBLE
            containerLocalVideo.isClickable = true
            isOpenRelativeLayoutLocalVideo = !isOpenRelativeLayoutLocalVideo
            updateEmoji?.invoke() // update imoji
        }
    }

    fun hasRemoteVideo(attendee: RosterAttendee): Boolean {
        remoteVideoTileStates.forEach {
            if (attendee.attendeeId == it.videoTileState.attendeeId) {
                return true
            }
        }
        return false
    }

    private fun setUpBegin(chimeMeetConferenceOptions: ChimeMeetConferenceOptions) {
        audioVideo = meetingSession!!.audioVideo
        callingState = CallingState()
        audioDevices = audioVideo.listAudioDevices()
        audioSelectDialog = AudioSelectDialog(audioDevices, this)
        setUpConfig()
        subscribeListener()
        setUpLocalVideo(chimeMeetConferenceOptions)
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
        rcSlide.alpha = 0.5F
        userJoinedAdapter = UserJoinedAdapter(currentRosters)
        rcSlide.adapter = userJoinedAdapter

        notificationUserJoin = {
            userJoinedAdapter!!.notifyDataSetChanged()
        }

        icMoveView.setOnClickListener {
            val displayMetrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val width = displayMetrics.widthPixels
            val moveViewXSize: Int = icMoveView.measuredWidth
            val slideXSize = rcSlide.measuredWidth
            if (isOpenListUser) {
                rcSlide.animate().x(width.toFloat() - Convert.convertDpToPixel(2f, context))
                    .duration = 300
                icMoveView.animate()
                    .x(width - moveViewXSize.toFloat() - Convert.convertDpToPixel(2f, context))
                    .duration = 300
                isOpenListUser = !isOpenListUser
                icMoveView.setImageResource(R.drawable.ic_baseline_keyboard_arrow_right_24)
            } else {
                icMoveView.animate().x((width - moveViewXSize - slideXSize.toFloat())).duration =
                    300

                rcSlide.animate().x((width - slideXSize).toFloat()).duration = 300
                isOpenListUser = !isOpenListUser
                icMoveView.setImageResource(R.drawable.ic_baseline_keyboard_arrow_left_24)
            }
        }
    }

    private fun setUpLocalVideo(chimeMeetConferenceOptions: ChimeMeetConferenceOptions) {
        if (chimeMeetConferenceOptions.videoMuted!!) {
            audioVideo.startLocalVideo(cameraCaptureSource)
            cameraCaptureSource.start()
        }
    }

    fun setConfigMuteChange(check: Boolean) {
        with(meetingSession!!.audioVideo) {
            if (check)
                realtimeLocalUnmute() // start mic
            else
                realtimeLocalMute() // stop mic
        }
        callingState?.isSpeaking = !check
        updateCallingState?.invoke(callingState!!)
    }

    // Check khi thay bam nut video
    private fun setConfigVideoChange(check: Boolean) {
        with(meetingSession!!.audioVideo) {
            if (memberJoined == 1) { // Số người join = 1
                if (check) {
                    invisibleCamera.visibility = View.GONE
                    hideContainerLocalMiniSize()
                    startLocalVideo(cameraCaptureSource) // start camera
                    cameraCaptureSource.start()
                } else {
                    invisibleCamera.visibility = View.VISIBLE
                    hideContainerLocalMiniSize()
                    cameraCaptureSource.stop() // stop camera
                    stopLocalVideo()
                }
            }
            if (memberJoined > 1) { // Số người join > 1
                if (containerLocalVideo.layoutParams != layoutParamsMaxParentRelativeLayout) { // Nếu layout params containerLocalVideo là minimum size
                    if (check) {
                        containerLocalVideo.visibility = View.VISIBLE
                        localRenderView.visibility = View.VISIBLE
                        showContainerLocalMiniSize()
                        startLocalVideo(cameraCaptureSource) // start camera
                        cameraCaptureSource.start()
                    } else {
                        containerLocalVideo.visibility = View.GONE
                        localRenderView.visibility = View.GONE
                        hideContainerLocalMiniSize()
                        cameraCaptureSource.stop() // stop camera
                        stopLocalVideo()
                    }
                } else { // Nếu layout params containerLocalVideo là maxparent
                    if (check) {
                        resetTranslationZ()
                        invisibleCamera.visibility = View.GONE
                        startLocalVideo(cameraCaptureSource) // start camera
                        cameraCaptureSource.start()
                    } else {
                        setTranslationZWhenClickLocalVideo()
                        invisibleCamera.visibility = View.VISIBLE
                        cameraCaptureSource.stop() // stop camera
                        stopLocalVideo()
                    }
                }
            }
        }
        callingState?.isStreaming = !check
        updateCallingState?.invoke(callingState!!)
    }

    private fun setTranslationZWhenClickLocalVideo() {
        invisibleCamera.translationZ = 1f
        btnSpeaker.translationZ = 1f
        icMoveView.translationZ = 1f
        rcSlide.translationZ = 1f
        icClose.translationZ = 1f
    }

    private fun resetTranslationZ() {
        if (invisibleCamera.translationZ != 0f || btnSpeaker.translationZ != 0f || icClose.translationZ != 0f || icMoveView.translationZ != 0f || rcSlide.translationZ != 0f) {
            invisibleCamera.translationZ = 0f
            btnSpeaker.translationZ = 0f
            icMoveView.translationZ = 0f
            rcSlide.translationZ = 0f
            icClose.translationZ = 0f
        }
    }

    private fun urlReWriter(url: String): String {
        // You can change urls by url.replace("example.com", "my.example.com")
        return url
    }

    private suspend fun joinMeeting(
        meetingUrl: String,
        meetingId: String?,
        attendeeName: String?,
        MEETING_REGION: String
    ): String? {
        return withContext(Dispatchers.IO) {
            val url = if (meetingUrl.endsWith("/")) meetingUrl else "$meetingUrl/"
            val serverUrl =
                URL(
                    "${url}join?title=${encodeURLParam(meetingId)}&name=${encodeURLParam(
                        attendeeName
                    )}&region=${encodeURLParam(MEETING_REGION)}"
                )
            try {
                val response = StringBuffer()
                with(serverUrl.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true

                    BufferedReader(InputStreamReader(inputStream)).use {
                        var inputLine = it.readLine()
                        while (inputLine != null) {
                            response.append(inputLine)
                            inputLine = it.readLine()
                        }
                        it.close()
                    }

                    if (responseCode == 201) {
                        Log.d("response.toString()", response.toString())
                        response.toString()
                    } else {
                        Log.d("TAG", "Unable to join meeting. Response code: $responseCode")
                        null
                    }
                }
            } catch (exception: Exception) {
                Log.d("TAG", "There was an exception while joining the meeting: $exception")
                null
            }
        }
    }

    private fun encodeURLParam(string: String?): String {
        return URLEncoder.encode(string, "utf-8")
    }

    private fun sendInformation(
        meetingIdEditText: String,
        nameEditText: String,
        region: String,
        applicationContext: Context
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val response = joinMeeting(
                " https://calling-chime-demo.dev.calling.fun/",
                meetingIdEditText,
                nameEditText,
                region
            )
            if (response != null) {
                Log.d("XXXXX", "Thông tin phòng đúng ")
                val joinMeetingResponse = gson.fromJson(response, JoinMeetingResponse::class.java)
                val sessionConfig = MeetingSessionConfiguration(
                    CreateMeetingResponse(joinMeetingResponse.joinInfo.meetingResponse.meeting),
                    CreateAttendeeResponse(joinMeetingResponse.joinInfo.attendeeResponse.attendee),
                    ::urlReWriter
                )
                meetingSession = sessionConfig.let {
                    Log.d("TAG", "Creating meeting session for meeting Id: $meetingIdEditText")
                    DefaultMeetingSession(
                        it,
                        logger,
                        applicationContext,
                        eglCoreFactory
                    )
                }

                val surfaceTextureCaptureSourceFactory =
                    DefaultSurfaceTextureCaptureSourceFactory(logger, eglCoreFactory)
                cameraCaptureSource = DefaultCameraCaptureSource(
                    applicationContext,
                    logger,
                    surfaceTextureCaptureSourceFactory
                )
                Log.d("TAG", "meetingSession: $meetingSession")
                cpuVideoProcessor = CpuVideoProcessor(logger, eglCoreFactory)
                gpuVideoProcessor = GpuVideoProcessor(logger, eglCoreFactory)
                val result = gson.fromJson<Response>(response, Response::class.java)
                SharedPreferencesManager.getInstance(applicationContext)
                    .putObject(SharePreferenceKey.RESPONSE, result)
                initChimeView?.invoke()
                Log.d("xxxxx", "Response:: $result")
            } else {
                Log.d("XXXXX", "Thông tin phòng sai ")
            }
        }
    }

    var initChimeView: (() -> Unit)? = null

    fun join(chimeMeetConferenceOptions: ChimeMeetConferenceOptions) {
        // Lần đầu vào join
        if (firstTimeJoin) {
            firstTimeJoin = false
            sendInformation(
                chimeMeetConferenceOptions.room!!,
                chimeMeetConferenceOptions.name!!,
                chimeMeetConferenceOptions.region!!,
                this.context
            )
            channelData = ChannelData(
                chimeMeetConferenceOptions.videoMuted!!,
                chimeMeetConferenceOptions.audioOnly!!
            )
            initChimeView = {
                memberJoined++
                inforMeeting = SharedPreferencesManager.getInstance(this.context)
                    .getObject(SharePreferenceKey.RESPONSE, Response::class.java)
                containerLocalVideo.visibility = View.GONE
                setUpBegin(chimeMeetConferenceOptions)
                handleMoveView()
                handleChooseAudio()
                handleSwipeCamera()
                handleClickLocalVideo()
                handleDisplay(chimeMeetConferenceOptions)
                handleClickUser()
                updateEmoji()
            }
        } else { // Lần thứ 2 vào join
            channelData = ChannelData(
                chimeMeetConferenceOptions.videoMuted!!,
                chimeMeetConferenceOptions.audioOnly!!
            )
            handleDisplay(chimeMeetConferenceOptions)
            handleSetStatusDisplay()
            updateEmoji?.invoke()
        }
    }

    private fun handleSetStatusDisplay() {
        if (memberJoined > 1) {
            STATUS_DISPLAY = if (channelData!!.openCamera) {
                BOTH_MINI_SIZE_LOCAL_AND_REMOTE_
            } else {
                BOTH_MINI_SIZE_LOCAL_TURN_OFF_AND_REMOTE
            }
        }
        if (memberJoined == 1) {
            STATUS_DISPLAY = ONLY_LOCAL
        }
    }

    private fun handleDisplay(options: ChimeMeetConferenceOptions) {
        setConfigVideoChange(options.videoMuted!!)
        setConfigMuteChange(options.audioOnly!!)
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
        // check focus video then update surface view
        if (focusAttendee != null && focusAttendee?.attendeeId == tileState.attendeeId) {
            createVideoCollectionTile(tileState).let {
                // hide non video when turn on again camera
                bind(it, renderViewLayout!!.findViewById(R.id.chime))
                invisibleCamera.visibility = View.GONE
            }
        }
        Log.d(TAG, "yyyy " + tileState.isLocalTile)
        Log.d(TAG, "zzzz " + remoteVideoTileStates.size)

        /**
         *sort remoteVideoTileState để vị trí 0 luôn là vị trí video local
         *
         */

        remoteVideoTileStates.forEachIndexed { index, videoTileState ->
            if (videoTileState.videoTileState.isLocalTile) {
                if (index == 0) return@forEachIndexed
                else {
                    Collections.swap(remoteVideoTileStates, 0, index)
                    return@forEachIndexed
                }
            }
        }

        if ((remoteVideoTileStates.size == 1 && tileState.isLocalTile) || tileState.isLocalTile) {
            showOnlyLocal()
        }
        if (callingState == null) {
            val inforMeeting = SharedPreferencesManager.getInstance(this.context)
                .getObject(SharePreferenceKey.RESPONSE, Response::class.java)
            inforMeeting?.let {
                callingState = CallingState(
                    getAttendeeName(
                        it.joinInfo.attendee.attendee.attendeeId,
                        it.joinInfo.attendee.attendee.externalUserId
                    ), it.joinInfo.attendee.attendee.attendeeId
                )
            }
        }
        if (videoCollectionTile.videoTileState.isLocalTile) {
            callingState?.isStreaming = true
        }
    }

    private fun showOnlyLocal() {
        if (memberJoined > 1) {
            bind(remoteVideoTileStates[0], renderViewLocalLayout.findViewById(R.id.chime_2))
            showContainerLocalMiniSize()
        } else {
            invisibleCamera.visibility = View.GONE
            hideContainerLocalMiniSize()
            bind(remoteVideoTileStates[0], renderViewLayout!!.findViewById(R.id.chime))
        }
    }

    private fun <T> List<T>.replace(newValue: T, block: (T) -> Boolean): List<T> {
        return map {
            if (block(it)) newValue else it
        }
    }

    private fun hideContainerLocalMiniSize() {
        containerLocalVideo.visibility = View.GONE
        renderViewLocalLayout.findViewById<DefaultVideoRenderView>(R.id.chime_2).visibility =
            View.GONE
        containerLocalVideo.isClickable = false
    }

    private fun showContainerLocalMiniSize() {
        containerLocalVideo.visibility = View.VISIBLE
        renderViewLocalLayout.findViewById<DefaultVideoRenderView>(R.id.chime_2).visibility =
            View.VISIBLE
        containerLocalVideo.isClickable = true
    }

    private fun showChime() {
        // khi có 1 người join
        if (memberJoined == 1) {
            hideContainerLocalMiniSize()
            if (channelData!!.openCamera) {
                invisibleCamera.visibility = View.GONE
                bind(remoteVideoTileStates[0], renderViewLayout!!.findViewById(R.id.chime))
            } else {
                invisibleCamera.visibility = View.VISIBLE
            }
        }
        // khi có >1 người join
        if (memberJoined > 1) {
            // show container LocalVideo
            if (channelData!!.openCamera) {
                showContainerLocalMiniSize()
            }
            var isHaveRemoteVideo = false
            remoteVideoTileStates.forEach {
                if (it.videoTileState.attendeeId == focusAttendee!!.attendeeId) {
                    isHaveRemoteVideo = true
                    bind(
                        it,
                        renderViewLayout!!.findViewById(R.id.chime)
                    ) // Bind video remote focus Attendee
                    invisibleCamera.visibility =
                        View.GONE // Ân invisibleCamera trong trường hợp icon close được nhấn.
                }
            }
            if (!isHaveRemoteVideo) {
                invisibleCamera.visibility = View.VISIBLE
            }
            if (channelData!!.openCamera) {
                bind(remoteVideoTileStates[0], renderViewLocalLayout.findViewById(R.id.chime_2))
            } else {
                containerLocalVideo.visibility = View.GONE
            }
        }
    }

    private fun bind(
        videoCollectionTile: VideoCollectionTile,
        videoRenderView: DefaultVideoRenderView
    ) {
        videoCollectionTile.videoRenderView = videoRenderView
        audioVideo.bindVideoView(videoRenderView, videoCollectionTile.videoTileState.tileId)
    }

    private fun unbind(videoCollectionTile: VideoCollectionTile) {
        audioVideo.unbindVideoView(videoCollectionTile.videoTileState.tileId)
    }

    override fun onVideoTilePaused(tileState: VideoTileState) {
        Log.d(TAG, "onVideoTilePaused: ")
    }

    private fun getVideoCollectionTileStateFromVideoTileState(tileState: VideoTileState): VideoCollectionTile? {
        if (remoteVideoTileStates.size > 0) {
            remoteVideoTileStates.forEach {
                if (it.videoTileState.attendeeId == tileState.attendeeId) {
                    return it
                }
            }
        }
        return null
    }

    override fun onVideoTileRemoved(tileState: VideoTileState) {
        Log.d(TAG, "onVideoTileRemoved: ${tileState.attendeeId}")
        if (tileState.attendeeId == callingState?.idAttendee) {
            callingState?.isStreaming = false
        }
        if (tileState.attendeeId == focusAttendee?.attendeeId) {
            // unbind video khi một video ngừng hiển thị từ web
            getVideoCollectionTileStateFromVideoTileState(tileState)?.let {
                unbind(it)
            }
            invisibleCamera.visibility = View.VISIBLE
        }
        val mutableIterator = remoteVideoTileStates.iterator()
        while (mutableIterator.hasNext()) {
            val current = mutableIterator.next()
            if (current.videoTileState.attendeeId == tileState.attendeeId) {
                unbind(current)
                mutableIterator.remove()
                return
            }
        }
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
        attendeeInfo.forEach {
            Log.d(TAG, "attendeeInfo: $it")
        }
        if (callingState == null) {
            inforMeeting?.let {
                callingState = CallingState(
                    getAttendeeName(
                        it.joinInfo.attendee.attendee.attendeeId,
                        it.joinInfo.attendee.attendee.externalUserId
                    ), it.joinInfo.attendee.attendee.attendeeId
                )
            }
        }
        Log.d(TAG, "member$memberJoined")
        attendeeInfo.forEach { (attendeeId, externalUserId) ->
            currentRoster.getOrPut(attendeeId,
                { RosterAttendee(attendeeId, getAttendeeName(attendeeId, externalUserId)) })
        }
        currentRosters.clear()
        memberJoined = 0
        currentRoster.forEach { (_, roster) ->
            Log.d(TAG, "xxxxxxxxx " + roster.attendeeName)
            currentRosters.add(roster)
            memberJoined++
        }
        /* sort currentRosters
        */
        currentRosters.forEachIndexed { index, currentRoster ->
            if (currentRoster.attendeeId == inforMeeting?.joinInfo?.attendee?.attendee?.attendeeId) {
                if (index == 0) return@forEachIndexed
                else {
                    Collections.swap(currentRosters, 0, index)
                    return@forEachIndexed
                }
            }
        }
        if (focusAttendee == null) {
            focusAttendee = currentRosters[0]
        }
        if (firstTimeSet && focusAttendee != null && memberJoined > 1) {
            focusAttendee = currentRosters[1]
            resetChimeContainer()
            firstTimeSet = false
        }
        if (memberJoined > 1) {
            focusAttendee = currentRosters[1]
        }
        if (memberJoined > 0) {
            showChime()
        }
        Log.d(TAG, "member$memberJoined")
        callingState?.allAttendees = currentRoster.values.toList()
        updateCallingState?.invoke(callingState!!)
        Log.d(TAG, "onAttendeesJoined" + callingState.toString() + currentRoster.size)
        handleSetStatusDisplay()
        notificationUserJoin?.invoke(currentRosters)
        updateEmoji?.invoke() // update emoji
    }

    private fun resetChimeContainer() {
        val inflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        chimeContainer.removeAllViews()
        renderViewLayout = inflater.inflate(R.layout.chime_view, chimeContainer, true)
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
        memberJoined -= attendeeInfo.size
        // xóa currentRoster
        attendeeInfo.forEach { (attendeeId, _) ->
            currentRoster.remove(attendeeId)
        }
        // set lại currentRosters
        currentRosters.clear()
        currentRoster.forEach { (_, roster) ->
            Log.d(TAG, "xxxxxxxxx " + roster.attendeeName)
            currentRosters.add(roster)
        }

        /*
        sort currentRosters
        */
        currentRosters.forEachIndexed { index, currentRoster ->
            if (currentRoster.attendeeId == inforMeeting?.joinInfo?.attendee?.attendee?.attendeeId) {
                if (index == 0) return@forEachIndexed
                else {
                    Collections.swap(currentRosters, 0, index)
                    return@forEachIndexed
                }
            }
        }

        // Khi một thằng rời đi -> kiểm tra thằng nào rời. có phải là thằng đang hiển thị video không
        attendeeInfo.forEach {
            if (it.attendeeId == focusAttendee?.attendeeId) {
                if (currentRosters.size > 1) {
                    // gán focusAttendee khi còn trên 2 người trong room
                    focusAttendee = currentRosters[1]
                    // gone invisibleCamera tai vì đã visible camera khi onVideoTileRemove
                    invisibleCamera.visibility = View.GONE
                    showChime()
                } else {
                    // gán focusAttendee khi còn đúng bản thân trong room
                    focusAttendee = currentRosters[0]
                    showChime()
                }
                return@forEach
            }
        }
        notificationUserJoin?.invoke(currentRosters)
        handleSetStatusDisplay() // type màn hình đang hiển thị.
        updateEmoji?.invoke()
    }

    override fun onAttendeesMuted(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach {
            if (it.attendeeId == callingState?.idAttendee) {
                callingState?.isSpeaking = callingState?.isSpeaking?.not()!!
                updateCallingState?.invoke(callingState!!)
                Log.d(TAG, "onAttendeesMuted: $callingState")
            }
        }
        Log.d(TAG, "onAttendeesMuted: ")
    }

    override fun onAttendeesUnmuted(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach {
            if (it.attendeeId == callingState?.idAttendee) {
                callingState?.isSpeaking = callingState?.isSpeaking?.not()!!
                updateCallingState?.invoke(callingState!!)
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
//        val inforMeeting = SharedPreferencesManager.getInstance(this.context)
//            .getObject(SharePreferenceKey.RESPONSE, Response::class.java)
//        inforMeeting?.let {
//            callingState = CallingState(
//                getAttendeeName(
//                    it.joinInfo.attendee.attendee.attendeeId,
//                    it.joinInfo.attendee.attendee.externalUserId
//                ), it.joinInfo.attendee.attendee.attendeeId
//            )
//            currentRosters.add(
//                RosterAttendee(
//                    inforMeeting.joinInfo.attendee.attendee.externalUserId,
//                    getAttendeeName(
//                        it.joinInfo.attendee.attendee.attendeeId,
//                        it.joinInfo.attendee.attendee.externalUserId
//                    )
//                )
//            )
//            notificationUserJoin?.invoke(currentRosters)
//        }
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
//        slideCustomView = chimeLayout.findViewById(R.id.slideCustomView)
        btnSpeaker = chimeLayout.findViewById(R.id.btnSpeaker)
        swipeCamera = chimeLayout.findViewById(R.id.swipe_camera)
        icClose = chimeLayout.findViewById(R.id.icClose)
        emoji = chimeLayout.findViewById(R.id.emoji)
        emojiCurrentUser = chimeLayout.findViewById(R.id.emojiCurrentUser)
        emojiCurrentUserWhenTurnOffCamera =
            chimeLayout.findViewById(R.id.emojiCurrentUserWhenTurnOffCamera)
        containerLocalVideo = chimeLayout.findViewById(R.id.containerLocalVideo)
        invisibleCamera = chimeLayout.findViewById(R.id.invisibleCamera)
        test = chimeLayout.findViewById(R.id.test)
//        constraintLayoutMove = chimeLayout.findViewById(R.id.constraintLayoutMove)
        rcSlide = chimeLayout.findViewById(R.id.rcSlide)
        mainChimeLayout = chimeLayout.findViewById(R.id.mainChimeLayout)
        viewCorner = chimeLayout.findViewById(R.id.viewCorner)
        renderViewLayout = inflater.inflate(R.layout.chime_view, chimeContainer, true)
        renderViewLocalLayout = inflater.inflate(R.layout.chime_view_2, localVideo, true)
//        renderViewLocalLayout.background = ResourcesCompat.getDrawable(resources,R.drawable.border_local_video,context.theme)
        localRenderView = renderViewLocalLayout.findViewById(R.id.chime_2)
//        localRenderView.background = ResourcesCompat.getDrawable(resources,R.drawable.border_local_video,context.theme)

        // inflater customView slider add ViewSlide
//        userJoinedView = inflater.inflate(R.layout.view_users_joined, slideCustomView, true)
//        rcUsersJoined = userJoinedView.findViewById(R.id.rcUsersJoined)

        getDisplayValue()
    }

    private fun getDisplayValue() {
        positionEmojiX = emojiCurrentUser.x
        positionEmojiY = emojiCurrentUser.x
    }

    private fun setUpConfig() {
        gpuVideoProcessor = GpuVideoProcessor(logger, eglCoreFactory)
        cpuVideoProcessor = CpuVideoProcessor(logger, eglCoreFactory)
    }

    override fun onClickListener(position: Int) {
//        Toast.makeText(context  ,position, Toast.LENGTH_SHORT).show()
    }

    private fun displayEmoji(imageView: ImageView, data: String) {
        with(imageView) {
            when (data) {
                "f09f918c" -> setImageResource((R.drawable.react_features_conference_components_customize_noto_image_sets_1f44c))
                "f09f918d" -> setImageResource((R.drawable.react_features_conference_components_customize_noto_image_sets_1f44d))
                "f09f918e" -> setImageResource((R.drawable.react_features_conference_components_customize_noto_image_sets_1f44e))
                "f09f918f" -> setImageResource((R.drawable.react_features_conference_components_customize_noto_image_sets_1f44f))
                "f09f9880" -> setImageResource((R.drawable.react_features_conference_components_customize_noto_image_sets_1f600))
                "f09f988d" -> setImageResource((R.drawable.react_features_conference_components_customize_noto_image_sets_1f60d))
                "f09f98a0" -> setImageResource((R.drawable.react_features_conference_components_customize_noto_image_sets_1f621))
                "f09f98a8" -> setImageResource((R.drawable.react_features_conference_components_customize_noto_image_sets_1f628))
                "f09fa494" -> setImageResource((R.drawable.react_features_conference_components_customize_noto_image_sets_1f914))
                "e29c8b" -> setImageResource(R.drawable.react_features_conference_components_customize_noto_image_sets_270b)
                else -> setImageResource(0)
            }
        }
    }

    companion object {
        const val ONLY_LOCAL = 0
        const val BOTH_MINI_SIZE_LOCAL_TURN_OFF_AND_REMOTE = 1
        const val BOTH_MINI_SIZE_LOCAL_AND_REMOTE_ = 2
        const val BOTH_FULL_SIZE_LOCAL_AND_REMOTE_ = 3
    }
}

data class ChannelData(val openCamera: Boolean, val openMic: Boolean)
