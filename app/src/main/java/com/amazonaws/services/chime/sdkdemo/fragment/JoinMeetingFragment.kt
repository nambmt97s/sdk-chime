package com.amazonaws.services.chime.sdkdemo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.activity.HomeActivity

class JoinMeetingFragment : Fragment() {

    companion object {
        fun newInstance(meetingId: String): JoinMeetingFragment {
            val fragment = JoinMeetingFragment()

            fragment.arguments =
                    Bundle().apply { putString(HomeActivity.MEETING_ID_KEY, meetingId) }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(container?.context).inflate(R.layout.fragment_join_meeting, container, false)
    }
}
