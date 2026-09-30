plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ultimatetool.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ultimatetool.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // Базовый URL бэкенда (авторизация, сообщество). Пусто — работает демо-режим.
        buildConfigField("String", "BACKEND_BASE_URL", "\"https://api.ultimatetool.example/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE.md",
            "META-INF/NOTICE.md"
        )
    }

    // Машина сборки ограничена ~2 ГБ RAM: режем кучу тестовым JVM.
    testOptions {
        unitTests.all { it.maxHeapSize = "384m" }
    }
}

dependencies {
    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Jetpack Compose (Material 3)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Persistence
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Security
    implementation(libs.security.crypto)
    implementation(libs.biometric)

    // Async / images
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)

    // Plugin engine + утилиты
    implementation(libs.luaj)          // Lua-интерпретатор (песочница плагинов)
    implementation(libs.zxing.core)     // Генерация QR-кодов
    implementation(libs.markwon.core)   // Markdown в AI-чате
    implementation(libs.xz)             // Распаковка .tar.xz (rootfs Linux)

    // Tests
    testImplementation(libs.junit)
}
