plugins {
    id("com.android.library")
    kotlin("android")
    id("maven-publish")
}

android {
    namespace = "co.gomarketme.kotlin"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34 // Update based on deprecation warning
        version = "1.0.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

repositories {
    google()
    mavenCentral()
}

tasks.register<Jar>("releaseSourcesJar") {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
    duplicatesStrategy = DuplicatesStrategy.WARN
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("gpr") {
                from(components["release"]) // Access component after evaluation
                groupId = "com.github.GoMarketMe"
                artifactId = "gomarketme-kotlin"
                version = "1.0.4"
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/GoMarketMe/gomarketme-kotlin")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
                    password = project.findProperty("gpr.token") as String? ?: System.getenv("GPR_TOKEN")
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.6.0")
    implementation("com.google.android.material:material:1.7.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.android.billingclient:billing-ktx:5.0.0") // Google Play Billing Library
}
