package com.example.soulmate.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.soulmate.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AttachBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var shareImageOption: TextView
    private lateinit var shareVideoOption: TextView

    interface OnMediaSelectedListener {
        fun onImageSelected()
        fun onVideoSelected()
    }

    var listener: OnMediaSelectedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_attach_bottom_sheet, container, false)

        shareImageOption = view.findViewById(R.id.share_image_option)
        shareVideoOption = view.findViewById(R.id.share_video_option)

        shareImageOption.setOnClickListener {
            listener?.onImageSelected()
            dismiss()
        }

        shareVideoOption.setOnClickListener {
            listener?.onVideoSelected()
            dismiss()
        }

        return view
    }
}