<div align="center">
	<img src="https://static.gomarketme.net/assets/gmm-icon.png" alt="GoMarketMe"/>
	<br>
    <h1>gomarketme-kotlin</h1>
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
    implementation("com.github.GoMarketMe:gomarketme-kotlin:4.0.1")
}
```

## Usage

### ⚙️ Basic Integration

To initialize GoMarketMe, import the SDK and initialize it with your API key:

```kotlin
import co.gomarketme.kotlin.GoMarketMe

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GoMarketMe.initialize(this, "API_KEY")
    }
}
```

No further steps needed. The SDK automatically attributes and reports your affiliate sales in real time.

### ⚙️ OR - Advanced Integration ([Programmatic Affiliate Marketing](https://gomarketme.co/programmatic-affiliate-marketing/))

Use this approach for more advanced scenarios, such as:

* Affiliate-aware paywalls: Offer exclusive pricing or promotions to users acquired through affiliate campaigns.
* Personalized onboarding: For example, a social or fitness app can automatically make new users follow the influencer who referred them, strengthening engagement and maximizing the affiliate's impact.

```kotlin
import co.gomarketme.kotlin.GoMarketMe
import co.gomarketme.kotlin.GoMarketMeAffiliateMarketingData

class MainActivity : AppCompatActivity() {
    private var affiliateData: GoMarketMeAffiliateMarketingData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GoMarketMe.initialize(this, "API_KEY")

        val data = GoMarketMe.affiliateMarketingData
        if (data != null) { // user acquired through affiliate campaign
            println("Affiliate ID: ${data.affiliate.id}")                         // maps to GoMarketMe > Affiliates > Export > id column
            println("Affiliate %: ${data.saleDistribution.affiliatePercentage}")  // maps to GoMarketMe > Campaigns > [Name] > Affiliate's Revenue Split (%)
            println("Campaign ID: ${data.campaign.id}")                           // maps to GoMarketMe > Campaigns > [Name] > id in the URL

            affiliateData = data
        }
    }
}
```

`GoMarketMe.initialize(...)` runs asynchronously. If you need to read `affiliateMarketingData` immediately after initialization completes, wait until the SDK has finished posting system info before using the value.

Make sure to replace `API_KEY` with your actual GoMarketMe API key. You can find it on the product onboarding page and under **Profile > API Key**.

## Support

Check out our sample Android app at [https://github.com/GoMarketMe/gomarketme-kotlin-sample-app](https://github.com/GoMarketMe/gomarketme-kotlin-sample-app).

If you run into any issues, please reach out to us at [integrations@gomarketme.co](mailto:integrations@gomarketme.co) or visit [https://gomarketme.co](https://gomarketme.co).
