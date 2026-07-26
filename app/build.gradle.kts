plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseKeystoreFile = providers.environmentVariable(
    "RUNE_COMPANION_KEYSTORE_FILE",
).orNull
val releaseKeystorePassword = providers.environmentVariable(
    "RUNE_COMPANION_KEYSTORE_PASSWORD",
).orNull
val releaseKeyAlias = providers.environmentVariable(
    "RUNE_COMPANION_KEY_ALIAS",
).orNull
val releaseKeyPassword = providers.environmentVariable(
    "RUNE_COMPANION_KEY_PASSWORD",
).orNull
val releaseSigningAvailable = listOf(
    releaseKeystoreFile,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

providers.gradleProperty("runeCompanionBuildDir").orNull?.let { externalBuildDirectory ->
    layout.buildDirectory.set(file(externalBuildDirectory))
}

android {
    namespace = "io.github.taxledgr.runecompanion"
    compileSdk = 35
    testBuildType = "qualityTest"

    defaultConfig {
        applicationId = "io.github.taxledgr.runecompanion"
        minSdk = 30
        targetSdk = 35
        versionCode = 34
        versionName = "1.10.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (releaseSigningAvailable) {
            create("release") {
                storeFile = file(requireNotNull(releaseKeystoreFile))
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("qualityTest") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".qualitytest"
            versionNameSuffix = "-qualitytest"
            matchingFallbacks += listOf("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    // WorkManager 2.11+ uses Kotlin 2.1 metadata; 2.10 remains compatible with
    // this project's Kotlin 1.9 and Compose compiler toolchain.
    implementation("androidx.work:work-runtime-ktx:2.10.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    add("qualityTestImplementation", composeBom)
    add("qualityTestImplementation", "androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

tasks.register("securityCheck") {
    group = "verification"
    description = "Checks security-critical Android configuration and forbidden APIs."

    val manifest = file("src/main/AndroidManifest.xml")
    val backupRules = file("src/main/res/xml/backup_rules.xml")
    val extractionRules = file("src/main/res/xml/data_extraction_rules.xml")
    val sources = fileTree("src/main/java") { include("**/*.kt") }
    inputs.files(manifest, backupRules, extractionRules, sources)

    doLast {
        val manifestText = manifest.readText()
        check("""android:usesCleartextTraffic="false"""" in manifestText)
        check("""android:networkSecurityConfig="@xml/network_security_config"""" in manifestText)
        check("""android:name=".overlay.OverlayService"""" in manifestText)
        check("""android:exported="false"""" in manifestText)
        check("""android.permission.HIDE_OVERLAY_WINDOWS""" in manifestText)
        check("""path="."""" !in backupRules.readText())
        check("""path="."""" !in extractionRules.readText())

        val sourceText = sources.files.joinToString("\n") { it.readText() }
        listOf(
            "javaScriptEnabled = true",
            "addJavascriptInterface(",
            "HostnameVerifier",
            "X509TrustManager",
        ).forEach { forbidden ->
            check(forbidden !in sourceText) {
                "Forbidden security-sensitive API found: $forbidden"
            }
        }
    }
}
