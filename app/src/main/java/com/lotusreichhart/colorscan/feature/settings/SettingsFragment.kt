package com.lotusreichhart.colorscan.feature.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lotusreichhart.colorscan.AdHelper
import com.lotusreichhart.colorscan.BuildConfig
import com.lotusreichhart.colorscan.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import timber.log.Timber

class SettingsFragment : Fragment() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set version dynamically from BuildConfig
        binding.tvVersion.text = "Version ${BuildConfig.VERSION_NAME}"

        setupListeners()
        observeBillingStatus()
    }

    @SuppressLint("SetTextI18n")
    private fun observeBillingStatus() {
        val billingManager = com.lotusreichhart.colorscan.core.data.BillingManager.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            billingManager.activeProductId.collect { productId ->
                if (productId == "premium_lifetime") {
                    binding.btnRemoveAds.setOnClickListener(null)
                    binding.btnRemoveAds.isClickable = false
                    binding.btnRemoveAds.isFocusable = false
                } else {
                    binding.btnRemoveAds.isClickable = true
                    binding.btnRemoveAds.isFocusable = true
                    binding.btnRemoveAds.setOnClickListener {
                        val intent = Intent(requireContext(), PaywallActivity::class.java)
                        startActivity(intent)
                    }
                }

                if (productId != null) {
                    val tierName = when (productId) {
                        "premium_lifetime" -> "PRO Lifetime"
                        "premium_yearly" -> "PRO Yearly"
                        "premium_monthly" -> "PRO Monthly"
                        else -> "PRO Active"
                    }
                    binding.tvRemoveAds.text = "$tierName - Active"
                    binding.adViewContainer.visibility = View.GONE
                } else {
                    binding.tvRemoveAds.text = "Remove Ads"
                    binding.adViewContainer.visibility = View.VISIBLE
                    // Load AdMob Banner Ad
                    AdHelper.loadBannerAd(binding.adViewContainer)
                }
            }
        }
    }

    private fun setupListeners() {

        binding.btnPrivacyPolicy.setOnClickListener {
            openWebUrl(BuildConfig.PRIVACY_POLICY_URL)
        }

        binding.btnTermsOfService.setOnClickListener {
            openWebUrl(BuildConfig.TERMS_OF_SERVICE_URL)
        }
    }

    private fun openWebUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e)
            Toast.makeText(
                requireContext(),
                "Cannot open link: $url",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}