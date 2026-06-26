package com.lotusreichhart.colorscan.feature.manual

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.lotusreichhart.colorscan.AdHelper
import com.lotusreichhart.colorscan.R
import com.lotusreichhart.colorscan.core.model.ColorItem
import com.lotusreichhart.colorscan.core.util.ColorConverter
import com.lotusreichhart.colorscan.databinding.FragmentManualBinding
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

class ManualFragment : Fragment() {

    companion object {
        fun newInstance() = ManualFragment()
    }

    private val viewModel: ManualViewModel by viewModels()
    private var _binding: FragmentManualBinding? = null
    private val binding get() = _binding!!
    private lateinit var similarColorsAdapter: SimilarColorsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeState()

        AdHelper.loadBannerAd(binding.adViewContainer)
        AdHelper.preloadInterstitialAd(requireContext())
    }

    private fun setupRecyclerView() {
        similarColorsAdapter = SimilarColorsAdapter(
            onColorClicked = { colorItem ->
                viewModel.onEvent(ManualUiEvent.SelectColor(colorItem))
                binding.nsvResults.smoothScrollTo(0, 0)
            },
            onCopyClicked = { anchorView, colorItem ->
                handleCopyAction(anchorView, colorItem)
            }
        )
        binding.rvSimilarColors.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = similarColorsAdapter
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                viewModel.onEvent(ManualUiEvent.SearchQueryChanged(query))
                if (query.isEmpty()) {
                    binding.ivSearchIcon.setImageResource(R.drawable.ic_search)
                } else {
                    binding.ivSearchIcon.setImageResource(R.drawable.ic_clear)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                viewModel.onEvent(
                    ManualUiEvent.SearchQueryChanged(
                        binding.etSearch.text.toString(),
                        immediate = true
                    )
                )
                hideKeyboard()
                true
            } else {
                false
            }
        }

        binding.ivSearchIcon.setOnClickListener {
            if (binding.etSearch.text.isNotEmpty()) {
                binding.etSearch.setText("")
            } else {
                viewModel.onEvent(
                    ManualUiEvent.SearchQueryChanged(
                        binding.etSearch.text.toString(),
                        immediate = true
                    )
                )
                hideKeyboard()
            }
        }

        binding.root.setOnClickListener {
            hideKeyboard()
        }

        binding.nsvResults.setOnTouchListener { _, _ ->
            hideKeyboard()
            false
        }

        binding.tvEmptyState.setOnClickListener {
            hideKeyboard()
        }

        binding.tvNoResultsState.setOnClickListener {
            hideKeyboard()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUiState(state)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUiState(state: ManualUiState) {

        when (state.status) {
            SearchStatus.EMPTY -> {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvNoResultsState.visibility = View.GONE
                binding.nsvResults.visibility = View.GONE
            }

            SearchStatus.NO_RESULTS -> {
                binding.tvEmptyState.visibility = View.GONE
                binding.tvNoResultsState.visibility = View.VISIBLE
                binding.nsvResults.visibility = View.GONE
            }

            SearchStatus.SUCCESS -> {
                binding.tvEmptyState.visibility = View.GONE
                binding.tvNoResultsState.visibility = View.GONE
                binding.nsvResults.visibility = View.VISIBLE

                val activeColor = state.activeColor
                if (activeColor != null) {
                    val colorInt = ColorConverter.parseHex(activeColor.hex) ?: Color.BLACK
                    val isLight = ColorConverter.isColorLight(colorInt)

                    val textColor = if (isLight) "#0A0A0A".toColorInt() else Color.WHITE
                    val secondaryTextColor =
                        if (isLight) "#333333".toColorInt() else "#D1D1D6".toColorInt()

                    binding.cardActiveColor.setCardBackgroundColor(colorInt)
                    binding.tvActiveColorName.text = activeColor.name
                    binding.tvActiveColorName.textColor = textColor
                    binding.tvActiveColorHex.text = "HEX: ${activeColor.hex}"
                    binding.tvActiveColorHex.textColor = secondaryTextColor
                    binding.tvActiveColorRgb.text = "RGB: ${activeColor.rgb}"
                    binding.tvActiveColorRgb.textColor = secondaryTextColor
                    binding.ivCopyActiveIcon.imageTintList =
                        android.content.res.ColorStateList.valueOf(textColor)

                    binding.btnCopyActiveColor.setOnClickListener {
                        handleCopyAction(it, activeColor)
                    }

                    binding.cardComplementSource.setCardBackgroundColor(colorInt)
                    binding.tvComplementSourceLabel.textColor = secondaryTextColor
                    binding.tvComplementSourceName.text = activeColor.name
                    binding.tvComplementSourceName.textColor = textColor
                    binding.tvComplementSourceHex.text = activeColor.hex
                    binding.tvComplementSourceHex.textColor = secondaryTextColor
                    binding.tvComplementSourceRgb.text = activeColor.rgb
                    binding.tvComplementSourceRgb.textColor = secondaryTextColor

                    val compColor = state.complementColor
                    if (compColor != null) {
                        val compColorInt = ColorConverter.parseHex(compColor.hex) ?: Color.BLACK
                        val compIsLight = ColorConverter.isColorLight(compColorInt)
                        val compTextColor = if (compIsLight) "#0A0A0A".toColorInt() else Color.WHITE
                        val compSecondaryTextColor =
                            if (compIsLight) "#333333".toColorInt() else "#D1D1D6".toColorInt()

                        binding.cardComplementTarget.setCardBackgroundColor(compColorInt)
                        binding.tvComplementTargetLabel.textColor = compSecondaryTextColor
                        binding.tvComplementTargetName.text = compColor.name
                        binding.tvComplementTargetName.textColor = compTextColor
                        binding.tvComplementTargetHex.text = compColor.hex
                        binding.tvComplementTargetHex.textColor = compSecondaryTextColor
                        binding.tvComplementTargetRgb.text = compColor.rgb
                        binding.tvComplementTargetRgb.textColor = compSecondaryTextColor

                        binding.cardComplementTarget.setOnClickListener {
                            viewModel.onEvent(ManualUiEvent.SelectColor(compColor))
                            binding.nsvResults.smoothScrollTo(0, 0)
                        }
                    }

                    binding.cardTriadic1.setCardBackgroundColor(colorInt)
                    binding.tvTriadic1Hex.text = activeColor.hex
                    binding.tvTriadic1Hex.textColor = textColor
                    binding.tvTriadic1Rgb.text = activeColor.rgb
                    binding.tvTriadic1Rgb.textColor = secondaryTextColor

                    val t1 = state.triadic1
                    if (t1 != null) {
                        val t1ColorInt = ColorConverter.parseHex(t1.hex) ?: Color.BLACK
                        val t1IsLight = ColorConverter.isColorLight(t1ColorInt)
                        binding.cardTriadic2.setCardBackgroundColor(t1ColorInt)
                        binding.tvTriadic2Hex.text = t1.hex
                        binding.tvTriadic2Hex.textColor =
                            if (t1IsLight) "#0A0A0A".toColorInt() else Color.WHITE
                        binding.tvTriadic2Rgb.text = t1.rgb
                        binding.tvTriadic2Rgb.textColor =
                            if (t1IsLight) "#333333".toColorInt() else "#D1D1D6".toColorInt()

                        binding.cardTriadic2.setOnClickListener {
                            viewModel.onEvent(ManualUiEvent.SelectColor(t1))
                            binding.nsvResults.smoothScrollTo(0, 0)
                        }
                    }

                    val t2 = state.triadic2
                    if (t2 != null) {
                        val t2ColorInt = ColorConverter.parseHex(t2.hex) ?: Color.BLACK
                        val t2IsLight = ColorConverter.isColorLight(t2ColorInt)
                        binding.cardTriadic3.setCardBackgroundColor(t2ColorInt)
                        binding.tvTriadic3Hex.text = t2.hex
                        binding.tvTriadic3Hex.textColor =
                            if (t2IsLight) "#0A0A0A".toColorInt() else Color.WHITE
                        binding.tvTriadic3Rgb.text = t2.rgb
                        binding.tvTriadic3Rgb.textColor =
                            if (t2IsLight) "#333333".toColorInt() else "#D1D1D6".toColorInt()

                        binding.cardTriadic3.setOnClickListener {
                            viewModel.onEvent(ManualUiEvent.SelectColor(t2))
                            binding.nsvResults.smoothScrollTo(0, 0)
                        }
                    }

                    similarColorsAdapter.submitList(state.searchResults)
                    val suggestionsAvailable = state.searchResults.isNotEmpty()
                    binding.tvSimilarColorsTitle.visibility =
                        if (suggestionsAvailable) View.VISIBLE else View.GONE
                    binding.rvSimilarColors.visibility =
                        if (suggestionsAvailable) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun handleCopyAction(view: View, colorItem: ColorItem) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add(0, 1, 0, "Copy Name")
        popup.menu.add(0, 2, 0, "Copy HEX")
        popup.menu.add(0, 3, 0, "Copy RGB")
        popup.setOnMenuItemClickListener { item ->
            val copied = when (item.itemId) {
                1 -> {
                    copyToClipboard("Name", colorItem.name)
                    true
                }
                2 -> {
                    copyToClipboard("HEX", colorItem.hex)
                    true
                }
                3 -> {
                    copyToClipboard("RGB", colorItem.rgb)
                    true
                }
                else -> false
            }
            if (copied) {
                viewModel.onEvent(ManualUiEvent.SaveColorToHistory(colorItem))
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

    private fun hideKeyboard() {
        binding.etSearch.clearFocus()
        val view = activity?.currentFocus ?: binding.etSearch
        val imm =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private var android.widget.TextView.textColor: Int
        get() = currentTextColor
        set(value) = setTextColor(value)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}