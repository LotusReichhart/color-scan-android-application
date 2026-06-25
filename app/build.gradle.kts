plugins {
    alias(libs.plugins.android.application)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.lotusreichhart.colorscan"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lotusreichhart.colorscan"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

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
            manifestPlaceholders["admobAppId"] = "ca-app-pub-5834661651760052~2162787575"
            buildConfigField(
                "String",
                "BANNER_AD_ID",
                "\"ca-app-pub-5834661651760052/4273377370\""
            )
            buildConfigField(
                "String",
                "INTERSTITIAL_AD_ID",
                "\"ca-app-pub-5834661651760052/5915585932\""
            )
            buildConfigField(
                "String",
                "APP_OPEN_AD_ID",
                "\"ca-app-pub-5834661651760052/2992009319\""
            )

            isMinifyEnabled = false
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