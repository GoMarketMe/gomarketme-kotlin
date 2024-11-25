package co.gomarketme.kotlin

import android.content.Context
import android.provider.Settings
import android.os.Build
import android.util.DisplayMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*
import com.android.billingclient.api.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object GoMarketMe {
    private const val sdkInitializedKey = "GOMARKETME_SDK_INITIALIZED"
    private var affiliateCampaignCode: String = ""
    private var deviceId: String = ""
    private const val sdkInitializationUrl = "https://api.gomarketme.net/v1/sdk-initialization"
    private const val systemInfoUrl = "https://api.gomarketme.net/v1/mobile/system-info"
    private const val eventUrl = "https://api.gomarketme.net/v1/event"
    private lateinit var billingClient: BillingClient

    private val client = OkHttpClient()

    fun initialize(context: Context, apiKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val isSDKInitialized = isSDKInitialized(context)
            if (!isSDKInitialized) {
                postSDKInitialization(context, apiKey)
            }
            val systemInfo = getSystemInfo(context)
            postSystemInfo(systemInfo, apiKey)
            setupBillingClient(context, apiKey)
        }
    }

    private fun setupBillingClient(context: Context, apiKey: String) {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    handlePurchases(purchases, apiKey)
                } else {
                    println("Purchase error or user cancelled: ${billingResult.debugMessage}")
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    println("Billing Client is ready")
                } else {
                    println("Error setting up Billing Client: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                println("Billing service disconnected, attempting to reconnect.")
                setupBillingClient(context, apiKey)
            }
        })
    }

    private fun handlePurchases(purchases: List<Purchase>, apiKey: String) {
        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        sendEventToServer(purchase, apiKey)
                    }
                }
            }
        }
    }

    private fun sendEventToServer(purchase: Purchase, apiKey: String) {
        val purchaseInfo = JSONObject().apply {
            put("purchaseToken", purchase.purchaseToken)
            put("orderId", purchase.orderId)
            put("productId", purchase.skus.firstOrNull() ?: "")
            put("purchaseTime", purchase.purchaseTime)
        }

        val uri = eventUrl.toHttpUrlOrNull()!!
        val requestBody = purchaseInfo.toString().toRequestBody("application/json".toMediaType())

        CoroutineScope(Dispatchers.IO).launch {
            val request = Request.Builder()
                .url(uri)
                .post(requestBody)
                .addHeader("x-api-key", apiKey)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to send purchase event to server: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        println("Purchase event sent successfully")
                    } else {
                        println("Failed to send purchase event. Status code: ${response.code}")
                    }
                }
            })
        }
    }

    private fun isSDKInitialized(context: Context): Boolean {
        val prefs = context.getSharedPreferences("GoMarketMePrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(sdkInitializedKey, false)
    }

    private fun markSDKAsInitialized(context: Context) {
        val prefs = context.getSharedPreferences("GoMarketMePrefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean(sdkInitializedKey, true)
            apply()
        }
    }

    private fun getAndroidId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun getSystemInfo(context: Context): Map<String, Any> {
        val metrics = context.resources.displayMetrics
        val deviceInfo = mapOf(
            "androidId" to getAndroidId(context),
            "model" to Build.MODEL,
            "manufacturer" to Build.MANUFACTURER,
            "systemVersion" to Build.VERSION.SDK_INT,
            "SDK_INT" to Build.VERSION.SDK_INT,
            "BASE_OS" to Build.VERSION.BASE_OS,
            "RELEASE" to Build.VERSION.RELEASE,
            "CODENAME" to Build.VERSION.CODENAME,
            "INCREMENTAL" to Build.VERSION.INCREMENTAL,
            "SDK" to Build.VERSION.SDK,
            "MEDIA_PERFORMANCE_CLASS" to Build.VERSION.MEDIA_PERFORMANCE_CLASS,
            "PREVIEW_SDK_INT" to Build.VERSION.PREVIEW_SDK_INT,
            "RELEASE_OR_CODENAME" to Build.VERSION.RELEASE_OR_CODENAME,
            "RELEASE_OR_PREVIEW_DISPLAY" to Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY,
            "SECURITY_PATCH" to Build.VERSION.SECURITY_PATCH,
            "PREVIEW_SDK_INT" to Build.VERSION.PREVIEW_SDK_INT,
            "USER" to Build.USER,
        )

        println("SDK_INT")
        print(Build.VERSION.SDK_INT)
        println("BASE_OS")
        print(Build.VERSION.BASE_OS)
        println("RELEASE")
        print(Build.VERSION.RELEASE)
        println("CODENAME")
        print(Build.VERSION.CODENAME)
        println("INCREMENTAL")
        print(Build.VERSION.INCREMENTAL)
        println("SDK")
        print(Build.VERSION.SDK)
        println("MEDIA_PERFORMANCE_CLASS")
        print(Build.VERSION.MEDIA_PERFORMANCE_CLASS)
        println("PREVIEW_SDK_INT")
        print(Build.VERSION.PREVIEW_SDK_INT)
        println("RELEASE_OR_CODENAME")
        print(Build.VERSION.RELEASE_OR_CODENAME)
        println("RELEASE_OR_PREVIEW_DISPLAY")
        print(Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY)
        println("SECURITY_PATCH")
        print(Build.VERSION.SECURITY_PATCH)
        println("PREVIEW_SDK_INT")
        print(Build.VERSION.PREVIEW_SDK_INT)

        val windowInfo = mapOf(
            "devicePixelRatio" to metrics.density,
            "width" to metrics.widthPixels,
            "height" to metrics.heightPixels
        )

        return mapOf(
            "device_info" to deviceInfo,
            "window_info" to windowInfo,
            "time_zone" to TimeZone.getDefault().id,
            "language_code" to Locale.getDefault().language
        )
    }

    private fun postSDKInitialization(context: Context, apiKey: String) {
        val uri = sdkInitializationUrl.toHttpUrlOrNull()!!
        val requestBody = JSONObject().apply {
            put("apiKey", apiKey) // Assuming the API expects an apiKey field
        }.toString().toRequestBody("application/json".toMediaType())

        CoroutineScope(Dispatchers.IO).launch {
            val request = Request.Builder()
                .url(uri)
                .post(requestBody)
                .addHeader("x-api-key", apiKey)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to post SDK initialization: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        markSDKAsInitialized(context)
                    } else {
                        println("SDK initialization failed. Status code: ${response.code}")
                    }
                }
            })
        }
    }

    private fun postSystemInfo(systemInfo: Map<String, Any>, apiKey: String) {
        val uri = systemInfoUrl.toHttpUrlOrNull()!!
        val requestBody = JSONObject(systemInfo).toString().toRequestBody("application/json".toMediaType())

        CoroutineScope(Dispatchers.IO).launch {
            val request = Request.Builder()
                .url(uri)
                .post(requestBody)
                .addHeader("x-api-key", apiKey)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to post system info: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        println("System info posted successfully")
                    } else {
                        println("Failed to post system info. Status code: ${response.code}")
                    }
                }
            })
        }
    }
}
