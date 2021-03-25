package com.amazonaws.services.chime.sdk.meetings.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.FragmentManager
import com.amazonaws.services.chime.sdk.R
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

@Suppress("DEPRECATION")
class AudioSelectDialog(
    private val mediaDevices: List<MediaDevice>,
    private val onItemBottomClickListener: OnItemBottomClickListener
) :
    BottomSheetDialogFragment() {
    private lateinit var speakersLv: ListView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_shet_dialog, container, false)
        speakersLv = view.findViewById(R.id.rcSpeakers)
        val adapter = ArrayAdapter<MediaDevice>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            mediaDevices
        )
        speakersLv.adapter = adapter
        speakersLv.setOnItemClickListener { _, _, position, _ ->
            onItemBottomClickListener.onClickListener(position)
            dialog?.dismiss()
        }
        return view
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag)
        val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        dialog?.window?.decorView?.systemUiVisibility = flags
        this.dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (view!!.parent.parent.parent as View).fitsSystemWindows = false
    }
}

interface OnItemBottomClickListener {
    fun onClickListener(position: Int)
}
