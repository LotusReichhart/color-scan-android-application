package com.lotusreichhart.colorscan.feature.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import com.lotusreichhart.colorscan.BuildConfig
import com.lotusreichhart.colorscan.core.data.BillingManager
import com.lotusreichhart.colorscan.databinding.ActivityPaywallBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class PaywallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaywallBinding
    private val viewModel: PaywallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val billingManager = BillingManager.getInstance(this)
        if (billingManager.activeProductId.value == "premium_lifetime") {
            finish()
            return
        }

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
            viewModel.selectProduct("premium_monthly")
        }

        binding.cardYearly.setOnClickListener {
            viewModel.selectProduct("premium_yearly")
        }

        binding.cardLifetime.setOnClickListener {
            viewModel.selectProduct("premium_lifetime")
        }

        binding.btnSubscribeAction.setOnClickListener {
            viewModel.launchPurchaseFlow()
        }

        binding.btnRestore.setOnClickListener {
            Toast.makeText(this, "Restoring purchases...", Toast.LENGTH_SHORT).show()
            viewModel.queryPurchases()
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
        lifecycleScope.launch {
            viewModel.activeProductId.collect { activeId ->
                if (activeId == "premium_lifetime") {
                    finish()
                    return@collect
                }
                updateSelectionUI()
                updateProductPricesUI(viewModel.productDetailsList.value, activeId)
            }
        }

        lifecycleScope.launch {
            viewModel.selectedProductId.collect {
                updateSelectionUI()
            }
        }

        lifecycleScope.launch {
            viewModel.productDetailsList.collectLatest { detailsList ->
                updateProductPricesUI(detailsList, viewModel.activeProductId.value)
            }
        }

        lifecycleScope.launch {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is PaywallUiEvent.LaunchBillingFlow -> {
                        val billingManager = BillingManager.getInstance(this@PaywallActivity)
                        billingManager.launchBillingFlow(this@PaywallActivity, event.productDetails)
                    }
                    is PaywallUiEvent.PurchaseSuccess -> {
                        Toast.makeText(this@PaywallActivity, "Color Scan PRO Unlocked!", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    is PaywallUiEvent.ShowError -> {
                        Toast.makeText(this@PaywallActivity, event.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateProductPricesUI(detailsList: List<ProductDetails>, activeId: String?) {
        if (detailsList.isNotEmpty()) {
            val monthlyProduct = detailsList.find { it.productId == "premium_monthly" }
            val yearlyProduct = detailsList.find { it.productId == "premium_yearly" }
            val lifetimeProduct = detailsList.find { it.productId == "premium_lifetime" }

            monthlyProduct?.let { product ->
                val offer = product.subscriptionOfferDetails?.firstOrNull()
                val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                if (price != null) {
                    binding.tvPriceMonthly.text = if (activeId == "premium_monthly") "$price (Active)" else price
                }
            }

            yearlyProduct?.let { product ->
                val offer = product.subscriptionOfferDetails?.firstOrNull()
                val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                if (price != null) {
                    binding.tvPriceYearly.text = if (activeId == "premium_yearly") "$price (Active)" else price
                }
            }

            lifetimeProduct?.let { product ->
                val price = product.oneTimePurchaseOfferDetails?.formattedPrice
                if (price != null) {
                    binding.tvPriceLifetime.text = if (activeId == "premium_lifetime") "$price (Active)" else price
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateSelectionUI() {
        val selectedId = viewModel.selectedProductId.value
        val activeId = viewModel.activeProductId.value

        val selectedStrokeColor = "#FFB300".toColorInt()
        val defaultStrokeColor = "#2C2C2E".toColorInt()

        binding.cardMonthly.strokeColor = if (selectedId == "premium_monthly") selectedStrokeColor else defaultStrokeColor
        binding.cardMonthly.strokeWidth = if (selectedId == "premium_monthly") dpToPx(2) else dpToPx(1)

        binding.cardYearly.strokeColor = if (selectedId == "premium_yearly") selectedStrokeColor else defaultStrokeColor
        binding.cardYearly.strokeWidth = if (selectedId == "premium_yearly") dpToPx(2) else dpToPx(1)

        binding.cardLifetime.strokeColor = if (selectedId == "premium_lifetime") selectedStrokeColor else defaultStrokeColor
        binding.cardLifetime.strokeWidth = if (selectedId == "premium_lifetime") dpToPx(2) else dpToPx(1)

        if (selectedId == activeId) {
            binding.btnSubscribeAction.text = "Current Plan"
            binding.btnSubscribeAction.isEnabled = false
            binding.btnSubscribeAction.alpha = 0.5f
        } else {
            binding.btnSubscribeAction.isEnabled = true
            binding.btnSubscribeAction.alpha = 1.0f
            binding.btnSubscribeAction.text = when (selectedId) {
                "premium_yearly" -> "Start 3-Day Free Trial"
                "premium_monthly" -> "Subscribe Now"
                "premium_lifetime" -> "Unlock Lifetime Pro"
                else -> "Subscribe Now"
            }
        }
    }

    private fun openWebUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
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
