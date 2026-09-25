package co.gomarketme.kotlin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoMarketMeAffiliateMarketingDataTest {
    private val base = mapOf<String, Any?>(
        "campaign" to emptyMap<String, Any?>(),
        "affiliate" to emptyMap<String, Any?>(),
        "sale_distribution" to emptyMap<String, Any?>(),
        "affiliate_campaign_code" to "campaign",
        "device_id" to "device"
    )

    @Test
    fun referralCodeIsOptionalForCachedAttribution() {
        assertNull(GoMarketMeAffiliateMarketingData.fromMap(base)?.referralCode)
        assertEquals(
            "FRIEND20",
            GoMarketMeAffiliateMarketingData.fromMap(base + ("referral_code" to "FRIEND20"))?.referralCode
        )
    }
}
