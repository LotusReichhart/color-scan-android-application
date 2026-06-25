package com.lotusreichhart.colorscan.feature.scan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lotusreichhart.colorscan.R
import com.lotusreichhart.colorscan.core.util.ColorConverter
import com.lotusreichhart.colorscan.databinding.FragmentScanBinding
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.core.graphics.toColorInt

class ScanFragment : Fragment() {

    companion object {
        fun newInstance() = ScanFragment()
    }

    private val viewModel: ScanViewModel by viewModels()
    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!
    private var camera: Camera? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "Camera permission is required to scan colors.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnFreeze.setOnClickListener {
            viewModel.onEvent(ScanUiEvent.ToggleFreeze)
        }

        binding.btnFlash.setOnClickListener {
            viewModel.onEvent(ScanUiEvent.ToggleFlash)
        }

        binding.btnZoomIn.setOnClickListener {
            viewModel.onEvent(ScanUiEvent.ZoomIn)
        }

        binding.btnZoomOut.setOnClickListener {
            viewModel.onEvent(ScanUiEvent.ZoomOut)
        }

        binding.btnCopy.setOnClickListener { view ->
            val popup = androidx.appcompat.widget.PopupMenu(requireContext(), view)
            popup.menu.add(0, 1, 0, "Copy Name")
            popup.menu.add(0, 2, 0, "Copy HEX")
            popup.menu.add(0, 3, 0, "Copy RGB")
            popup.setOnMenuItemClickListener { item ->
                val state = viewModel.uiState.value
                val copied = when (item.itemId) {
                    1 -> {
                        copyToClipboard("Name", state.colorName)
                        true
                    }
                    2 -> {
                        copyToClipboard("HEX", state.colorHex)
                        true
                    }
                    3 -> {
                        copyToClipboard("RGB", state.colorRgb)
                        true
                    }
                    else -> false
                }
                if (copied) {
                    viewModel.onEvent(ScanUiEvent.SaveColor)
                }
                true
            }
            popup.show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.btnFreeze.isSelected = state.isFrozen
                    binding.scanGrid.isSelected = state.isFrozen
                    binding.badgeFrozen.visibility = if (state.isFrozen) View.VISIBLE else View.GONE

                    binding.btnFlash.setImageResource(
                        if (state.isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
                    )
                    camera?.cameraControl?.enableTorch(state.isFlashOn)

                    binding.tvZoom.text =
                        String.format(java.util.Locale.US, "%.1fx", state.zoomRatio)
                    camera?.cameraControl?.setZoomRatio(state.zoomRatio)

                    binding.tvColorName.text = state.colorName
                    binding.tvColorHex.text = "HEX: ${state.colorHex}"
                    binding.tvColorRgb.text = "RGB: ${state.colorRgb}"
                    updateColorPreview(state.colorHex)
                }
            }
        }

        binding.previewView.setOnTouchListener { view, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                camera?.let { cam ->
                    val factory = binding.previewView.meteringPointFactory
                    val point = factory.createPoint(event.x, event.y)
                    val action = androidx.camera.core.FocusMeteringAction.Builder(point, androidx.camera.core.FocusMeteringAction.FLAG_AF)
                        .setAutoCancelDuration(2, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    cam.cameraControl.startFocusAndMetering(action)
                }
                view.performClick()
                true
            } else {
                false
            }
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            if (_binding == null) return@addListener

            val cameraProvider = try {
                cameraProviderFuture.get()
            } catch (exc: Exception) {
                Timber.e(exc, "Camera initialization failed")
                return@addListener
            }

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }
            var lastAnalysisTime = 0L
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val currentTime = System.currentTimeMillis()
                    if (!viewModel.uiState.value.isFrozen && (currentTime - lastAnalysisTime >= 150L)) {
                        lastAnalysisTime = currentTime
                        val colorInt = ColorConverter.getCenterPixelColor(imageProxy)
                        val hex = ColorConverter.intToHex(colorInt)
                        val rgb = ColorConverter.intToRgbString(colorInt)
                        viewModel.updateScannedColor(hex, rgb)
                    }
                } catch (exc: Exception) {
                    Timber.e(exc, "Error during image analysis color extraction")
                } finally {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageAnalysis
                )
                applyCameraState()
            } catch (exc: Exception) {
                Timber.e(exc, "Use case binding failed")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun applyCameraState() {
        val state = viewModel.uiState.value
        camera?.cameraControl?.enableTorch(state.isFlashOn)
        camera?.cameraControl?.setZoomRatio(state.zoomRatio)
    }

    private fun updateColorPreview(colorHex: String) {
        val colorInt = try {
            colorHex.toColorInt()
        } catch (e: Exception) {
            Timber.e(e)
            "#FF4500".toColorInt()
        }

        val glowColor = (colorInt and 0x00FFFFFF) or 0x33000000

        val glowDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(glowColor)
        }

        val solidDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(colorInt)
        }

        val layerDrawable =
            android.graphics.drawable.LayerDrawable(arrayOf(glowDrawable, solidDrawable)).apply {
                val density = resources.displayMetrics.density
                val paddingPx = (8 * density).toInt()
                setLayerInset(1, paddingPx, paddingPx, paddingPx, paddingPx)
            }

        binding.colorPreviewCircle.background = layerDrawable
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard =
            requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "$label copied: $text", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        camera = null
        cameraExecutor.shutdown()
    }
}