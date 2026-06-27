package com.lotusreichhart.colorscan.feature.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.lotusreichhart.colorscan.AdHelper
import com.lotusreichhart.colorscan.core.model.PhotoHistoryEntity
import com.lotusreichhart.colorscan.databinding.FragmentHistoryTabBinding

class PhotoColorHistoryFragment : Fragment() {

    companion object {
        fun newInstance() = PhotoColorHistoryFragment()
    }

    // Shared ViewModel scoped to parent Fragment (HistoryFragment)
    private val viewModel: HistoryViewModel by viewModels({ requireParentFragment() })
    private var _binding: FragmentHistoryTabBinding? = null
    private val binding get() = _binding!!
    private lateinit var photoAdapter: PhotoColorAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEmptyText.text = "NO SAVED PHOTOS"
        setupRecyclerView()
        observeState()
    }

    private fun setupRecyclerView() {
        photoAdapter = PhotoColorAdapter(
            onItemClicked = { photoItem ->
                openPhotoDetail(photoItem)
            },
            onCopyClicked = { anchorView, photoItem ->
                handleCopyAction(anchorView, photoItem)
            },
            onDeleteClicked = { photoItem ->
                showDeleteConfirmationDialog(photoItem)
            }
        )
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = photoAdapter
        }
    }

    private fun observeState() {
        viewModel.photoColorHistory.observe(viewLifecycleOwner) { items ->
            if (items.isNullOrEmpty()) {
                binding.rvHistory.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
            } else {
                binding.rvHistory.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
                photoAdapter.submitList(items)
            }
        }
    }

    private fun openPhotoDetail(photoItem: PhotoHistoryEntity) {
        val intent = Intent(requireContext(), PhotoDetailActivity::class.java).apply {
            putExtra(PhotoDetailActivity.EXTRA_IMAGE_PATH, photoItem.imagePath)
            putExtra(PhotoDetailActivity.EXTRA_HEX, photoItem.hex)
            putExtra(PhotoDetailActivity.EXTRA_NAME, photoItem.name)
            putExtra(PhotoDetailActivity.EXTRA_RGB, photoItem.rgb)
            putExtra(PhotoDetailActivity.EXTRA_TIMESTAMP, photoItem.timestamp)
        }
        startActivity(intent)
    }

    private fun handleCopyAction(view: View, photoItem: PhotoHistoryEntity) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add(0, 1, 0, "Copy Name")
        popup.menu.add(0, 2, 0, "Copy HEX")
        popup.menu.add(0, 3, 0, "Copy RGB")
        popup.setOnMenuItemClickListener { item ->
            val copied = when (item.itemId) {
                1 -> {
                    copyToClipboard("Name", photoItem.name)
                    true
                }
                2 -> {
                    copyToClipboard("HEX", photoItem.hex)
                    true
                }
                3 -> {
                    copyToClipboard("RGB", photoItem.rgb)
                    true
                }
                else -> false
            }
            if (copied) {
                AdHelper.showInterstitialAd(requireActivity()) {}
            }
            true
        }
        popup.show()
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "$label copied: $text", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmationDialog(photoItem: PhotoHistoryEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Photo")
            .setMessage("Are you sure you want to delete this photo and color from history?")
            .setPositiveButton("Delete") { dialog, _ ->
                viewModel.deletePhotoColor(photoItem)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
