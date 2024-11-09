plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jlleitschuh.gradle.ktlint")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.ssafy.shieldroneapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ssafy.shieldroneapp"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.play.services.wearable)
    implementation(platform(libs.androidx.compose.bom))
    
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.tiles)
    implementation(libs.androidx.tiles.material)
    implementation(libs.horologist.compose.tools)
    implementation(libs.horologist.tiles)
    implementation(libs.androidx.watchface.complications.data.source.ktx)
    
    implementation("androidx.wear.compose:compose-material:1.2.1")
    implementation("androidx.wear.compose:compose-foundation:1.2.1")
    implementation("androidx.wear.compose:compose-navigation:1.2.1")
    
    // Material Icons 추가
    implementation("androidx.compose.material:material-icons-core:1.5.4")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.8.4")
    implementation("androidx.work:work-runtime:2.9.1")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    implementation ("androidx.navigation:navigation-compose:2.5.3")
   // Hilt dependency injection
    implementation("com.google.dagger:hilt-android:${rootProject.extra["hiltVersion"]}")
    implementation(libs.androidx.constraintlayout)
    kapt("com.google.dagger:hilt-android-compiler:${rootProject.extra["hiltVersion"]}")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")


 // Wear
    implementation("androidx.wear:wear:1.3.0")

    // Health Services
    implementation("androidx.health:health-services-client:1.1.0-alpha03")


    // Used to bridge between Futures and coroutines
    implementation("com.google.guava:guava:33.3.1-android")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.2.0")

   // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-service:2.8.6")

    // 테스트 관련 의존성
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    wearApp(project(":wear"))
}

// ktlint 설정 추가
ktlint {
    debug.set(false) // 디버그 모드 설정
    android.set(true) // Android 코드 스타일 사용 여부
    outputToConsole.set(true) // 콘솔에 포맷 결과 출력 여부
    ignoreFailures.set(true) // 오류 발생 시 빌드를 실패하지 않도록 설정
}