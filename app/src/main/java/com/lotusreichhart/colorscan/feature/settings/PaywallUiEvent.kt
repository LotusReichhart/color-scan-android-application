package com.lotusreichhart.colorscan.feature.settings

import com.android.billingclient.api.ProductDetails

sealed class PaywallUiEvent {
    data class LaunchBillingFlow(val productDetails: ProductDetails) : PaywallUiEvent()
    data class PurchaseSuccess(val productId: String) : PaywallUiEvent()
    data class ShowError(val message: String) : PaywallUiEvent()
}