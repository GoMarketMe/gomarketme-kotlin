package co.gomarketme.kotlin

import android.content.Context
import android.os.Build
import co.gomarketme.core.GoMarketMeGoogleCore
import co.gomarketme.core.GoMarketMeGoogleCoreConfiguration
import kotlinx.coroutines.launch

/**
 * Affiliate marketing context returned by GoMarketMe when the current install
 * can be matched to an affiliate campaign.
 */
data class GoMarketMeAffiliateMarketingData(
    val campaign: Campaign,
    val affiliate: Affiliate,
    val saleDistribution: SaleDistribution,
    val affiliateCampaignCode: String,
    val deviceId: String,
    val offerCode: String?
) {
    companion object {
        fun fromMap(map: Map<String, Any?>?): GoMarketMeAffiliateMarketingData? {
            if (map.isNullOrEmpty()) {
                return null
            }

            return GoMarketMeAffiliateMarketingData(
                campaign = Campaign.fromMap(map.mapValue("campaign")),
                affiliate = Affiliate.fromMap(map.mapValue("affiliate")),
                saleDistribution = SaleDistribution.fromMap(map.mapValue("sale_distribution")),
                affiliateCampaignCode = map.stringValue("affiliate_campaign_code"),
                deviceId = map.stringValue("device_id"),
                offerCode = map.stringValue("offer_code").takeIf { it.isNotBlank() }
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
        fun fromMap(map: Map<String, Any?>?): Campaign {
            return Campaign(
                id = map.stringValue("id"),
                name = map.stringValue("name"),
                status = map.stringValue("status"),
                type = map.stringValue("type"),
                publicLinkUrl = map.stringValue("public_link_url").takeIf { it.isNotBlank() }
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
        fun fromMap(map: Map<String, Any?>?): Affiliate {
            return Affiliate(
                id = map.stringValue("id"),
                firstName = map.stringValue("first_name"),
                lastName = map.stringValue("last_name"),
                countryCode = map.stringValue("country_code"),
                instagramAccount = map.stringValue("instagram_account"),
                tiktokAccount = map.stringValue("tiktok_account"),
                xAccount = map.stringValue("x_account")
            )
        }
    }
}

data class SaleDistribution(
    val platformPercentage: String,
    val affiliatePercentage: String
) {
    companion object {
        fun fromMap(map: Map<String, Any?>?): SaleDistribution {
            return SaleDistribution(
                platformPercentage = map.stringValue("platform_percentage"),
                affiliatePercentage = map.stringValue("affiliate_percentage")
            )
        }
    }
}

data class GoMarketMeTransactionSyncResult(
    val fetchedCount: Int,
    val sentCount: Int,
    val failedCount: Int,
    val success: Boolean
)

object GoMarketMe {
    private const val sdkType = "Kotlin"
    private const val sdkVersion = "5.0.0"

    private var core: GoMarketMeGoogleCore? = null

    var affiliateMarketingData: GoMarketMeAffiliateMarketingData? = null
        private set

    /**
     * Initializes GoMarketMe. The SDK prepares attribution asynchronously.
     */
    fun initialize(context: Context, apiKey: String) {
        val appContext = context.applicationContext
        val googleCore = core ?: GoMarketMeGoogleCore(appContext)
        core = googleCore

        val initialConfig = GoMarketMeGoogleCoreConfiguration(
            apiKey = apiKey,
            sourceName = "google_play",
            sdkType = sdkType,
            sdkVersion = sdkVersion,
            packageName = appContext.packageName,
            isProduction = isProduction(appContext)
        )

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
            try {
                val prepared = googleCore.prepareAttribution(initialConfig)
                val config = prepared.first
                val affiliateDataMap = prepared.second

                affiliateMarketingData = GoMarketMeAffiliateMarketingData.fromMap(affiliateDataMap)

                googleCore.configure(config)
                googleCore.start()
            } catch (throwable: Throwable) {
                println("GoMarketMe initialization failed: ${throwable.message}")
            }
        }
    }

    /**
     * Call this after a successful purchase and before your app acknowledges,
     * consumes, or otherwise completes the transaction.
     */
    suspend fun syncAllTransactions(): GoMarketMeTransactionSyncResult {
        val googleCore = core
            ?: throw IllegalStateException("GoMarketMe SDK must be initialized before syncing transactions.")

        val result = googleCore.sendCurrentPurchasesWithResult()

        return GoMarketMeTransactionSyncResult(
            fetchedCount = result.fetchedCount,
            sentCount = result.sentCount,
            failedCount = result.failedCount,
            success = result.success
        )
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

    private fun isProduction(context: Context): Boolean {
        val installerPackageName = getInstallerPackageName(context)

        return installerPackageName == "com.android.vending" ||
            installerPackageName == "com.amazon.venezia" ||
            installerPackageName == null
    }
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>?.mapValue(key: String): Map<String, Any?>? {
    return this?.get(key) as? Map<String, Any?>
}

private fun Map<String, Any?>?.stringValue(key: String): String {
    return this?.get(key)?.toString().orEmpty()
}
