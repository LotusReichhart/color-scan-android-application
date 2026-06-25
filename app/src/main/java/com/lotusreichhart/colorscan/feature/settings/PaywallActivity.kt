package com.lotusreichhart.colorscan.feature.settings

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lotusreichhart.colorscan.BuildConfig
import com.lotusreichhart.colorscan.core.data.BillingManager
import com.lotusreichhart.colorscan.databinding.ActivityPaywallBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class PaywallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaywallBinding
    private var selectedProductId = "premium_yearly"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaywallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSelectionUI()
        setupBillingObservers()
        setupListeners()
    }

    private fun setupSelectionUI() {
        updateSelectionUI()
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.cardMonthly.setOnClickListener {
            selectedProductId = "premium_monthly"
            updateSelectionUI()
        }

        binding.cardYearly.setOnClickListener {
            selectedProductId = "premium_yearly"
            updateSelectionUI()
        }

        binding.cardLifetime.setOnClickListener {
            selectedProductId = "premium_lifetime"
            updateSelectionUI()
        }

        binding.btnSubscribeAction.setOnClickListener {
            launchPurchaseFlow()
        }

        binding.btnRestore.setOnClickListener {
            Toast.makeText(this, "Restoring purchases...", Toast.LENGTH_SHORT).show()
            val billingManager = BillingManager.getInstance(this)
            billingManager.queryPurchases()
        }

        binding.btnTerms.setOnClickListener { v ->
            val popup = androidx.appcompat.widget.PopupMenu(v.context, v)
            popup.menu.add("Terms of Service")
            popup.menu.add("Privacy Policy")
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Terms of Service" -> openWebUrl(BuildConfig.TERMS_OF_SERVICE_URL)
                    "Privacy Policy" -> openWebUrl(BuildConfig.PRIVACY_POLICY_URL)
                }
                true
            }
            popup.show()
        }
    }

    private fun setupBillingObservers() {
        val billingManager = BillingManager.getInstance(this)

        lifecycleScope.launch {
            billingManager.isProUser.collect { isPro ->
                if (isPro) {
                    Toast.makeText(this@PaywallActivity, "Color Scan PRO Unlocked!", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            billingManager.productDetailsList.collectLatest { detailsList ->
                if (detailsList.isNotEmpty()) {
                    val monthlyProduct = detailsList.find { it.productId == "premium_monthly" }
                    val yearlyProduct = detailsList.find { it.productId == "premium_yearly" }
                    val lifetimeProduct = detailsList.find { it.productId == "premium_lifetime" }

                    monthlyProduct?.let { product ->
                        val offer = product.subscriptionOfferDetails?.firstOrNull()
                        val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                        if (price != null) {
                            binding.tvPriceMonthly.text = price
                        }
                    }

                    yearlyProduct?.let { product ->
                        val offer = product.subscriptionOfferDetails?.firstOrNull()
                        val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                        if (price != null) {
                            binding.tvPriceYearly.text = price
                        }
                    }

                    lifetimeProduct?.let { product ->
                        val price = product.oneTimePurchaseOfferDetails?.formattedPrice
                        if (price != null) {
                            binding.tvPriceLifetime.text = price
                        }
                    }
                }
            }
        }
    }

    private fun updateSelectionUI() {
        val selectedStrokeColor = Color.parseColor("#FFB300")
        val defaultStrokeColor = Color.parseColor("#2C2C2E")

        binding.cardMonthly.strokeColor = if (selectedProductId == "premium_monthly") selectedStrokeColor else defaultStrokeColor
        binding.cardMonthly.strokeWidth = if (selectedProductId == "premium_monthly") dpToPx(2) else dpToPx(1)

        binding.cardYearly.strokeColor = if (selectedProductId == "premium_yearly") selectedStrokeColor else defaultStrokeColor
        binding.cardYearly.strokeWidth = if (selectedProductId == "premium_yearly") dpToPx(2) else dpToPx(1)

        binding.cardLifetime.strokeColor = if (selectedProductId == "premium_lifetime") selectedStrokeColor else defaultStrokeColor
        binding.cardLifetime.strokeWidth = if (selectedProductId == "premium_lifetime") dpToPx(2) else dpToPx(1)

        binding.btnSubscribeAction.text = when (selectedProductId) {
            "premium_yearly" -> "Start 3-Day Free Trial"
            "premium_monthly" -> "Subscribe Now"
            "premium_lifetime" -> "Unlock Lifetime Pro"
            else -> "Subscribe Now"
        }
    }

    private fun launchPurchaseFlow() {
        val billingManager = BillingManager.getInstance(this)
        val detailsList = billingManager.productDetailsList.value
        val productDetails = detailsList.find { it.productId == selectedProductId }

        if (productDetails != null) {
            billingManager.launchBillingFlow(this, productDetails)
        } else {
            Toast.makeText(
                this,
                "Unable to connect to Google Play. Using offline mode or Play Store is not configured.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openWebUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e)
            Toast.makeText(this, "Cannot open link: $url", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
