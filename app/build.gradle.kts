plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val aiGatewayUrl = providers.gradleProperty("AI_GATEWAY_URL").orElse("").get()
val escapedAiGatewayUrl = aiGatewayUrl.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "ru.apphelper"
    compileSdk = 37

    defaultConfig {
        applicationId = "ru.apphelper"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "AI_GATEWAY_URL", "\"$escapedAiGatewayUrl\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("com.google.android.gms:play-services-location:21.4.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
