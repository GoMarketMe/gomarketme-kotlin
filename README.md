<div align="center">
	<img src="https://static.gomarketme.net/assets/gmm-icon.png" alt="GoMarketMe"/>
	<br>
	<h1>GoMarketMe Kotlin</h1>
	<p>Affiliate Marketing for Android Applications.</p>
	<br>
	<br>
</div>

## 📦 Setup

1. Add the following to your `build.gradle.kts` file:

```kotlin
repositories {
    mavenCentral()
	maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.GoMarketMe:gomarketme-kotlin:1.1.1")
}
```

## 🧑‍💻 Usage

### ⚡️ Initialize Client

Create an instance of `GoMarketMe` client:

```kotlin
override fun onCreate() {
    super.onCreate()    
    GoMarketMe.initialize(this, "API_KEY")
}
```

Make sure to replace API_KEY with your actual GoMarketMe API key. You can find it on the product onboarding page and under Profile > API Key.

Check out our <a href="https://github.com/GoMarketMe/gomarketme-kotlin-sample-app" target="_blank">sample app</a> for an example.
