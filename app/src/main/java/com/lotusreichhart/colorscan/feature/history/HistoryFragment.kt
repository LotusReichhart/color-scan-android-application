package com.lotusreichhart.colorscan.feature.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.lotusreichhart.colorscan.AdHelper
import com.lotusreichhart.colorscan.R
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

    private var currentTabPosition = 0
    private var colorOnlyCount = 0
    private var photoColorCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        setupListeners()
        observeSortState()

        AdHelper.loadBannerAd(binding.adViewContainer)
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = HistoryPagerAdapter(this)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Colors"
                1 -> "Photos"
                else -> ""
            }
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentTabPosition = position
                updateCountText()
            }
        })

        // Observe sizes to dynamically update tvSavedCount
        viewModel.colorOnlyHistory.observe(viewLifecycleOwner) { list ->
            colorOnlyCount = list?.size ?: 0
            if (currentTabPosition == 0) {
                updateCountText()
            }
        }

        viewModel.photoColorHistory.observe(viewLifecycleOwner) { list ->
            photoColorCount = list?.size ?: 0
            if (currentTabPosition == 1) {
                updateCountText()
            }
        }
    }

    private fun updateCountText() {
        val count = if (currentTabPosition == 0) colorOnlyCount else photoColorCount
        val text = if (currentTabPosition == 0) {
            if (count == 1) "1 COLOR SAVED" else "$count COLORS SAVED"
        } else {
            if (count == 1) "1 PHOTO SAVED" else "$count PHOTOS SAVED"
        }
        binding.tvSavedCount.text = text
    }

    private fun setupListeners() {
        binding.btnSort.setOnClickListener {
            viewModel.toggleSort()
        }
    }

    private fun observeSortState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sortOption.collect { sortOption ->
                    if (sortOption == HistorySort.DESC) {
                        binding.btnSort.setImageResource(R.drawable.ic_calendar_arrow_down)
                    } else {
                        binding.btnSort.setImageResource(R.drawable.ic_calendar_arrow_up)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class HistoryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ColorOnlyHistoryFragment.newInstance()
                1 -> PhotoColorHistoryFragment.newInstance()
                else -> throw IllegalStateException("Invalid position $position")
            }
        }
    }
}