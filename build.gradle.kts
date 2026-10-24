buildscript {
    dependencies {
        // AGP 9 built-in Kotlin ships an older KGP. Upgrade to the approved Kotlin/KSP pins
        // without applying org.jetbrains.kotlin.android or kotlin-kapt.
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.ksp.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
}
