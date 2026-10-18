import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

val apiBaseUrl = providers.gradleProperty("API_BASE_URL")
    .getOrElse("https://efficientia-api.onrender.com/")
val aiBaseUrl = providers.gradleProperty("CINTTIA_BASE_URL")
    .getOrElse("https://ia-desktop.onrender.com/")
val localSettings = Properties().apply {
    val settingsFile = rootProject.file("local.properties")
    if (settingsFile.exists()) settingsFile.inputStream().use { load(it) }
}
val mapsApiKey = providers.gradleProperty("MAPS_API_KEY")
    .getOrElse(localSettings.getProperty("MAPS_API_KEY", ""))

android {
    namespace = "com.inter.efficientia_mobile"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.inter.efficientia_mobile"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "CINTTIA_BASE_URL", "\"$aiBaseUrl\"")
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation("com.google.android.gms:play-services-maps:20.0.0")
    implementation("com.google.android.gms:play-services-location:21.4.0")
    implementation("com.google.android.libraries.places:places:5.3.0")
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
