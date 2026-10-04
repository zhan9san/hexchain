plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The repository root holds the shared data (docs/android.md A-27).
val repoRoot: File = rootDir.parentFile

android {
    namespace = "com.zhan9san.hexchain"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.zhan9san.hexchain"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("appVersionCode").get().toInt()
        versionName = providers.gradleProperty("appVersionName").get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing (A-32): CI provides the keystore through environment variables.
    // The key alias is "hexchain" and the key password equals the keystore password
    // (scripts/create-release-key.sh).
    val keystore = System.getenv("KEYSTORE_FILE")
    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "hexchain"
                keyPassword = System.getenv("KEY_PASSWORD") ?: storePassword
            }
        }
    }

    buildTypes {
        // A-48: debug builds install next to the release app instead of conflicting with it.
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main").assets.srcDir(repoRoot.resolve("data"))
        getByName("androidTest").assets.srcDir(repoRoot.resolve("tests"))
    }
}

// JVM unit tests read data/grid.json and tests/scenarios.json from the repository.
tasks.withType<Test>().configureEach {
    systemProperty("repoRoot", repoRoot.absolutePath)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
