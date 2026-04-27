package co.gomarketme.kotlin

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import java.io.IOException
import java.util.Currency
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

data class GoMarketMeAffiliateMarketingData(
    val campaign: Campaign,
    val affiliate: Affiliate,
    val saleDistribution: SaleDistribution,
    val affiliateCampaignCode: String,
    val deviceId: String,
    val offerCode: String?
) {
    companion object {
        fun fromJson(json: JSONObject?): GoMarketMeAffiliateMarketingData? {
            if (json == null || json.length() == 0) {
                return null
            }

            return GoMarketMeAffiliateMarketingData(
                campaign = Campaign.fromJson(json.optJSONObject("campaign")),
                affiliate = Affiliate.fromJson(json.optJSONObject("affiliate")),
                saleDistribution = SaleDistribution.fromJson(json.optJSONObject("sale_distribution")),
                affiliateCampaignCode = json.optString("affiliate_campaign_code", ""),
                deviceId = json.optString("device_id", ""),
                offerCode = json.optString("offer_code").takeIf { it.isNotBlank() }
            )
        }
    }
}

data class Campaign(
    val id: String,
    val name: String,
    val status: String,
    val type: String,
    val publicLinkUrl: String?
) {
    companion object {
        fun fromJson(json: JSONObject?): Campaign {
            return Campaign(
                id = json?.optString("id", "").orEmpty(),
                name = json?.optString("name", "").orEmpty(),
                status = json?.optString("status", "").orEmpty(),
                type = json?.optString("type", "").orEmpty(),
                publicLinkUrl = json?.optString("public_link_url")?.takeIf { it.isNotBlank() }
            )
        }
    }
}

data class Affiliate(
    val id: String,
    val firstName: String,
    val lastName: String,
    val countryCode: String,
    val instagramAccount: String,
    val tiktokAccount: String,
    val xAccount: String
) {
    companion object {
        fun fromJson(json: JSONObject?): Affiliate {
            return Affiliate(
                id = json?.optString("id", "").orEmpty(),
                firstName = json?.optString("first_name", "").orEmpty(),
                lastName = json?.optString("last_name", "").orEmpty(),
                countryCode = json?.optString("country_code", "").orEmpty(),
                instagramAccount = json?.optString("instagram_account", "").orEmpty(),
                tiktokAccount = json?.optString("tiktok_account", "").orEmpty(),
                xAccount = json?.optString("x_account", "").orEmpty()
            )
        }
    }
}

data class SaleDistribution(
    val platformPercentage: String,
    val affiliatePercentage: String
) {
    companion object {
        fun fromJson(json: JSONObject?): SaleDistribution {
            return SaleDistribution(
                platformPercentage = json?.optString("platform_percentage", "").orEmpty(),
                affiliatePercentage = json?.optString("affiliate_percentage", "").orEmpty()
            )
        }
    }
}

object GoMarketMe {
    private const val sdkInitializedKey = "GOMARKETME_SDK_INITIALIZED"
    private const val sdkType = "Kotlin"
    private const val sdkVersion = "4.0.1"
    private const val sdkInitializationUrl = "https://4v9008q1a5.execute-api.us-west-2.amazonaws.com/prod/v1/sdk-initialization"
    private const val systemInfoUrl = "https://4v9008q1a5.execute-api.us-west-2.amazonaws.com/prod/v1/mobile/system-info"
    private const val eventUrl = "https://4v9008q1a5.execute-api.us-west-2.amazonaws.com/prod/v1/event"

    private var affiliateCampaignCode: String = ""
    private var deviceId: String = ""
    private var packageName: String = ""
    private lateinit var billingClient: BillingClient
    private val client = OkHttpClient()

    var affiliateMarketingData: GoMarketMeAffiliateMarketingData? = null
        private set

