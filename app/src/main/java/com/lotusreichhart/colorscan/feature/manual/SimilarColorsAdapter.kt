package com.lotusreichhart.colorscan.feature.manual

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lotusreichhart.colorscan.core.model.ColorItem
import com.lotusreichhart.colorscan.core.util.ColorConverter
import com.lotusreichhart.colorscan.databinding.ItemSimilarColorBinding

class SimilarColorsAdapter(
    private val onColorClicked: (ColorItem) -> Unit,
    private val onCopyClicked: (View, ColorItem) -> Unit
) : RecyclerView.Adapter<SimilarColorsAdapter.ViewHolder>() {

    private var items: List<ColorItem> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<ColorItem>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSimilarColorBinding.inflate(
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

    inner class ViewHolder(private val binding: ItemSimilarColorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(colorItem: ColorItem) {
            binding.tvColorName.text = colorItem.name
            binding.tvColorDetails.text = "HEX: ${colorItem.hex}  |  RGB: ${colorItem.rgb}"

            val colorInt = ColorConverter.parseHex(colorItem.hex) ?: Color.BLACK
            val previewDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colorInt)
            }
            binding.colorPreview.background = previewDrawable

            binding.root.setOnClickListener {
                onColorClicked(colorItem)
            }

            binding.btnCopy.setOnClickListener { view ->
                onCopyClicked(view, colorItem)
            }
        }
    }
}
