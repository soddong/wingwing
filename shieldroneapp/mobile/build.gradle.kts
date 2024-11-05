plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("kapt")  // KAPT 플러그인 적용
    id("com.google.dagger.hilt.android")  // Hilt 플러그인 적용
}

android {
    namespace = "com.ssafy.shieldroneapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ssafy.shieldroneapp"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            // 디버그 빌드용 API URL
            buildConfigField("String", "BASE_API_URL", "\"https://debug-api.example.com/\"")
            isMinifyEnabled = false
        }
        release {
            // 릴리스 빌드용 API URL
            buildConfigField("String", "BASE_API_URL", "\"https://api.example.com/\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Jetpack Compose 설정 추가
    buildFeatures {
        compose = true // Compose 활성화
        buildConfig = true // BuildConfig 활성화
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0" // Kotlin Compiler Extension 버전
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.wearable)
    implementation(libs.material)
    implementation(libs.androidx.activity)

    // Jetpack Compose 의존성 추가
    implementation(libs.androidx.activity.compose)
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation Compose 라이브러리 추가
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // 테스트 관련 의존성 추가
    testImplementation(libs.junit) // 단위 테스트를 위한 기본 라이브러리
    androidTestImplementation(libs.androidx.junit) // 안드로이드 테스트용 JUnit
    androidTestImplementation(libs.androidx.espresso.core) // 안드로이드 테스트용 JUnit
    androidTestImplementation("androidx.compose.ui:ui-test-junit4") // Compose UI 테스트용


    // Wearable 관련 설정
    wearApp(project(":wear"))
}