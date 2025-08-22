package com.smartswitch

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.smartswitch.ads.inter_ads.InterstitialClass
import com.smartswitch.databinding.FragmentPremiumNewBinding
import com.smartswitch.subscriptions.Constants
import com.smartswitch.subscriptions.GooglePlayBuySubscription
import com.smartswitch.subscriptions.PrefUtil
import com.smartswitch.subscriptions.SharedPreferencesClass
import com.smartswitch.subscriptions.SubscriptionPurchaseInterface

import com.smartswitch.utils.extensions.handleBackPressWithAction
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.extensions.setSafeOnClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

@AndroidEntryPoint
class PremiumFragment : Fragment(), SubscriptionPurchaseInterface {
    private var _binding: FragmentPremiumNewBinding? = null
    private val binding get() = _binding!!
    private val args: PremiumFragmentArgs by navArgs()


    private var billingClient: BillingClient? = null
    private val arrayList = ArrayList<ProductDetails>()

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private var mSharePrefHelper: SharedPreferencesClass? = null
    private var check: String = ""
    private val oneWeak: String = "Weekly"
    private var weaklyPrice = ""
    private var productDetailsList: ArrayList<ProductDetails> = ArrayList()
    private var retryCount = 1
    private val TAG = "PremiumActivityTAG"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiumNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isAlive { activityContext ->
            (activityContext as FragmentActivity).handleBackPressWithAction {
                onCrossButtonClick()
            }
            initializePreferences()
            setupUI()
            initializeBillingClient()
            setupClickListeners()

        }


    }

    private fun initializePreferences() {
        mSharePrefHelper = SharedPreferencesClass(requireContext())
        mSharePrefHelper?.setBooleanPreferences(mSharePrefHelper!!.REMOVE_AD_ACTIVITY_OPEN, true)
    }

    private fun setupClickListeners() {
        binding.cancelBtn.setSafeOnClickListener {
            onCrossButtonClick()
        }
        binding.noPaymentNow.setSafeOnClickListener {
            onCrossButtonClick()
        }
        binding.btnContinue.setOnClickListener {
            handleSubscribeClick()
        }
        binding.cancelAnytime.setOnClickListener {
            if (PrefUtil(requireContext()).getBool("is_premium", false)) {
                openPlayStoreSubscription()
            } else {
                Snackbar.make(
                    binding.root,
                    "You didn't subscribe to any plan.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun setupUI() {

        Glide.with(this).load(R.drawable.gif_new_premium).into(binding.mainGif)

    }
    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(requireContext())
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().enablePrepaidPlans()
                    .build()
            )
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    purchases.forEach { purchase ->
                        verifySubPurchase(purchase)
                    }
                }
            }
            .build()

        establishConnection()
        GooglePlayBuySubscription.purchasesInterface = this

    }

    private fun establishConnection() {
        Log.d(TAG, "Attempting to establish billing connection...")
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "Billing setup finished: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connection successful, querying products...")
                    getProducts()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    retryConnection()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e(TAG, "Billing service disconnected")
                retryConnection()
            }
        })
    }
    private fun retryConnection() {
        if (retryCount <= 3) {
            lifecycleScope.launch {
                delay(1000L * retryCount)
                establishConnection()
                retryCount++
            }
        }
    }
    private fun getProducts() {
        lifecycleScope.launch {
            try {
                val productList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("weekly_freetrial")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )

                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()

                val result = suspendCancellableCoroutine<Pair<BillingResult, List<ProductDetails>?>> { continuation ->
                    billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
                        continuation.resume(
                            billingResult to productDetailsResult.productDetailsList
                        )
                    }
                }

                val (billingResult, productDetails) = result

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    productDetails?.let { details ->
                        Log.d(TAG, "Fetched products: ${details.map { it.productId }}")
                        productDetailsList.clear()
                        productDetailsList.addAll(details)
                        withContext(Dispatchers.Main) {
                            updateWeeklyPrice()
                        }
                    }
                } else {
                    Log.e(TAG, "Product query failed: ${billingResult.debugMessage}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying products", e)
            }
        }
    }

    // only update weekly price
    private fun updateWeeklyPrice() {
        if (productDetailsList.isEmpty()) {
            Log.e(TAG, "Product details list is empty")
            binding.WeeklyPrice.text = getString(R.string.price_unavailable)
            return
        }

        val weeklyProduct = productDetailsList.firstOrNull { it.productId == "weekly_freetrial" }
        if (weeklyProduct == null) {
            Log.e(TAG, "Weekly product not found in list. Available products: ${productDetailsList.map { it.productId }}")
            binding.WeeklyPrice.text = getString(R.string.price_unavailable)
            return
        }

        val offerDetails = weeklyProduct.subscriptionOfferDetails
        if (offerDetails.isNullOrEmpty()) {
            Log.e(TAG, "No offer details for Weekly product")
            binding.WeeklyPrice.text = getString(R.string.price_unavailable)
            return
        }

        val firstOffer = offerDetails.first()
        val pricingPhases = firstOffer.pricingPhases.pricingPhaseList
        if (pricingPhases.isNullOrEmpty()) {
            Log.e(TAG, "No pricing phases for Weekly product")
            binding.WeeklyPrice.text = getString(R.string.price_unavailable)
            return
        }

        val firstPricingPhase = pricingPhases.first()
        val realPricingPhase = pricingPhases.find { phase -> phase.priceAmountMicros > 0 }
        val weeklyPriceStr = realPricingPhase?.formattedPrice ?: ""
       // val weeklyPriceStr = firstPricingPhase.formattedPrice ?: ""

        if (weeklyPriceStr.isNotEmpty()) {
            Log.d(TAG, "Weekly price found: $weeklyPriceStr")
            binding.WeeklyPrice.text = "${getString(R.string.just)} $weeklyPriceStr ${getString(R.string.aweek)}"
            weaklyPrice = weeklyPriceStr
        } else {
            Log.e(TAG, "Weekly price is empty")
            binding.WeeklyPrice.text = getString(R.string.price_unavailable)
        }
    }

    private fun verifySubPurchase(purchase: Purchase) {
        lifecycleScope.launch {
            try {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                val result = suspendCancellableCoroutine<BillingResult> { continuation ->
                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                        continuation.resume(billingResult)
                    }
                }

                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    withContext(Dispatchers.Main) {
                        PrefUtil(requireContext()).setBool("is_premium", true)
                        Toast.makeText(
                            requireContext(),
                            "Subscription verified",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying purchase", e)
            }
        }
    }
    private fun handleSubscribeClick() {
        if (!isNetworkAvailable(requireContext())) {
            Toast.makeText(requireContext(), "No Internet", Toast.LENGTH_SHORT).show()
            return
        }
        if (productDetailsList.isEmpty()) {
            Log.e(TAG, "Product details list is empty")
            return
        }
        launchPurchaseFlow(productDetailsList[0])
    }

    private fun launchPurchaseFlow(productDetails: ProductDetails) {
        lifecycleScope.launch {
            try {
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                    ?: throw IllegalStateException("No offer token available")

                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                val billingResult = suspendCancellableCoroutine<BillingResult> { continuation ->
                    billingClient?.launchBillingFlow(requireActivity(), billingFlowParams)?.let {
                        continuation.resume(it)
                    } ?: continuation.resume(
                        BillingResult.newBuilder()
                            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                            .setDebugMessage("Billing client is null")
                            .build()
                    )
                }

                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "Launch billing flow failed: ${billingResult.debugMessage}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error launching billing flow", e)
            }
        }
    }

    private fun openPlayStoreSubscription() {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/account/subscriptions")
                )
            );
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        billingClient?.endConnection()
    }

    private fun onCrossButtonClick() {
        isAlive { activityContext ->
            when (args.source) {
                "home" -> {
                    findNavController().popBackStack()
                }

                "language" -> {
                    // TODO : Goto Home
                    if ( PrefUtil(requireContext()).getBool("is_premium", false) || !InterstitialClass.isInternetAvailable(
                            requireContext()
                        )
                    ) {
                        findNavController().navigate(R.id.action_premiumFragment_to_homeSendifyFragment)
                    } else {
                        InterstitialClass.request_interstitial(
                            requireContext(),
                            requireActivity(),
                            getString(R.string.premium_screen_inter)
                        ) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                isAlive {
                                    findNavController().navigate(R.id.action_premiumFragment_to_homeSendifyFragment)
                                }
                            }, 200L)
                        }
                    }
                }

                "permission" -> {
                    if ( PrefUtil(requireContext()).getBool("is_premium", false) || !InterstitialClass.isInternetAvailable(
                            requireContext()
                        )
                    ) {
                        findNavController().navigate(R.id.action_premiumFragment_to_homeSendifyFragment)
                    } else {
                        InterstitialClass.request_interstitial(
                            requireContext(),
                            requireActivity(),
                            getString(R.string.premium_screen_inter)
                        ) {
                            isAlive {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    isAlive {
                                        findNavController().navigate(R.id.action_premiumFragment_to_homeSendifyFragment)
                                    }
                                }, 200L)
                            }
                        }


                    }
                }
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun productPurchasedSuccessful() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                PrefUtil(requireContext()).setBool("is_premium", true)
                Toast.makeText(requireContext(), "Subscribed successfully", Toast.LENGTH_SHORT)
                    .show()
                onCrossButtonClick()

            }
        }
    }

    override fun productPurchaseFailed() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Subscription Failed", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo!!.isConnected
    }
}