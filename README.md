<div align="center">
	<img src="https://static.gomarketme.net/assets/gmm-icon.png" alt="GoMarketMe"/>
	<br>
    <h1>GoMarketMe Kotlin SDK</h1>
	<p>Affiliate marketing for Android apps.</p>
</div>

## Installation

Add JitPack and Maven Central to your project repositories:

```kotlin
repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}
```

Add GoMarketMe to your app dependencies:

```kotlin
dependencies {
    implementation("com.github.GoMarketMe:gomarketme-kotlin:5.0.0")
}
```

## Usage

GoMarketMe takes only a few lines to set up.

### Step 1/2: Initialize

To initialize GoMarketMe, import the SDK and initialize it with your API key:

```kotlin
import co.gomarketme.kotlin.GoMarketMe

private val goMarketMeSDK = GoMarketMe

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        goMarketMeSDK.initialize(this, "API_KEY")
    }
}
```

Replace `API_KEY` with your actual GoMarketMe API key. You can find it on the product onboarding page and under **Profile > API Key**.

### Alternative Step 1/2: Programmatic Affiliate Marketing

For apps that want to customize the user experience based on affiliate attribution, initialize GoMarketMe and read affiliate marketing data after initialization.

This enables [Programmatic Affiliate Marketing](https://gomarketme.co/programmatic-affiliate-marketing/), including affiliate-aware paywalls, personalized onboarding, promotions, and custom in-app experiences.

```kotlin
import co.gomarketme.kotlin.GoMarketMe
import co.gomarketme.kotlin.GoMarketMeAffiliateMarketingData

private val goMarketMeSDK = GoMarketMe

class MainActivity : AppCompatActivity() {
    private var affiliateData: GoMarketMeAffiliateMarketingData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        goMarketMeSDK.initialize(this, "API_KEY")

        val data = goMarketMeSDK.affiliateMarketingData
        if (data != null) {
            // maps to GoMarketMe > Affiliates > Export > id column
            println("Affiliate ID: ${data.affiliate.id}")

            // maps to GoMarketMe > Campaigns > [Name] > Affiliate's Revenue Split (%)
            println("Affiliate %: ${data.saleDistribution.affiliatePercentage}")
            
            // maps to GoMarketMe > Campaigns > [Name] > id in the URL
            println("Campaign ID: ${data.campaign.id}")

            // Use this data to customize onboarding, paywalls, promotions, or in-app experiences.
            affiliateData = data
        }
    }
}
```

`goMarketMeSDK.initialize(...)` runs asynchronously. If you need to read `affiliateMarketingData`, wait until the SDK has finished preparing attribution before using the value.

### Step 2/2: Sync after purchase

After your app completes a purchase through Google Play Billing, RevenueCat, Adapty, or another in-app purchase provider, call:

```kotlin
lifecycleScope.launch {
    goMarketMeSDK.syncAllTransactions()
}
```

If your purchase library lets you decide when to acknowledge, consume, or complete the transaction, call `syncAllTransactions()` first.

```kotlin
private fun handlePurchase(purchase: Purchase) {
    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
        lifecycleScope.launch {
            goMarketMeSDK.syncAllTransactions()

            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.consumeAsync(consumeParams) { billingResult, _ ->
                // Continue your purchase-completion flow.
            }
        }
    }
}
```

That's it. GoMarketMe automatically attributes and reports affiliate sales.

## Platform Support

GoMarketMe supports native Android apps using Kotlin.

This SDK is designed for Android apps that sell in-app purchases or subscriptions through Google Play Billing, either directly or through a compatible in-app purchase provider.

## IAP Provider Compatibility

GoMarketMe works with any in-app purchase provider that ultimately completes purchases through Google Play Billing, including:

- Google Play Billing
- RevenueCat
- Adapty
- Qonversion
- Superwall
- Glassfy
- Custom purchase flows built on Google Play Billing

## Support

Check out our sample Android app at [https://github.com/GoMarketMe/gomarketme-kotlin-sample-app](https://github.com/GoMarketMe/gomarketme-kotlin-sample-app).

If you run into any issues, please reach out to us at [integrations@gomarketme.co](mailto:integrations@gomarketme.co) or visit [https://gomarketme.co](https://gomarketme.co).
