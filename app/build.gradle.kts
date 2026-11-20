plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signing credentials come from private CI environment, never from the public repository.
val sCallerSigningFile = System.getenv("SCALL_SIGNING_STORE_FILE")
val sCallerSigningPassword = System.getenv("SCALL_SIGNING_STORE_PASSWORD")
val sCallerSigningAlias = System.getenv("SCALL_SIGNING_KEY_ALIAS")
val sCallerSigningKeyPassword = System.getenv("SCALL_SIGNING_KEY_PASSWORD")
val hasSCSigning = listOf(sCallerSigningFile, sCallerSigningPassword,
    sCallerSigningAlias, sCallerSigningKeyPassword).all { !it.isNullOrBlank() }

android {
    namespace = "com.scall.phone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.scall.phone"
        minSdk = 23
        targetSdk = 35
        versionCode = 36
        versionName = "1.9.8"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasSCSigning) {
            create("stableUpgrade") {
                storeFile = file(sCallerSigningFile!!)
                storePassword = sCallerSigningPassword
                keyAlias = sCallerSigningAlias
                keyPassword = sCallerSigningKeyPassword
            }
        }
    }
    buildTypes {
        release {
            if (hasSCSigning) {
                signingConfig = signingConfigs.getByName("stableUpgrade")
            }
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.core:core:1.15.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("junit:junit:4.13.2")
}
