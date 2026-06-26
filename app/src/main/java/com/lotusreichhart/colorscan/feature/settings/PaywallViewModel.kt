package com.lotusreichhart.colorscan.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.lotusreichhart.colorscan.core.data.BillingManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PaywallViewModel(application: Application) : AndroidViewModel(application) {

    private val billingManager = BillingManager.getInstance(application)

    private val _selectedProductId = MutableStateFlow("premium_yearly")
    val selectedProductId: StateFlow<String> = _selectedProductId.asStateFlow()

    val activeProductId: StateFlow<String?> = billingManager.activeProductId
    val productDetailsList: StateFlow<List<ProductDetails>> = billingManager.productDetailsList
    val isProUser: StateFlow<Boolean> = billingManager.isProUser

    private val _uiEvent = MutableSharedFlow<PaywallUiEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            billingManager.purchaseSuccessEvent.collect { productId ->
                _uiEvent.emit(PaywallUiEvent.PurchaseSuccess(productId))
            }
        }
    }

    fun selectProduct(productId: String) {
        _selectedProductId.value = productId
    }

    fun launchPurchaseFlow() {
        val details = productDetailsList.value.find { it.productId == _selectedProductId.value }
        if (details != null) {
            viewModelScope.launch {
                _uiEvent.emit(PaywallUiEvent.LaunchBillingFlow(details))
            }
        } else {
            viewModelScope.launch {
                _uiEvent.emit(
                    PaywallUiEvent.ShowError(
                        "Unable to connect to Google Play. Using offline mode or Play Store is not configured."
                    )
                )
            }
        }
    }

    fun queryPurchases() {
        billingManager.queryPurchases()
    }
}
