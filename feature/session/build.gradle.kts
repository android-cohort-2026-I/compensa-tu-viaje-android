plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.compensatuviaje.tracker.feature.session"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
