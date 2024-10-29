plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jlleitschuh.gradle.ktlint")
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
    // Google Play Services의 Wearable API 사용을 위한 라이브러리
    implementation(libs.play.services.wearable)

    // Compose 관련 BOM, Compose 라이브러리 버전 통일
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI의 기본 UI 컴포넌트 라이브러리
    implementation(libs.androidx.ui)

    // Compose UI의 미리보기 도구
    implementation(libs.androidx.ui.tooling.preview)

    // Compose Material 디자인 컴포넌트 라이브러리
    implementation(libs.androidx.compose.material)

    // Compose의 기본적인 Foundation 컴포넌트 제공 라이브러리
    implementation(libs.androidx.compose.foundation)

    // Wear OS용 미리보기 도구 제공 라이브러리
    implementation(libs.androidx.wear.tooling.preview)

    // Compose Activity를 위한 라이브러리
    implementation(libs.androidx.activity.compose)

    // Android 12 이상의 앱에서 스플래시 스크린을 쉽게 적용할 수 있는 라이브러리
    implementation(libs.androidx.core.splashscreen)

    // Android Wear OS 타일(Tiles) 기능을 위한 라이브러리
    implementation(libs.androidx.tiles)

    // Material 디자인 기반의 Wear OS Tiles UI 컴포넌트를 제공하는 라이브러리
    implementation(libs.androidx.tiles.material)

    // Horologist 라이브러리: Wear OS에서 Compose를 위한 도구 라이브러리
    implementation(libs.horologist.compose.tools)

    // Horologist 라이브러리: Wear OS에서 Tiles를 위한 도구 라이브러리
    implementation(libs.horologist.tiles)

    // Wear OS의 복잡도(Complications) 데이터 소스를 쉽게 관리할 수 있도록 지원하는 라이브러리
    implementation(libs.androidx.watchface.complications.data.source.ktx)

    // Android 테스트에서 Compose BOM을 사용
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // JUnit4를 위한 Compose UI 테스트 라이브러리
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // 디버그 모드에서 UI 도구를 위한 라이브러리 (UI 미리보기 및 개발 편의)
    debugImplementation(libs.androidx.ui.tooling)

    // Compose 테스트에서 AndroidManifest.xml 파일을 위한 라이브러리
    debugImplementation(libs.androidx.ui.test.manifest)

    // Wear OS 앱을 위한 모듈 참조
    wearApp(project(":wear"))
}

// ktlint 설정 추가
ktlint {
    debug.set(false) // 디버그 모드 설정
    android.set(true) // Android 코드 스타일 사용 여부
    outputToConsole.set(true) // 콘솔에 포맷 결과 출력 여부
    ignoreFailures.set(true) // 오류 발생 시 빌드를 실패하지 않도록 설정
}