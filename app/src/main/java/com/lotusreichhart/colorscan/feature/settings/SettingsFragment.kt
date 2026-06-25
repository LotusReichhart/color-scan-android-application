package com.lotusreichhart.colorscan.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.lotusreichhart.colorscan.AdHelper
import com.lotusreichhart.colorscan.BuildConfig
import com.lotusreichhart.colorscan.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    private val viewModel: SettingsViewModel by viewModels()
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set version dynamically from BuildConfig
        binding.tvVersion.text = "Version ${BuildConfig.VERSION_NAME}"

        setupListeners()
        observeBillingStatus()
    }

    private fun observeBillingStatus() {
        val billingManager = com.lotusreichhart.colorscan.core.data.BillingManager.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            billingManager.isProUser.collect { isPro ->
                if (isPro) {
                    binding.tvRemoveAds.text = "PRO Version Unlocked"
                    binding.btnRemoveAds.setOnClickListener {
                        Toast.makeText(
                            requireContext(),
                            "You have already unlocked Color Scan PRO!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    binding.adViewContainer.visibility = View.GONE
                } else {
                    binding.tvRemoveAds.text = "Remove Ads"
                    binding.btnRemoveAds.setOnClickListener {
                        val intent = Intent(requireContext(), PaywallActivity::class.java)
                        startActivity(intent)
                    }
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
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
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