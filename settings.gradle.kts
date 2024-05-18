pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "com.android") {
                useVersion("8.4.0")  // Assuming this is the latest or intended version
            }
            if (requested.id.namespace == "org.jetbrains.kotlin") {
                useVersion("1.9.10")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GoMarketMeKotlin"
include(":kotlin")
