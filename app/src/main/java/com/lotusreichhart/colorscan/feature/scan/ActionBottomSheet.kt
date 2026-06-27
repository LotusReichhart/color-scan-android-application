package com.lotusreichhart.colorscan.feature.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lotusreichhart.colorscan.R
import com.lotusreichhart.colorscan.databinding.DialogActionBottomSheetBinding

enum class ActionType {
    COPY_NAME,
    COPY_HEX,
    COPY_RGB,
    SAVE_PHOTO
}

class ActionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogActionBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var onActionSelected: ((ActionType) -> Unit)? = null

    companion object {
        private const val ARG_COLOR_NAME = "arg_color_name"
        private const val ARG_HEX = "arg_hex"
        private const val ARG_RGB = "arg_rgb"
        private const val ARG_SHOW_SAVE_PHOTO = "arg_show_save_photo"

        fun newInstance(
            colorName: String,
            hex: String,
            rgb: String,
            showSavePhoto: Boolean
        ): ActionBottomSheet {
            val fragment = ActionBottomSheet()
            fragment.arguments = Bundle().apply {
                putString(ARG_COLOR_NAME, colorName)
                putString(ARG_HEX, hex)
                putString(ARG_RGB, rgb)
                putBoolean(ARG_SHOW_SAVE_PHOTO, showSavePhoto)
            }
            return fragment
        }
    }

    fun setOnActionSelectedListener(listener: (ActionType) -> Unit) {
        onActionSelected = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogActionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val showSavePhoto = arguments?.getBoolean(ARG_SHOW_SAVE_PHOTO) ?: false

        if (showSavePhoto) {
            binding.btnSavePhoto.visibility = View.VISIBLE
        } else {
            binding.btnSavePhoto.visibility = View.GONE
        }

        binding.btnCopyName.setOnClickListener {
            onActionSelected?.invoke(ActionType.COPY_NAME)
            dismiss()
        }

        binding.btnCopyHex.setOnClickListener {
            onActionSelected?.invoke(ActionType.COPY_HEX)
            dismiss()
        }

        binding.btnCopyRgb.setOnClickListener {
            onActionSelected?.invoke(ActionType.COPY_RGB)
            dismiss()
        }

        binding.btnSavePhoto.setOnClickListener {
            onActionSelected?.invoke(ActionType.SAVE_PHOTO)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
