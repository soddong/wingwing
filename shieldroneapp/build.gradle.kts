// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("org.jetbrains.kotlin.kapt") version "1.8.10" apply false
}

buildscript {
    //val kotlinVersion by extra("1.9.25")
    val hiltVersion by extra("2.52")
    //val lifecycleVersion by extra("2.3.1")

    dependencies {
        //classpath("com.android.tools.build:gradle:8.6.0")
        //classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.google.dagger:hilt-android-gradle-plugin:$hiltVersion")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}