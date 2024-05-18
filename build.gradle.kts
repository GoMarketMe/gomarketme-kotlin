plugins {
    id("com.android.application") version "8.4.0" apply false
    kotlin("android") version "1.9.10" apply false
}

allprojects {
    repositories {
        // These are now unnecessary due to settings.gradle.kts central configuration
    }
}
