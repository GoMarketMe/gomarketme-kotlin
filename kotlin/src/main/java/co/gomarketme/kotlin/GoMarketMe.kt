package co.gomarketme.kotlin

import android.os.Build
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

object GoMarketMe {
    private const val sdkInitializedKey = "GOMARKETME_SDK_INITIALIZED"
    private var affiliateCampaignCode: String = ""
    private var deviceId: String = ""
    private const val sdkInitializationUrl = "https://api.gomarketme.net/v1/sdk-initialization"
    private const val systemInfoUrl = "https://api.gomarketme.net/v1/mobile/system-info"
    private const val eventUrl = "https://api.gomarketme.net/v1/event"

    private val client = OkHttpClient()

    suspend fun initialize(apiKey: String) {
        try {
            val isSDKInitialized = isSDKInitialized()
            if (!isSDKInitialized) {
                postSDKInitialization(apiKey)
            }
            val systemInfo = getSystemInfo()
            postSystemInfo(systemInfo, apiKey)
            addListener(apiKey)
        } catch (e: Exception) {
            println("Error initializing GoMarketMe: $e")
        }
    }

    private suspend fun addListener(apiKey: String) {
        // Implement purchase listener logic here as per the platform's purchase APIs
    }

    private suspend fun getSystemInfo(): Map<String, Any> {
        val deviceData = mutableMapOf<String, Any>()
        val deviceInfo = DeviceInfo()
        val windowMetrics = WindowMetrics()
        deviceData.putAll(deviceInfo.getDeviceInfo())
        val windowInfo = windowMetrics.getWindowInfo()

        return mapOf(
            "device_info" to deviceData,
            "window_info" to windowInfo,
            "time_zone_code" to TimeZone.getDefault().id,
            "language_code" to Locale.getDefault().toString()
        )
    }

    private suspend fun postSDKInitialization(apiKey: String) {
        val uri = HttpUrl.parse(sdkInitializationUrl)!!
        val request = Request.Builder()
            .url(uri)
            .post(JSONObject().toString().toRequestBody("application/json".toMediaType()))
            .addHeader("x-api-key", apiKey)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Error sending SDK information to server: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    markSDKAsInitialized()
                } else {
                    println("Failed to mark SDK as Initialized. Status code: ${response.code}")
                }
            }
        })
    }

    private suspend fun postSystemInfo(systemInfo: Map<String, Any>, apiKey: String) {
        val uri = HttpUrl.parse(systemInfoUrl)!!
        val requestBody = JSONObject(systemInfo).toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(uri)
            .post(requestBody)
            .addHeader("x-api-key", apiKey)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Error sending system info to server: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    println("System Info sent successfully")
                    val responseData = JSONObject(response.body!!.string())
                    affiliateCampaignCode = responseData.optString("affiliate_campaign_code")
                    deviceId = responseData.optString("device_id")
                } else {
                    println("Failed to send system info. Status code: ${response.code}")
                }
            }
        })
    }

    private fun markSDKAsInitialized() {
        // Implementation to save SDK initialization state
    }

    private fun isSDKInitialized(): Boolean {
        // Implementation to check SDK initialization state
        return false
    }
}
