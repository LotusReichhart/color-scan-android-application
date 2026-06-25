package com.lotusreichhart.colorscan.feature.history

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.lotusreichhart.colorscan.core.model.HistoryEntity
import com.lotusreichhart.colorscan.databinding.ItemHistoryColorBinding
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onCopyClicked: (View, HistoryEntity) -> Unit,
    private val onDeleteClicked: (HistoryEntity) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var items: List<HistoryEntity> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<HistoryEntity>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryColorBinding.inflate(
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

    inner class ViewHolder(private val binding: ItemHistoryColorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(historyItem: HistoryEntity) {
            binding.tvColorName.text = historyItem.name
            binding.tvColorHex.text = "HEX: ${historyItem.hex}"
            binding.tvColorRgb.text = "RGB: ${historyItem.rgb}"

            binding.tvTimestamp.text = formatHistoryTimestamp(historyItem.timestamp)

            val colorInt = try {
                historyItem.hex.toColorInt()
            } catch (e: Exception) {
                Timber.e(e)
                Color.BLACK
            }

            val density = binding.root.context.resources.displayMetrics.density
            val cornerRadiusPx = 12f * density

            val glowColor = (colorInt and 0x00FFFFFF) or 0x33000000

            val glowDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerRadiusPx
                setColor(glowColor)
            }

            val solidDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerRadiusPx
                setColor(colorInt)
            }

            val layerDrawable = LayerDrawable(arrayOf(glowDrawable, solidDrawable)).apply {
                val paddingPx = (6 * density).toInt()
                setLayerInset(1, paddingPx, paddingPx, paddingPx, paddingPx)
            }

            binding.colorPreview.background = layerDrawable

            binding.btnCopy.setOnClickListener { view ->
                onCopyClicked(view, historyItem)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClicked(historyItem)
            }
        }
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
