plugins {
    id("com.android.library")
    id("maven-publish")
}

version = "5.0.1"

android {
    namespace = "co.gomarketme.kotlin"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

}


afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("gpr") {
                from(components["release"])
                groupId = "com.github.GoMarketMe"
                artifactId = "gomarketme-kotlin"
                version = project.version.toString()
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
    implementation(files("libs/core-5.0.1.jar"))
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.android.billingclient:billing-ktx:8.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
