package com.lotusreichhart.colorscan.feature.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.lotusreichhart.colorscan.R
import com.lotusreichhart.colorscan.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    var onNextClickListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val pageIndex = args.getInt(ARG_PAGE_INDEX)
        val imageRes = args.getInt(ARG_IMAGE)
        val titleRes = args.getInt(ARG_TITLE)
        val descRes = args.getInt(ARG_DESC)
        val nextRes = args.getInt(ARG_NEXT)

        binding.imgOnboarding.setImageResource(imageRes)
        binding.tvOnboardingTitle.setText(titleRes)
        binding.tvOnboardingDesc.setText(descRes)
        binding.btnNext.setText(nextRes)

        updateIndicators(pageIndex)

        binding.btnNext.setOnClickListener {
            onNextClickListener?.invoke()
        }
    }

    private fun updateIndicators(pageIndex: Int) {
        val activeWidth = dpToPx(24)
        val inactiveWidth = dpToPx(8)

        val indicators = listOf(binding.indicator1, binding.indicator2, binding.indicator3)

        for (i in indicators.indices) {
            val indicator = indicators[i]
            val params = indicator.layoutParams as ViewGroup.MarginLayoutParams
            if (i == pageIndex) {
                params.width = activeWidth
                indicator.setBackgroundResource(R.drawable.indicator_active)
            } else {
                params.width = inactiveWidth
                indicator.setBackgroundResource(R.drawable.indicator_inactive)
            }
            indicator.layoutParams = params
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PAGE_INDEX = "arg_page_index"
        private const val ARG_IMAGE = "arg_image"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_DESC = "arg_desc"
        private const val ARG_NEXT = "arg_next"

        fun newInstance(
            pageIndex: Int,
            @DrawableRes imageRes: Int,
            @StringRes titleRes: Int,
            @StringRes descRes: Int,
            @StringRes nextRes: Int
        ): OnboardingFragment {
            return OnboardingFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PAGE_INDEX, pageIndex)
                    putInt(ARG_IMAGE, imageRes)
                    putInt(ARG_TITLE, titleRes)
                    putInt(ARG_DESC, descRes)
                    putInt(ARG_NEXT, nextRes)
                }
            }
        }
    }
}