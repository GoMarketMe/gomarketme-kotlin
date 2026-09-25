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
    implementation("com.github.GoMarketMe:gomarketme-kotlin:6.0.0")
}
```

## Usage

GoMarketMe takes only a few lines to set up.

### Step 1: Initialize

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

Replace `API_KEY` with your actual GoMarketMe API key. You can find it during onboarding or in **Profile > [API Key](https://gomarketme.net/marketer/profile/#account-settings)**.

### Step 2: Sync Purchases (recommended)

GoMarketMe automatically detects and reports purchases. For additional reliability, we also recommend manually syncing after Google Play Billing, RevenueCat, Adapty, or another provider confirms a successful purchase:

```kotlin
lifecycleScope.launch {
    goMarketMeSDK.syncAllTransactions()
}
```

Call it before acknowledging or consuming the purchase when your purchase library controls that step.

## Optional

### Step 3: Referral Codes

Referral codes work alongside affiliate links when a link isn't practical, such as in conversations, podcasts, videos, events, or print.

Enable Referral Codes in one line:

```kotlin
GoMarketMeReferralCodeTrigger()
```

**Placement:** Put this referral UI on the first screen users see after installing the app, ideally during onboarding or immediately afterward.

Customize its text, colors, typography, and layout directly in [https://gomarketme.net/marketer/settings#referral-codes](https://gomarketme.net/marketer/settings#referral-codes).

Learn more about [Referral Codes](https://gomarketme.co/referral-codes/).

### Step 4: Programmatic Affiliate Marketing

Programmatic Affiliate Marketing lets your app personalize the user experience based on the affiliate and campaign that referred the user. For example, you can customize onboarding, paywalls, offers, or in-app content.

After initialization finishes, read `affiliateMarketingData`:

```kotlin
GoMarketMe.affiliateMarketingData?.let { data ->
    println("Affiliate ID: ${data.affiliate.id}")
    println("Affiliate %: ${data.saleDistribution.affiliatePercentage}")
    println("Campaign ID: ${data.campaign.id}")
}
```

Learn more about [Programmatic Affiliate Marketing](https://gomarketme.co/programmatic-affiliate-marketing/).

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
