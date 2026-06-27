package com.lotusreichhart.colorscan.core.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import timber.log.Timber

class BillingManager private constructor(context: Context) : PurchasesUpdatedListener {

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private val _isProUser = MutableStateFlow(false)
    val isProUser = _isProUser.asStateFlow()

    private val _activeProductId = MutableStateFlow<String?>(null)
    val activeProductId = _activeProductId.asStateFlow()

    private val _activeSubscriptionToken = MutableStateFlow<String?>(null)
    val activeSubscriptionToken = _activeSubscriptionToken.asStateFlow()

    private val _productDetailsList = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetailsList = _productDetailsList.asStateFlow()

    private val _purchaseSuccessEvent = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val purchaseSuccessEvent = _purchaseSuccessEvent.asSharedFlow()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Timber.d("Billing Setup Finished: responseCode=${billingResult.responseCode}, debugMessage=${billingResult.debugMessage}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("Billing Setup Finished Successfully")
                    queryPurchases()
                    queryProductDetails()
                } else {
                    Timber.e("Billing Setup Failed: responseCode=${billingResult.responseCode}, message=${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Timber.w("Billing Service Disconnected. Reconnecting...")
                startConnection()
            }
        })
    }

    fun queryPurchases() {
        if (!billingClient.isReady) return

        val purchasedProductIds = mutableSetOf<String>()
        val purchaseTokenMap = mutableMapOf<String, String>()
        var isPro = false

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            Timber.d("Query In-App Purchases: responseCode=${result.responseCode}, size=${purchases.size}")
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        isPro = true
                        val prodId = purchase.products.firstOrNull()
                        if (prodId != null) {
                            purchasedProductIds.add(prodId)
                            purchaseTokenMap[prodId] = purchase.purchaseToken
                        }
                        processAcknowledge(purchase)
                    }
                }
            }

            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ) { subResult, subPurchases ->
                Timber.d("Query Subscriptions Purchases: responseCode=${subResult.responseCode}, size=${subPurchases.size}")
                if (subResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    for (purchase in subPurchases) {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            isPro = true
                            val prodId = purchase.products.firstOrNull()
                            if (prodId != null) {
                                purchasedProductIds.add(prodId)
                                purchaseTokenMap[prodId] = purchase.purchaseToken
                            }
                            processAcknowledge(purchase)
                        }
                    }
                }

                // Determine activeProductId by priority:
                // 1. premium_lifetime
                // 2. premium_yearly
                // 3. premium_monthly
                val activeId = when {
                    purchasedProductIds.contains("premium_lifetime") -> "premium_lifetime"
                    purchasedProductIds.contains("premium_yearly") -> "premium_yearly"
                    purchasedProductIds.contains("premium_monthly") -> "premium_monthly"
                    else -> purchasedProductIds.firstOrNull()
                }

                val activeSubToken =
                    if (activeId == "premium_monthly" || activeId == "premium_yearly") {
                        purchaseTokenMap[activeId]
                    } else {
                        null
                    }

                _isProUser.value = isPro
                _activeProductId.value = activeId
                _activeSubscriptionToken.value = activeSubToken
            }
        }
    }

    private fun queryProductDetails() {
        val subsProducts = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_monthly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_yearly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val inappProducts = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_lifetime")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val tempCombinedList =
            java.util.Collections.synchronizedList(mutableListOf<ProductDetails>())

        // 1. Query SUBS Products
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(subsProducts)
            .build()

        billingClient.queryProductDetailsAsync(subsParams, object : ProductDetailsResponseListener {
            override fun onProductDetailsResponse(
                billingResult: BillingResult,
                result: QueryProductDetailsResult
            ) {
                val productDetailsList = result.productDetailsList ?: emptyList()
                Timber.d("Query SUBS Callback: responseCode=${billingResult.responseCode}, debugMessage=${billingResult.debugMessage}, count=${productDetailsList.size}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    tempCombinedList.addAll(productDetailsList)
                    _productDetailsList.value = tempCombinedList.toList()
                } else {
                    Timber.e("Query SUBS Failed: responseCode=${billingResult.responseCode}, message=${billingResult.debugMessage}")
                }

                // 2. Query INAPP Products
                val inappParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(inappProducts)
                    .build()

                billingClient.queryProductDetailsAsync(
                    inappParams,
                    object : ProductDetailsResponseListener {
                        override fun onProductDetailsResponse(
                            inappResult: BillingResult,
                            inappResultObj: QueryProductDetailsResult
                        ) {
                            val inappDetailsList = inappResultObj.productDetailsList ?: emptyList()
                            Timber.d("Query INAPP Callback: responseCode=${inappResult.responseCode}, debugMessage=${inappResult.debugMessage}, count=${inappDetailsList.size}")
                            if (inappResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                tempCombinedList.addAll(inappDetailsList)
                                _productDetailsList.value = tempCombinedList.toList()
                            } else {
                                Timber.e("Query INAPP Failed: responseCode=${inappResult.responseCode}, message=${inappResult.debugMessage}")
                            }
                        }
                    })
            }
        })
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply {
                    productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let {
                        setOfferToken(it)
                    }
                }
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .apply {
                if (productDetails.productType == BillingClient.ProductType.SUBS) {
                    val oldToken = _activeSubscriptionToken.value
                    if (oldToken != null) {
                        setSubscriptionUpdateParams(
                            BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                                .setOldPurchaseToken(oldToken)
                                .setSubscriptionReplacementMode(
                                    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
                                )
                                .build()
                        )
                    }
                }
            }
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            var newlyPurchasedId: String? = null
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    newlyPurchasedId = purchase.products.firstOrNull()
                    processAcknowledge(purchase)
                }
            }
            queryPurchases()
            if (newlyPurchasedId != null) {
                _purchaseSuccessEvent.tryEmit(newlyPurchasedId)
            }
        } else {
            Timber.e("Purchases Updated Callback Error: responseCode=${billingResult.responseCode}, message=${billingResult.debugMessage}")
        }
    }

    private fun processAcknowledge(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("Purchase acknowledged successfully")
                    queryPurchases()
                } else {
                    Timber.e("Acknowledge Purchase Failed: responseCode=${billingResult.responseCode}")
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
