import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.devtools.ksp")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val releaseAdmobAppId = localProperties.getProperty("release.admob.app.id") ?: "ca-app-pub-3940256099942544~3347511713"
val releaseBannerAdId = localProperties.getProperty("release.banner.ad.id") ?: "ca-app-pub-3940256099942544/6300978111"
val releaseInterstitialAdId = localProperties.getProperty("release.interstitial.ad.id") ?: "ca-app-pub-3940256099942544/1033173712"
val releaseAppOpenAdId = localProperties.getProperty("release.app-open.ad.id") ?: "ca-app-pub-3940256099942544/9257395921"
val privacyPolicyUrl = localProperties.getProperty("privacy.policy.url") ?: "https://www.google.com"
val termsOfServiceUrl = localProperties.getProperty("terms.of.service.url") ?: "https://www.google.com"

android {
    namespace = "com.lotusreichhart.colorscan"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lotusreichhart.colorscan"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "BANNER_AD_ID",
            "\"ca-app-pub-3940256099942544/6300978111\""
        )
        buildConfigField(
            "String",
            "INTERSTITIAL_AD_ID",
            "\"ca-app-pub-3940256099942544/1033173712\""
        )
        buildConfigField(
            "String",
            "APP_OPEN_AD_ID",
            "\"ca-app-pub-3940256099942544/9257395921\""
        )
        buildConfigField(
            "String",
            "PRIVACY_POLICY_URL",
            "\"$privacyPolicyUrl\""
        )
        buildConfigField(
            "String",
            "TERMS_OF_SERVICE_URL",
            "\"$termsOfServiceUrl\""
        )
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField(
                "String",
                "BANNER_AD_ID",
                "\"ca-app-pub-3940256099942544/6300978111\""
            )
            buildConfigField(
                "String",
                "INTERSTITIAL_AD_ID",
                "\"ca-app-pub-3940256099942544/1033173712\""
            )
            buildConfigField(
                "String",
                "APP_OPEN_AD_ID",
                "\"ca-app-pub-3940256099942544/9257395921\""
            )
        }
        create("closedTest") {
            initWith(getByName("release"))
            matchingFallbacks.add("release")
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField(
                "String",
                "BANNER_AD_ID",
                "\"ca-app-pub-3940256099942544/6300978111\""
            )
            buildConfigField(
                "String",
                "INTERSTITIAL_AD_ID",
                "\"ca-app-pub-3940256099942544/1033173712\""
            )
            buildConfigField(
                "String",
                "APP_OPEN_AD_ID",
                "\"ca-app-pub-3940256099942544/9257395921\""
            )
        }
        release {
            manifestPlaceholders["admobAppId"] = releaseAdmobAppId
            buildConfigField(
                "String",
                "BANNER_AD_ID",
                "\"$releaseBannerAdId\""
            )
            buildConfigField(
                "String",
                "INTERSTITIAL_AD_ID",
                "\"$releaseInterstitialAdId\""
            )
            buildConfigField(
                "String",
                "APP_OPEN_AD_ID",
                "\"$releaseAppOpenAdId\""
            )

            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.material)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.play.services.ads)
    implementation("com.android.billingclient:billing-ktx:6.2.1")
    implementation(libs.gson)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.timber)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}