    fun initialize(context: Context, apiKey: String) {
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            if (!isSDKInitialized(appContext)) {
                postSDKInitialization(appContext, apiKey)
            }

            packageName = appContext.packageName
            val systemInfo = getSystemInfo(appContext)
            affiliateMarketingData = postSystemInfo(systemInfo, apiKey)
            affiliateMarketingData?.let { data ->
                affiliateCampaignCode = data.affiliateCampaignCode
                deviceId = data.deviceId
            }

            addListener(appContext, apiKey)
        }
    }

    private fun addListener(context: Context, apiKey: String) {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    fetchPurchases(purchases, apiKey)
                } else {
                    println("Billing listener error: ${billingResult.debugMessage}")
                }
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    println("Billing client is ready")
                } else {
                    println("Billing setup error: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                println("Billing service disconnected")
            }
        })
    }

    private fun fetchPurchases(purchases: List<Purchase>, apiKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            purchases.forEach { purchase ->
                val purchaseProductData = buildPurchaseProductJson(purchase)
                sendEventToServer("purchase_product", purchaseProductData, apiKey)
            }
        }
    }

    private suspend fun buildPurchaseProductJson(purchase: Purchase): JSONObject {
        val productIds = purchase.products
        val products = fetchProducts(productIds)
        val primaryProductId = productIds.firstOrNull().orEmpty()

        return JSONObject().apply {
            put("packageName", packageName)
            put("productID", primaryProductId)
            put("purchaseID", purchase.orderId.orEmpty())
            put("transactionDate", purchase.purchaseTime)
            put("status", purchase.purchaseState)
            put("verificationData", JSONObject().apply {
                put("localVerificationData", purchase.purchaseToken)
                put("serverVerificationData", purchase.purchaseToken)
                put("source", "google_play")
            })
            put("pendingCompletePurchase", !purchase.isAcknowledged)
            put("products", products)
        }
    }

    private suspend fun fetchProducts(productIds: List<String>): JSONArray {
        if (productIds.isEmpty()) {
            return JSONArray()
        }

        if (!::billingClient.isInitialized || !billingClient.isReady) {
            println("Billing client not ready for product details.")
            return JSONArray()
        }

        val productDetails = queryProductDetails(productIds, BillingClient.ProductType.INAPP) +
            queryProductDetails(productIds, BillingClient.ProductType.SUBS)

        return JSONArray().apply {
            productDetails.forEach { productDetails ->
                put(productDetails.toProductJson())
            }
        }
    }

    private suspend fun queryProductDetails(
        productIds: List<String>,
        productType: String
    ): List<ProductDetails> {
        val products = productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        return suspendCoroutine { continuation ->
            billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(queryProductDetailsResult.productDetailsList)
                } else {
                    println("Error fetching product details: ${billingResult.debugMessage}")
                    continuation.resume(emptyList())
                }
            }
        }
    }

    private fun ProductDetails.toProductJson(): JSONObject {
        val oneTimePurchaseOffer = oneTimePurchaseOfferDetails
        val subscriptionPricingPhase = subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()

        val formattedPrice = oneTimePurchaseOffer?.formattedPrice
            ?: subscriptionPricingPhase?.formattedPrice
            ?: ""
        val priceAmountMicros = oneTimePurchaseOffer?.priceAmountMicros
            ?: subscriptionPricingPhase?.priceAmountMicros
            ?: 0L
        val priceCurrencyCode = oneTimePurchaseOffer?.priceCurrencyCode
            ?: subscriptionPricingPhase?.priceCurrencyCode
            ?: ""

        return JSONObject().apply {
            put("packageName", packageName)
            put("productID", productId)
            put("productTitle", title)
            put("productDescription", description)
            put("productPrice", formattedPrice)
            put("productRawPrice", priceAmountMicros / 1_000_000.0)
            put("productCurrencyCode", priceCurrencyCode)
            put("productCurrencySymbol", getCurrencySymbol(priceCurrencyCode))
            put("hashCode", hashCode())
        }
    }

    private fun getCurrencySymbol(currencyCode: String): String {
        if (currencyCode.isBlank()) {
            return ""
        }

        return try {
            Currency.getInstance(currencyCode).symbol
        } catch (e: Exception) {
            currencyCode
        }
    }

    private fun sendEventToServer(eventType: String, eventData: JSONObject, apiKey: String) {
        if (affiliateCampaignCode.isNotEmpty() && deviceId.isNotEmpty()) {
            val requestBody = eventData.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(eventUrl)
                .post(requestBody)
                .addHeader("x-affiliate-campaign-code", affiliateCampaignCode)
                .addHeader("x-device-id", deviceId)
                .addHeader("x-product-type", "android")
                .addHeader("x-source-name", "google_play")
                .addHeader("x-api-key", apiKey)
                .addHeader("x-event-type", eventType)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to send $eventType event: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (it.isSuccessful) {
                            println("$eventType event sent successfully")
                        } else {
                            println("Failed to send $eventType event. Status: ${it.code}")
                        }
                    }
                }
            })
        } else {
            println("Affiliate campaign code or device ID not set. Event not sent.")
        }
    }

    private fun isSDKInitialized(context: Context): Boolean {
        val prefs = context.getSharedPreferences("GoMarketMePrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(sdkInitializedKey, false)
    }

    private fun markSDKAsInitialized(context: Context) {
        val prefs = context.getSharedPreferences("GoMarketMePrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(sdkInitializedKey, true).apply()
    }

    private fun getAndroidId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
    }

    private fun getSystemInfo(context: Context): Map<String, Any> {
        val metrics = context.resources.displayMetrics
        val androidId = getAndroidId(context)

        return mapOf(
            "device_info" to mapOf(
                "deviceId" to androidId,
                "androidId" to androidId,
                "brand" to Build.BRAND,
                "model" to Build.MODEL,
                "manufacturer" to Build.MANUFACTURER,
                "systemName" to "Android",
                "systemVersion" to Build.VERSION.RELEASE.orEmpty(),
                "sdkInt" to Build.VERSION.SDK_INT
            ),
            "window_info" to mapOf(
                "devicePixelRatio" to metrics.density,
                "width" to metrics.widthPixels,
                "height" to metrics.heightPixels
            ),
            "time_zone" to TimeZone.getDefault().id,
            "language_code" to Locale.getDefault().toLanguageTag(),
            "sdk_type" to sdkType,
            "sdk_version" to sdkVersion,
            "package_name" to context.packageName,
            "is_production" to isProduction(context)
        )
    }

    private fun postSDKInitialization(context: Context, apiKey: String) {
        val requestBody = JSONObject().toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(sdkInitializationUrl)
            .post(requestBody)
            .addHeader("x-api-key", apiKey)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to initialize SDK: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        markSDKAsInitialized(context)
                    } else {
                        println("SDK initialization failed: ${it.code}")
                    }
                }
            }
        })
    }

    private suspend fun postSystemInfo(
        systemInfo: Map<String, Any>,
        apiKey: String
    ): GoMarketMeAffiliateMarketingData? {
        val requestBody = JSONObject(systemInfo).toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(systemInfoUrl)
            .post(requestBody)
            .addHeader("x-api-key", apiKey)
            .build()

        return suspendCoroutine { continuation ->
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to post system info: $e")
                    continuation.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (it.isSuccessful) {
                            val responseBody = it.body?.string().orEmpty()
                            val data = parseSystemInfoResponse(responseBody)
                            continuation.resume(data)
                        } else {
                            println("Failed to post system info: ${it.code}")
                            continuation.resume(null)
                        }
                    }
                }
            })
        }
    }

    private fun parseSystemInfoResponse(responseBody: String): GoMarketMeAffiliateMarketingData? {
        if (responseBody.isBlank()) {
            return null
        }

        return try {
            val data = GoMarketMeAffiliateMarketingData.fromJson(JSONObject(responseBody))
            affiliateCampaignCode = data?.affiliateCampaignCode.orEmpty()
            deviceId = data?.deviceId.orEmpty()
            data
        } catch (e: Exception) {
            println("Failed to parse system info response: $e")
            null
        }
    }

    private fun isProduction(context: Context): Boolean {
        val installerPackageName = getInstallerPackageName(context)

        return installerPackageName == "com.android.vending" ||
            installerPackageName == "com.amazon.venezia" ||
            installerPackageName == null
    }

    @Suppress("DEPRECATION")
    private fun getInstallerPackageName(context: Context): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager
                    .getInstallSourceInfo(context.packageName)
                    .installingPackageName
            } else {
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        } catch (e: Exception) {
            println("Error determining installer package name: $e")
            null
        }
    }
}
