package com.revenuecat.paywallSample

import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams

class MainActivity : AppCompatActivity(), PurchasesUpdatedListener, BillingClientStateListener {

    private val productIds = listOf("bc6.premium", "bc6.standard")
    private var products = mutableListOf<ProductDetails>()
    private var paywallItems = mutableStateListOf<PaywallItem>()
    private var specialOfferProduct: ProductDetails? = null
    private var specialOffer: ProductDetails.SubscriptionOfferDetails? = null
    private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        billingClient =
            BillingClient
                .newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build()
        billingClient.startConnection(this)

        setContent {
            PaywallScreen(paywallItems = paywallItems,
                onPurchasePaywallItem = { product, paywallItem ->
                    subscribe(product, paywallItem.freeTrial!!)
                },
                products = products,
                onPurchaseSpecialOffer = {
                    if (specialOfferProduct != null && specialOffer != null) {
                        subscribe(specialOfferProduct!!, specialOffer!!)
                    }
                })
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            loadProducts()
        }
    }

    override fun onBillingServiceDisconnected() {
        billingClient.startConnection(this)
    }

    override fun onDestroy() {
        billingClient.endConnection()
        super.onDestroy()
    }

    private fun loadProducts() {
        val productDetailsParamsBuilder = QueryProductDetailsParams.newBuilder()
        val productType = BillingClient.ProductType.SUBS

        val productIdsToQuery = productIds.map {
            QueryProductDetailsParams.Product.newBuilder().setProductId(it)
                .setProductType(productType).build()
        }

        productDetailsParamsBuilder.setProductList(productIdsToQuery)
        billingClient.queryProductDetailsAsync(productDetailsParamsBuilder.build()) { billingResult, productDetails ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetails.isNotEmpty()) {
                runOnUiThread {
                    val productSubscriptionOffersByBasePlanId =
                        mutableMapOf<String, MutableMap<String, MutableList<ProductDetails.SubscriptionOfferDetails>>>()
                    productDetails.forEach { details ->
                        details.subscriptionOfferDetails?.forEach { offer ->
                            if (offer.offerTags.contains("specialoffer")) {
                                specialOffer = offer
                                specialOfferProduct = details
                            }
                            val basePlanOffers =
                                productSubscriptionOffersByBasePlanId.getOrPut(details.productId) { mutableMapOf() }
                            val offersForPlan =
                                basePlanOffers.getOrPut(offer.basePlanId) { mutableListOf() }
                            offersForPlan.add(offer)
                        }
                    }
                    products.clear()
                    products.addAll(productDetails)
                    paywallItems.clear()
                    paywallItems.addAll(productSubscriptionOffersByBasePlanId.flatMap { (productId, offersByPlan) ->
                        offersByPlan.map { PaywallItem(productId, it.value) }
                    })
                }
            }
        }
    }

    private fun subscribe(
        productDetails: ProductDetails,
        subscriptionOfferDetails: ProductDetails.SubscriptionOfferDetails
    ) {
        if (billingClient.isReady) {
            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(subscriptionOfferDetails.offerToken).build()

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams)).build()
            billingClient.launchBillingFlow(this, flowParams)
        } else {
            Toast.makeText(this, "Error: BillingClient not ready", LENGTH_SHORT).show()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken).build()
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { _ -> }
                    }
                }
            }
        }
    }
}

data class PaywallItem(
    val productId: String,
    val subscriptionOfferDetails: List<ProductDetails.SubscriptionOfferDetails>
) {
    val title: String
        get() = "${freeTrial?.pricingPhases?.pricingPhaseList?.first()?.billingPeriod?.billingPeriodFormatted()} free trial, then"

    val subtitle: String
        get() = "${basePlan?.pricingPhases?.pricingPhaseList?.first()?.formattedPrice} per ${basePlan?.pricingPhases?.pricingPhaseList?.first()?.billingPeriod?.billingPeriodFormatted()}"

    val basePlan: ProductDetails.SubscriptionOfferDetails?
        get() = subscriptionOfferDetails.firstOrNull { it.pricingPhases.pricingPhaseList.size == 1 }

    val freeTrial: ProductDetails.SubscriptionOfferDetails?
        get() = subscriptionOfferDetails.firstOrNull {
            it.pricingPhases.pricingPhaseList.dropLast(1)
                .firstOrNull { it.priceAmountMicros == 0L } != null
        }
}

fun String.billingPeriodFormatted(): String {
    val period = this.substring(2)
    val count = this.substring(1, 2).toInt()

    return when (period) {
        "D" -> "$count day${if (count > 1) "s" else ""}"
        "W" -> "$count week${if (count > 1) "s" else ""}"
        "M" -> "$count month${if (count > 1) "s" else ""}"
        "Y" -> "$count year${if (count > 1) "s" else ""}"
        else -> "Unknown"
    }
}

fun String.billingPeriodDays(): Int {
    val period = this.substring(2)
    val count = this.substring(1, 2).toInt()

    return when (period) {
        "D" -> count
        "W" -> count * 7
        "M" -> count * 30
        "Y" -> count * 365
        else -> 0
    }
}