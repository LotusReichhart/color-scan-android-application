package com.lotusreichhart.colorscan.feature.scan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lotusreichhart.colorscan.R
import com.lotusreichhart.colorscan.AdHelper
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
    private var hasRequestedPermission = false
    private var imageCapture: ImageCapture? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRequestedPermission = true
        if (isGranted) {
            binding.layoutPermissionDenied.visibility = View.GONE
            startCamera()
        } else {
            checkCameraPermissionState()
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
            val state = viewModel.uiState.value
            if (state.isFrozen) {
                // Currently frozen -> Unfreezing
                binding.ivFrozenPhoto.visibility = View.GONE
                binding.ivFrozenPhoto.setImageDrawable(null)
                val tempFile = java.io.File(requireContext().cacheDir, "temp_frozen.jpg")
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                viewModel.onEvent(ScanUiEvent.ToggleFreeze)
            } else {
                // Currently active -> Freezing
                viewModel.onEvent(ScanUiEvent.ToggleFreeze)
                captureTempFrozenPhoto()
            }
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

        binding.btnCopy.setOnClickListener {
            val state = viewModel.uiState.value
            val bottomSheet = ActionBottomSheet.newInstance(
                colorName = state.colorName,
                hex = state.colorHex,
                rgb = state.colorRgb,
                showSavePhoto = state.isFrozen
            )
            bottomSheet.setOnActionSelectedListener { actionType ->
                when (actionType) {
                    ActionType.COPY_NAME -> {
                        copyToClipboard("Name", state.colorName)
                        viewModel.onEvent(ScanUiEvent.SaveColor)
                        AdHelper.showInterstitialAd(requireActivity()) {}
                    }
                    ActionType.COPY_HEX -> {
                        copyToClipboard("HEX", state.colorHex)
                        viewModel.onEvent(ScanUiEvent.SaveColor)
                        AdHelper.showInterstitialAd(requireActivity()) {}
                    }
                    ActionType.COPY_RGB -> {
                        copyToClipboard("RGB", state.colorRgb)
                        viewModel.onEvent(ScanUiEvent.SaveColor)
                        AdHelper.showInterstitialAd(requireActivity()) {}
                    }
                    ActionType.SAVE_PHOTO -> {
                        saveFrozenPhotoPermanently(state.colorName, state.colorHex, state.colorRgb)
                        AdHelper.showInterstitialAd(requireActivity()) {}
                    }
                }
            }
            bottomSheet.show(parentFragmentManager, "ActionBottomSheet")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.btnFreeze.isSelected = state.isFrozen
                    binding.scanGrid.isSelected = state.isFrozen
                    binding.badgeFrozen.visibility = if (state.isFrozen) View.VISIBLE else View.GONE

                    if (!state.isFrozen) {
                        binding.ivFrozenPhoto.visibility = View.GONE
                        binding.ivFrozenPhoto.setImageDrawable(null)
                    }

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
            binding.layoutPermissionDenied.visibility = View.GONE
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

            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            this@ScanFragment.imageCapture = imageCapture

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
                    viewLifecycleOwner, cameraSelector, preview, imageAnalysis, imageCapture
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

    private fun captureTempFrozenPhoto() {
        val imageCapture = imageCapture ?: return
        val tempFile = java.io.File(requireContext().cacheDir, "temp_frozen.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    if (_binding == null) return
                    overlayTargetPoint(tempFile)
                    
                    binding.ivFrozenPhoto.setImageURI(null)
                    binding.ivFrozenPhoto.setImageURI(Uri.fromFile(tempFile))
                    binding.ivFrozenPhoto.visibility = View.VISIBLE
                }

                override fun onError(exception: ImageCaptureException) {
                    Timber.e(exception, "Failed to capture temp frozen photo")
                }
            }
        )
    }

    private fun saveFrozenPhotoPermanently(colorName: String, colorHex: String, colorRgb: String) {
        val tempFile = java.io.File(requireContext().cacheDir, "temp_frozen.jpg")
        if (!tempFile.exists()) {
            Toast.makeText(requireContext(), "Frozen photo not found", Toast.LENGTH_SHORT).show()
            return
        }

        val filename = "color_scan_${System.currentTimeMillis()}.jpg"
        val permanentFile = java.io.File(requireContext().filesDir, filename)

        try {
            tempFile.copyTo(permanentFile, overwrite = true)

            viewModel.saveColorWithPhoto(colorHex, colorName, colorRgb, permanentFile.absolutePath)
            Toast.makeText(
                requireContext(),
                "Successfully saved photo and color to History!",
                Toast.LENGTH_SHORT
            ).show()

            // Automatically clean up and unfreeze
            binding.ivFrozenPhoto.visibility = View.GONE
            binding.ivFrozenPhoto.setImageDrawable(null)
            tempFile.delete()
            viewModel.onEvent(ScanUiEvent.ToggleFreeze)
        } catch (e: Exception) {
            Timber.e(e, "Error saving frozen photo permanently")
            Toast.makeText(requireContext(), "Failed to save photo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun overlayTargetPoint(file: java.io.File) {
        try {
            // Read orientation metadata using built-in ExifInterface
            val exif = android.media.ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
            val rotationDegrees = when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap == null) return

            // Rotate bitmap if necessary
            val workingBitmap = if (rotationDegrees != 0) {
                val matrix = android.graphics.Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                val rotated = android.graphics.Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                bitmap.recycle()
                rotated
            } else {
                bitmap
            }

            // Crop to center-square (1:1 ratio) matching the visible preview screen
            val width = workingBitmap.width
            val height = workingBitmap.height
            val squareSize = Math.min(width, height)
            val x = (width - squareSize) / 2
            val y = (height - squareSize) / 2

            val croppedBitmap = android.graphics.Bitmap.createBitmap(
                workingBitmap, x, y, squareSize, squareSize
            )
            workingBitmap.recycle()

            val mutableBitmap = croppedBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
            croppedBitmap.recycle()

            val canvas = android.graphics.Canvas(mutableBitmap)
            val scale = mutableBitmap.width / 1080f

            val centerX = mutableBitmap.width / 2f
            val centerY = mutableBitmap.height / 2f
            val lineLength = 30f * scale
            val circleRadius = 12f * scale

            // 1. Draw black background reticle (slightly thicker stroke for shadow/contrast)
            val shadowPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                strokeWidth = 6f * scale
                style = android.graphics.Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawLine(centerX - lineLength, centerY, centerX + lineLength, centerY, shadowPaint)
            canvas.drawLine(centerX, centerY - lineLength, centerX, centerY + lineLength, shadowPaint)
            canvas.drawCircle(centerX, centerY, circleRadius, shadowPaint)

            // 2. Draw white foreground reticle (thinner stroke overlayed)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                strokeWidth = 3f * scale
                style = android.graphics.Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawLine(centerX - lineLength, centerY, centerX + lineLength, centerY, paint)
            canvas.drawLine(centerX, centerY - lineLength, centerX, centerY + lineLength, paint)
            canvas.drawCircle(centerX, centerY, circleRadius, paint)

            java.io.FileOutputStream(file).use { out ->
                mutableBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            mutableBitmap.recycle()

            // Reset EXIF orientation back to normal since we physically rotated the pixels
            val newExif = android.media.ExifInterface(file.absolutePath)
            newExif.setAttribute(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL.toString()
            )
            newExif.saveAttributes()

        } catch (e: Exception) {
            Timber.e(e, "Error overlaying target point on captured photo")
        }
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

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            binding.layoutPermissionDenied.visibility = View.GONE
            startCamera()
        } else if (hasRequestedPermission) {
            checkCameraPermissionState()
        }
    }

    private fun checkCameraPermissionState() {
        if (allPermissionsGranted()) {
            binding.layoutPermissionDenied.visibility = View.GONE
            startCamera()
        } else {
            binding.layoutPermissionDenied.visibility = View.VISIBLE
            val showRationale = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            if (showRationale) {
                binding.btnPermissionAction.text = "GRANT PERMISSION"
                binding.btnPermissionAction.setOnClickListener {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            } else {
                binding.btnPermissionAction.text = "GO TO SETTINGS"
                binding.btnPermissionAction.setOnClickListener {
                    openAppSettings()
                }
            }
        }
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open app settings")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        camera = null
        cameraExecutor.shutdown()
    }
}