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

//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }

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
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    wearApp(project(":wear"))
}