import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseKeys = listOf(
    providers.environmentVariable("KEYSTORE_PATH").orNull,
    providers.environmentVariable("KEYSTORE_PASSWORD").orNull,
    providers.environmentVariable("KEY_ALIAS").orNull,
    providers.environmentVariable("KEY_PASSWORD").orNull
)
val hasAnyReleaseKey = releaseKeys.any { !it.isNullOrBlank() }
val hasAllReleaseKeys = releaseKeys.all { !it.isNullOrBlank() }
val releaseTaskRequested = gradle.startParameter.taskNames.any {
    it.contains("assembleRelease", ignoreCase = true) ||
        it.contains("bundleRelease", ignoreCase = true)
}
if (hasAnyReleaseKey && !hasAllReleaseKeys) {
    throw GradleException("Release signing configuration is incomplete.")
}
if (releaseTaskRequested && !hasAllReleaseKeys) {
    throw GradleException("Release tasks require all signing environment variables.")
}

android {
    namespace = "com.erikraft.soundstickers"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.erikraft.soundstickers"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasAllReleaseKeys) {
                storeFile = file(releaseKeys[0]!!)
                storePassword = releaseKeys[1]
                keyAlias = releaseKeys[2]
                keyPassword = releaseKeys[3]
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasAllReleaseKeys) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.webkit:webkit:1.18.0")
    implementation("com.google.android.material:material:1.13.0")
    testImplementation("junit:junit:4.13.2")
}
