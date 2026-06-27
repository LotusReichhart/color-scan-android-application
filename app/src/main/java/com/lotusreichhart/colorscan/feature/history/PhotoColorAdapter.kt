package com.lotusreichhart.colorscan.feature.history

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.lotusreichhart.colorscan.R
import com.lotusreichhart.colorscan.core.model.PhotoHistoryEntity
import com.lotusreichhart.colorscan.databinding.ItemHistoryPhotoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PhotoColorAdapter(
    private val onItemClicked: (PhotoHistoryEntity) -> Unit,
    private val onCopyClicked: (View, PhotoHistoryEntity) -> Unit,
    private val onDeleteClicked: (PhotoHistoryEntity) -> Unit
) : RecyclerView.Adapter<PhotoColorAdapter.ViewHolder>() {

    private var items: List<PhotoHistoryEntity> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<PhotoHistoryEntity>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemHistoryPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var loadJob: Job? = null

        @SuppressLint("SetTextI18n")
        fun bind(photoItem: PhotoHistoryEntity) {
            // Cancel any pending load job for this recycled cell
            loadJob?.cancel()

            binding.tvColorName.text = photoItem.name
            binding.tvColorHex.text = "HEX: ${photoItem.hex}"
            binding.tvColorRgb.text = "RGB: ${photoItem.rgb}"
            binding.tvTimestamp.text = formatHistoryTimestamp(photoItem.timestamp)

            // Clear previous thumbnail during loading state
            binding.ivThumbnail.setImageDrawable(null)

            val file = File(photoItem.imagePath)
            if (file.exists()) {
                loadJob = CoroutineScope(Dispatchers.Main).launch {
                    val bitmap = withContext(Dispatchers.IO) {
                        try {
                            loadThumbnail(file, 128)
                        } catch (e: Exception) {
                            Timber.e(e, "Error loading thumbnail bitmap")
                            null
                        }
                    }
                    if (bitmap != null) {
                        binding.ivThumbnail.setImageBitmap(bitmap)
                    } else {
                        binding.ivThumbnail.setImageResource(R.drawable.logo)
                    }
                }
            } else {
                binding.ivThumbnail.setImageResource(R.drawable.logo)
            }

            // Setup Small Color Preview circle
            val colorInt = try {
                photoItem.hex.toColorInt()
            } catch (e: Exception) {
                Timber.e(e)
                Color.BLACK
            }
            val circleDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colorInt)
                setStroke(1, Color.parseColor("#444444"))
            }
            binding.colorPreview.background = circleDrawable

            binding.root.setOnClickListener {
                onItemClicked(photoItem)
            }

            binding.btnCopy.setOnClickListener { view ->
                onCopyClicked(view, photoItem)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClicked(photoItem)
            }
        }
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

    private fun formatHistoryTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        if (diff < 60000) {
            return "Just now"
        }

        val minutes = diff / 60000
        if (minutes < 60) {
            return if (minutes == 1L) "1 min ago" else "$minutes mins ago"
        }

        val timeCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val timeDay = timeCal.get(Calendar.DAY_OF_YEAR)
        val nowDay = nowCal.get(Calendar.DAY_OF_YEAR)
        val timeYear = timeCal.get(Calendar.YEAR)
        val nowYear = nowCal.get(Calendar.YEAR)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US).format(Date(timestamp))

        return if (timeYear == nowYear) {
            if (timeDay == nowDay) {
                "Today, $timeFormat"
            } else if (nowDay - timeDay == 1) {
                "Yesterday, $timeFormat"
            } else {
                SimpleDateFormat("MMM dd, hh:mm a", Locale.US).format(Date(timestamp))
            }
        } else {
            SimpleDateFormat("MMM dd yyyy, hh:mm a", Locale.US).format(Date(timestamp))
        }
    }
}
