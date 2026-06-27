package com.lotusreichhart.colorscan.feature.history

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.lotusreichhart.colorscan.AdHelper
import com.lotusreichhart.colorscan.databinding.ActivityPhotoDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoDetailBinding

    companion object {
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_HEX = "extra_hex"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_RGB = "extra_rgb"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPhotoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH) ?: ""
        val hex = intent.getStringExtra(EXTRA_HEX) ?: "#FFFFFF"
        val name = intent.getStringExtra(EXTRA_NAME) ?: "Unknown"
        val rgb = intent.getStringExtra(EXTRA_RGB) ?: ""
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0L)

        binding.btnClose.setOnClickListener {
            finish()
        }

        // Set Image Asynchronously
        val file = File(imagePath)
        if (file.exists()) {
            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        loadThumbnail(file, 1080)
                    } catch (e: Exception) {
                        Timber.e(e, "Error loading full size image")
                        null
                    }
                }
                if (bitmap != null) {
                    binding.ivDetailPhoto.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(this@PhotoDetailActivity, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Physical photo file not found", Toast.LENGTH_SHORT).show()
        }

        // Set Details text
        binding.tvColorName.text = name
        binding.tvColorHex.text = "HEX: $hex"
        binding.tvColorRgb.text = "RGB: $rgb"

        // Set formatted Timestamp
        val dateText = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.US).format(Date(timestamp))
        binding.tvTimestamp.text = "SAVED ON $dateText"

        // Set Color Preview Circle
        val colorInt = try {
            hex.toColorInt()
        } catch (e: Exception) {
            Timber.e(e)
            Color.WHITE
        }
        val circleDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(colorInt)
            setStroke(2, Color.parseColor("#444444"))
        }
        binding.colorPreviewCircle.background = circleDrawable

        // Setup Copy listener
        binding.btnCopyDetail.setOnClickListener { view ->
            val popup = androidx.appcompat.widget.PopupMenu(this, view)
            popup.menu.add(0, 1, 0, "Copy Name")
            popup.menu.add(0, 2, 0, "Copy HEX")
            popup.menu.add(0, 3, 0, "Copy RGB")
            popup.setOnMenuItemClickListener { item ->
                val copied = when (item.itemId) {
                    1 -> {
                        copyToClipboard("Name", name)
                        true
                    }
                    2 -> {
                        copyToClipboard("HEX", hex)
                        true
                    }
                    3 -> {
                        copyToClipboard("RGB", rgb)
                        true
                    }
                    else -> false
                }
                if (copied) {
                    AdHelper.showInterstitialAd(this) {}
                }
                true
            }
            popup.show()
        }

        // Load adaptive banner ad at the bottom
        AdHelper.loadBannerAd(binding.adViewContainer)
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "$label copied: $text", Toast.LENGTH_SHORT).show()
    }

    private fun loadThumbnail(file: File, targetSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        
        val width = options.outWidth
        val height = options.outHeight
        
        var inSampleSize = 1
        if (width > targetSize || height > targetSize) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while (halfWidth / inSampleSize >= targetSize && halfHeight / inSampleSize >= targetSize) {
                inSampleSize *= 2
            }
        }
        
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }
}
