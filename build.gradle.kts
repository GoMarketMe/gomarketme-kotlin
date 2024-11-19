plugins {
    kotlin("jvm") version "1.9.10"
    id("com.android.library") version "8.7.2" apply false
    kotlin("android") version "1.9.10" apply false
    id("maven-publish")
}

group = "co.gomarketme"
version = "1.0.6"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"]) // Assumes a Java component for JVM-related modules
            groupId = "co.gomarketme"
            artifactId = "kotlin-sdk"
            version = "1.0.6"
        }
    }
    repositories {
        //mavenLocal() // Publish to local Maven repository
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
