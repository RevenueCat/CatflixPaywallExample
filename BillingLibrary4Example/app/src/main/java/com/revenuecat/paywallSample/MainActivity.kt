package com.revenuecat.paywallSample

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
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
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams

class MainActivity : AppCompatActivity(), PurchasesUpdatedListener, BillingClientStateListener {
    private val productIds = listOf(
        "bc4.premium.weekly",
        "bc4.premium.monthly",
        "bc4.premium.yearly",
        "bc4.standard.weekly",
        "bc4.standard.monthly",
        "bc4.standard.yearly"
    )
    private val products = listOf(
        "Premium", "Standard"
    )

    private var paywallItems = mutableStateListOf<PaywallItem>()
    private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        billingClient = BillingClient
            .newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        billingClient.startConnection(this)

        setContent {
            PaywallScreen(
                paywallItems = paywallItems.sortedBy {
                    it.productDetails.subscriptionPeriod.billingPeriodDays()
                },
                products = products,
                onPurchasePaywallItem = { paywallItem ->
                    subscribe(paywallItem.productDetails)
                }
            )
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
        val newBuilder = SkuDetailsParams.newBuilder()
        val productType = BillingClient.SkuType.SUBS

        newBuilder.setSkusList(productIds).setType(productType)
        billingClient.querySkuDetailsAsync(newBuilder.build()) { billingResult, skuDetails ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetails?.isNotEmpty() == true) {
                runOnUiThread {
                    val skuDetailsByProduct = mutableMapOf<String, MutableList<SkuDetails>>()

                    skuDetails.forEach {
                        val key = when {
                            it.sku.startsWith("bc4.premium") -> "Premium"
                            it.sku.startsWith("bc4.standard") -> "Standard"
                            else -> throw Exception("Unknown product ${it.sku}")
                        }
                        skuDetailsByProduct.getOrPut(key) { mutableListOf() }.add(it)
                    }

                    paywallItems.clear()
                    paywallItems.addAll(skuDetailsByProduct.flatMap { (product, skuDetails) ->
                        skuDetails.map { PaywallItem(it, product) }
                    })
                }
            }
        }
    }

    private fun subscribe(skuDetails: SkuDetails) {
        if (billingClient.isReady) {
            val flowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build()
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
    val productDetails: SkuDetails, val productId: String
) {
    val title: String
        get() = "${productDetails.freeTrialPeriod.billingPeriodFormatted()} free trial, then"
    val subtitle: String
        get() = "${productDetails.originalPrice} per ${productDetails.subscriptionPeriod.billingPeriodFormatted()}"
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