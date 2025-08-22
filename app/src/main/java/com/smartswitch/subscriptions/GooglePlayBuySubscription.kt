package com.smartswitch.subscriptions

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class GooglePlayBuySubscription {

    companion object {
        private const val TAG = "GPlayBuySubscription"

        var purchasesInterface: SubscriptionPurchaseInterface? = null

        private var billingClient: BillingClient? = null
        var productDetailsList: List<ProductDetails>? = null

        private var sharedPrefHelper: SharedPreferencesClass? = null
        private var prefUtil: PrefUtil? = null

        fun initBillingClient(activity: Activity) {
            sharedPrefHelper = SharedPreferencesClass(activity)
            prefUtil = PrefUtil(activity)

            if (billingClient == null) {
                billingClient = BillingClient.newBuilder(activity)
                    .setListener(purchasesUpdatedListener)
                    .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().enablePrepaidPlans().build())
                    .build()
            }
        }

        fun makeGooglePlayConnectionRequest() {
            billingClient?.startConnection(billingClientStateListener)
        }

        private val purchasesUpdatedListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        purchases?.forEach { purchase ->
                            acknowledgePurchase(purchase)
                        }
                    }

                    BillingClient.BillingResponseCode.USER_CANCELED -> {
                        Log.d(TAG, "User canceled purchase")
                        purchasesInterface?.productPurchaseFailed()
                    }

                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                        Log.d(TAG, "Item already owned")
                        sharedPrefHelper?.setBooleanPreferences(
                            sharedPrefHelper!!.IS_SUBSCRIBED,
                            true
                        )
                        purchasesInterface?.productPurchasedSuccessful()
                    }

                    BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                        Log.d(TAG, "Item not owned")
                        sharedPrefHelper?.setBooleanPreferences(
                            sharedPrefHelper!!.IS_SUBSCRIBED,
                            false
                        )
                        prefUtil?.setBool("is_premium", false)
                    }

                    else -> {
                        Log.d(TAG, "Purchase failed: ${billingResult.responseCode}")
                        purchasesInterface?.productPurchaseFailed()
                    }
                }
            }

        private val billingClientStateListener = object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected")
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup finished")
                    runBlocking {
                        queryProductDetails()
                    }
                }
            }
        }

        private suspend fun queryProductDetails() = withContext(Dispatchers.IO) {
            checkPurchaseState()

            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId("weekly_freetrial")
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),

            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            billingClient?.queryProductDetailsAsync(
                params
            ) { billingResult: BillingResult, productDetailsResult: QueryProductDetailsResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    productDetailsList = productDetailsResult.productDetailsList
                    Log.d(TAG, "Loaded product count: ${productDetailsList?.size}")
                } else {
                    Log.d(TAG, "Error: ${billingResult.debugMessage}")
                }
            }

        }

        private fun acknowledgePurchase(purchase: Purchase) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

            if (!purchase.isAcknowledged) {
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient?.acknowledgePurchase(acknowledgeParams) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        handlePurchase(purchase.purchaseToken)
                        purchasesInterface?.productPurchasedSuccessful()
                    } else {
                        Log.d(TAG, "Acknowledge failed: ${result.responseCode}")
                        purchasesInterface?.productPurchaseFailed()
                    }
                }
            } else {
                handlePurchase(purchase.purchaseToken)
            }
        }

        private fun handlePurchase(purchaseToken: String) {
            Log.d(TAG, "Handling purchase")
            sharedPrefHelper?.setBooleanPreferences(sharedPrefHelper!!.IS_SUBSCRIBED, true)
            sharedPrefHelper?.setStringPreferences(
                sharedPrefHelper!!.TOKEN_SUBSCRIPTION,
                purchaseToken
            )
            prefUtil?.setBool("is_premium", true)
        }

        fun checkPurchaseState() {
            val queryParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            billingClient?.queryPurchasesAsync(queryParams) { result, purchases ->
                Log.d(TAG, "Checking purchase state: ${purchases.size}")
                if (purchases.isEmpty()) {
                    prefUtil?.setBool("is_premium", false)
                }

                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        } else {
                            handlePurchase(purchase.purchaseToken)
                        }
                    }
                }
            }
        }

        fun launchPurchaseFlow(activity: Activity, productId: String) {
            val productDetails = productDetailsList?.find { it.productId == productId }
            if (productDetails == null) {
                Log.e(TAG, "ProductDetails not found for $productId")
                return
            }

            val offerToken = productDetails.subscriptionOfferDetails
                ?.firstOrNull()?.offerToken

            if (offerToken == null) {
                Log.e(TAG, "No offerToken for $productId")
                return
            }

            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()

            billingClient?.launchBillingFlow(activity, billingFlowParams)
        }
    }
}
