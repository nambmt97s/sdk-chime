package com.amazonaws.services.chime.sdk.meetings

import android.content.Context
import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultEglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.internal.utils.CpuVideoProcessor
import com.amazonaws.services.chime.sdk.meetings.session.* // ktlint-disable
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.google.gson.Gson
import com.nam.demochime.utils.GpuVideoProcessor
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SetUpAudio {

    private lateinit var cameraCaptureSource: CameraCaptureSource
    private var gpuVideoProcessor: GpuVideoProcessor? = null
    private var cpuVideoProcessor: CpuVideoProcessor? = null
    private var meetingSession: MeetingSession? = null
    private val gson = Gson()
    private val logger = ConsoleLogger(LogLevel.DEBUG)
    private val eglCoreFactory: EglCoreFactory = DefaultEglCoreFactory()
    private var checkVideoStatus = true
    var startActivityMeeting: (() -> Unit)? = null

    fun sendInformation(
        meetingIdEditText: String,
        nameEditText: String,
        region: String,
        applicationContext: Context
    ) {
        startActivityMeeting?.invoke()

//        CoroutineScope(Dispatchers.Main).launch {
//            val response = joinMeeting(
//                " https://calling-chime-demo.dev.calling.fun/",
//                meetingIdEditText,
//                nameEditText,
//                region
//            )
//            if (response != null) {
//                Log.d("XXXXX", "Thông tin phòng đúng ")
//                val joinMeetingResponse = gson.fromJson(response, JoinMeetingResponse::class.java)
//                val sessionConfig = MeetingSessionConfiguration(
//                    CreateMeetingResponse(joinMeetingResponse.joinInfo.meetingResponse.meeting),
//                    CreateAttendeeResponse(joinMeetingResponse.joinInfo.attendeeResponse.attendee),
//                    ::urlReWriter
//                )
//                meetingSession = sessionConfig.let {
//                    Log.d("TAG", "Creating meeting session for meeting Id: $meetingIdEditText")
//                    DefaultMeetingSession(
//                        it,
//                        logger,
//                        applicationContext,
//                        eglCoreFactory
//                    )
//                }
//                setMeetingSession(meetingSession!!)
//                val surfaceTextureCaptureSourceFactory =
//                    DefaultSurfaceTextureCaptureSourceFactory(logger, eglCoreFactory)
//                cameraCaptureSource = DefaultCameraCaptureSource(
//                    applicationContext,
//                    logger,
//                    surfaceTextureCaptureSourceFactory
//                )
//                setCameraCaptureSource(cameraCaptureSource)
//                Log.d("TAG", "meetingSession: $meetingSession")
//                cpuVideoProcessor = CpuVideoProcessor(logger, eglCoreFactory)
//                gpuVideoProcessor = GpuVideoProcessor(logger, eglCoreFactory)
//                startActivityMeeting?.invoke()
//                val result = Gson().fromJson<Response>(response, Response::class.java)
//                SharedPreferencesManager.getInstance(applicationContext).putObject(SharePreferenceKey.RESPONSE, result)
//                Log.d("xxxxx", "Response:: $result")
//            } else {
//                Log.d("XXXXX", "Thông tin phòng sai ")
//            }
//        }
    }

    private fun setCameraCaptureSource(cameraCaptureSource: CameraCaptureSource) {
        this.cameraCaptureSource = cameraCaptureSource
    }

    fun getCameraCaptureSource() = cameraCaptureSource

    private fun urlReWriter(url: String): String {
        // You can change urls by url.replace("example.com", "my.example.com")
        return url
    }

    private fun setMeetingSession(meetingSession: MeetingSession) {
        this.meetingSession = meetingSession
    }

    fun meetingSession() = this.meetingSession

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

    companion object {
        var instanceAudio: SetUpAudio? = null
        fun getInstance(): SetUpAudio {
            if (instanceAudio == null) {
                instanceAudio = SetUpAudio()
                return instanceAudio!!
            }
            return instanceAudio!!
        }
    }

    fun toggleVideo() {
        if (checkVideoStatus) {
            toggleCameraOff()
            meetingSession?.audioVideo?.realtimeLocalMute()
        } else {
            toggleCameraOn()
            meetingSession?.audioVideo?.realtimeLocalUnmute()
        }
        cameraLocalClickListener?.invoke(checkVideoStatus)
        checkVideoStatus = !checkVideoStatus
    }

    private fun toggleCameraOn() {
        meetingSession?.audioVideo?.startLocalVideo(cameraCaptureSource)
        cameraCaptureSource.start()
    }

    private fun toggleCameraOff() {
        cameraCaptureSource.stop()
        meetingSession?.audioVideo?.stopLocalVideo()
    }

    var cameraLocalClickListener: ((Boolean) -> Unit)? = null
}
