package com.lotusreichhart.colorscan.feature.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.lotusreichhart.colorscan.AdHelper
import com.lotusreichhart.colorscan.R
import com.lotusreichhart.colorscan.core.model.HistoryEntity
import com.lotusreichhart.colorscan.core.model.HistorySort
import com.lotusreichhart.colorscan.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    companion object {
        fun newInstance() = HistoryFragment()
    }

    private val viewModel: HistoryViewModel by viewModels()
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeState()

        AdHelper.loadBannerAd(binding.adViewContainer)
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onCopyClicked = { anchorView, historyItem ->
                handleCopyAction(anchorView, historyItem)
            },
            onDeleteClicked = { historyItem ->
                showDeleteConfirmationDialog(historyItem)
            }
        )
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun setupListeners() {
        binding.btnSort.setOnClickListener {
            viewModel.onEvent(HistoryUiEvent.ToggleSort)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvSavedCount.text = state.countText

                    // Update sort button icon based on preference
                    if (state.sortOption == HistorySort.DESC) {
                        binding.btnSort.setImageResource(R.drawable.ic_calendar_arrow_down)
                    } else {
                        binding.btnSort.setImageResource(R.drawable.ic_calendar_arrow_up)
                    }

                    // Update empty state visibility
                    if (state.items.isEmpty()) {
                        binding.rvHistory.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                    } else {
                        binding.rvHistory.visibility = View.VISIBLE
                        binding.layoutEmpty.visibility = View.GONE
                        historyAdapter.submitList(state.items)
                    }
                }
            }
        }
    }

    private fun handleCopyAction(view: View, historyItem: HistoryEntity) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add(0, 1, 0, "Copy Name")
        popup.menu.add(0, 2, 0, "Copy HEX")
        popup.menu.add(0, 3, 0, "Copy RGB")
        popup.setOnMenuItemClickListener { item ->
            val copied = when (item.itemId) {
                1 -> {
                    copyToClipboard("Name", historyItem.name)
                    true
                }
                2 -> {
                    copyToClipboard("HEX", historyItem.hex)
                    true
                }
                3 -> {
                    copyToClipboard("RGB", historyItem.rgb)
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

    private fun showDeleteConfirmationDialog(historyItem: HistoryEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Color")
            .setMessage("Are you sure you want to delete this color from history?")
            .setPositiveButton("Delete") { dialog, _ ->
                viewModel.onEvent(HistoryUiEvent.DeleteColor(historyItem))
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