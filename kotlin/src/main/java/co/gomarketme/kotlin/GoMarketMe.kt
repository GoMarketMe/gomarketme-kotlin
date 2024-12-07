package co.gomarketme.kotlin

import android.content.Context
import android.provider.Settings
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.android.billingclient.api.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object GoMarketMe {
    private const val sdkInitializedKey = "GOMARKETME_SDK_INITIALIZED"
    private var _affiliateCampaignCode: String = ""
    private var _deviceId: String = ""
    private const val sdkInitializationUrl = "https://api.gomarketme.net/v1/sdk-initialization"
    private const val systemInfoUrl = "https://api.gomarketme.net/v1/mobile/system-info"
    private const val eventUrl = "https://api.gomarketme.net/v1/event"
    private lateinit var billingClient: BillingClient
    private val client = OkHttpClient()

    fun initialize(context: Context, apiKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (!isSDKInitialized(context)) {
                postSDKInitialization(context, apiKey)
            }
            val systemInfo = getSystemInfo(context)
            postSystemInfo(systemInfo, apiKey)
            addListener(context, apiKey)
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
            .enablePendingPurchases()
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
                println("Billing service disconnected. Retrying...")
                addListener(context, apiKey)
            }
        })
    }

    private fun fetchPurchases(purchases: List<Purchase>, apiKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            purchases.forEach { purchase ->
                val product = fetchProducts(purchase.skus)
                sendEventToServer("product", product, apiKey)
                val purchaseData = JSONObject().apply {
                    put("productID", purchase.skus.firstOrNull() ?: "")
                    put("purchaseID", purchase.orderId)
                    put("transactionDate", purchase.purchaseTime)
                    put("status", purchase.purchaseState)
                    put("verificationData", JSONObject().apply {
                        put("localVerificationData", purchase.originalJson)
                        put("serverVerificationData", purchase.purchaseToken)
                        put("source", "google_play")
                    })
                    put("pendingCompletePurchase", purchase.isAcknowledged)
                }
                sendEventToServer("purchase", purchaseData, apiKey)
            }
        }
    }

    private suspend fun fetchProducts(skus: List<String>): JSONObject {
        var details = JSONObject()
        if (billingClient.isReady) {
            val params = SkuDetailsParams.newBuilder()
                .setSkusList(skus)
                .setType(BillingClient.SkuType.INAPP)
                .build()

            suspendCoroutine<Unit> { continuation ->
                billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                        skuDetailsList.forEach { skuDetail ->
                            details = JSONObject().apply {
                                put("productID", skuDetail.sku)
                                put("productTitle", skuDetail.title)
                                put("productDescription", skuDetail.description)
                                put("productPrice", skuDetail.price)
                                put("productRawPrice", skuDetail.priceAmountMicros / 1_000_000.0) // Convert micros to currency units
                                put("productCurrencyCode", skuDetail.priceCurrencyCode)
                                put("productCurrencySymbol", getCurrencySymbol(skuDetail.priceCurrencyCode)) // Optional: Custom helper
                                put("hashCode", skuDetail.hashCode())
                            }
                        }
                    } else {
                        println("Error fetching SKU details: ${billingResult.debugMessage}")
                    }
                    continuation.resume(Unit)
                }
            }
        } else {
            println("Billing client not ready for SKU details.")
        }
        return details
    }

    private fun getCurrencySymbol(currencyCode: String): String {
        return try {
            val currency = Currency.getInstance(currencyCode)
            currency.symbol
        } catch (e: Exception) {
            currencyCode
        }
    }

    private fun sendEventToServer(eventType: String, eventData: JSONObject, apiKey: String) {
        if (_affiliateCampaignCode.isNotEmpty() && _deviceId.isNotEmpty()) {
            val requestBody = eventData.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(eventUrl)
                .post(requestBody)
                .addHeader("x-affiliate-campaign-code", _affiliateCampaignCode)
                .addHeader("x-device-id", _deviceId)
                .addHeader("x-source-name", "google_play")
                .addHeader("x-api-key", apiKey)
                .addHeader("x-event-type", eventType)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to send $eventType event: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        println("$eventType event sent successfully")
                    } else {
                        println("Failed to send $eventType event. Status: ${response.code}")
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
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun getSystemInfo(context: Context): Map<String, Any> {
        val metrics = context.resources.displayMetrics
        return mapOf(
            "device_info" to mapOf(
                "androidId" to getAndroidId(context),
                "model" to Build.MODEL,
                "manufacturer" to Build.MANUFACTURER,
                "systemVersion" to Build.VERSION.SDK_INT
            ),
            "window_info" to mapOf(
                "devicePixelRatio" to metrics.density,
                "width" to metrics.widthPixels,
                "height" to metrics.heightPixels
            ),
            "time_zone" to TimeZone.getDefault().id,
            "language_code" to Locale.getDefault().language
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
                if (response.isSuccessful) {
                    markSDKAsInitialized(context)
                } else {
                    println("SDK initialization failed: ${response.code}")
                }
            }
        })
    }

    private fun postSystemInfo(systemInfo: Map<String, Any>, apiKey: String) {
        val requestBody = JSONObject(systemInfo).toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(systemInfoUrl)
            .post(requestBody)
            .addHeader("x-api-key", apiKey)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to post system info: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { parseSystemInfoResponse(it) }
                } else {
                    println("Failed to post system info: ${response.code}")
                }
            }
        })
    }

    private fun parseSystemInfoResponse(responseBody: String) {
        val json = JSONObject(responseBody)
        _affiliateCampaignCode = json.optString("affiliate_campaign_code", "")
        _deviceId = json.optString("device_id", "")
    }
}
