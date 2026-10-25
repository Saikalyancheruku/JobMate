plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

val baseSemVer = rootProject.file("VERSION").readText().trim()
require(Regex("^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$").matches(baseSemVer)) {
    "VERSION must contain a stable MAJOR.MINOR.PATCH SemVer value, for example 0.1.0."
}
val ciRunNumber = providers.environmentVariable("GITHUB_RUN_NUMBER").orNull?.toLongOrNull()
val ciRunAttempt = providers.environmentVariable("GITHUB_RUN_ATTEMPT").orNull?.toLongOrNull() ?: 1L
require(ciRunAttempt in 1L..999L) { "GITHUB_RUN_ATTEMPT must be between 1 and 999 for monotonic Android version codes." }
val defaultJobMateVersionName = if (ciRunNumber == null) {
    "$baseSemVer+local.${System.currentTimeMillis()}"
} else {
    "$baseSemVer+ci.$ciRunNumber.$ciRunAttempt"
}
val defaultJobMateVersionCode = if (ciRunNumber == null) {
    1L
} else {
    1_000_000L + ciRunNumber * 1_000L + ciRunAttempt
}
val jobMateVersionName = providers.gradleProperty("jobmateVersionName").orElse(defaultJobMateVersionName).get()
val jobMateVersionCode = providers.gradleProperty("jobmateVersionCode").orElse(defaultJobMateVersionCode.toString()).get().toLongOrNull()
    ?: error("jobmateVersionCode must be an integer.")
require(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+\\+[0-9A-Za-z.-]+$").matches(jobMateVersionName)) {
    "JobMate versionName must be SemVer with build metadata, for example 0.1.0+ci.12.1."
}
require(jobMateVersionCode in 1L..2_100_000_000L) { "Calculated Android versionCode is outside Android's supported range." }
logger.lifecycle("JobMate version: $jobMateVersionName (versionCode $jobMateVersionCode)")

android {
    namespace = "com.saikalyan.jobmate"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.saikalyan.jobmate"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = jobMateVersionCode.toInt()
        versionName = jobMateVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += setOf("META-INF/NOTICE.md", "META-INF/LICENSE.md")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.jvmArgs(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.security=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            )
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.android.mail)
    implementation(libs.android.activation)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.play.services.auth)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
}



