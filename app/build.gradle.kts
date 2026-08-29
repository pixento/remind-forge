import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release identity comes from CI (.github/workflows/release.yml). Unset locally, so a local build
// keeps the placeholder version. Read through providers rather than System.getenv so the
// configuration cache treats them as tracked inputs instead of baking in a stale value.
val releaseVersionCode = providers.environmentVariable("VERSION_CODE").orElse("1").get().toInt()
val releaseVersionName = providers.environmentVariable("VERSION_NAME").orElse("1.0").get()

// Absolute path to the upload keystore, written by CI outside the working tree. Absent locally,
// which leaves the release signing config undefined and the release build unsigned.
val uploadKeystore = providers.environmentVariable("RELEASE_KEYSTORE_PATH").orNull
    ?.let(::file)
    ?.takeIf { it.exists() }

android {
    namespace = "nl.pixento.betterhabits"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "nl.pixento.betterhabits"
        minSdk = 24
        targetSdk = 37
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Only defined when CI supplied a keystore; the release build is unsigned otherwise.
        if (uploadKeystore != null) {
            create("release") {
                storeFile = uploadKeystore
                storePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            // Null on every local build, which is the correct "leave it unsigned" state - nobody
            // needs the upload key on their machine to build the release variant.
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    packaging {
        resources {
            // Duplicate license/notice entries from overlapping test jars; without excluding them
            // the androidTest APK fails to package.
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE.md,LICENSE-notice.md}"
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.core.ktx)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Not used directly - this is a version pin. The Compose test rule drags in espresso-core
    // transitively at 3.5.0, whose event injection calls the InputManager.getInstance method that
    // newer platforms removed, and every Compose test then dies on its first interaction.
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.withType<KotlinCompile>().configureEach {
    // Pin bytecode target independent of the host JDK running Gradle (which may be newer than
    // what Robolectric's bundled ASM version can read during unit tests).
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

tasks.withType<Test>().configureEach {
    // Robolectric's bundled ASM can't parse class files emitted by very new JDKs. Run unit
    // tests on a JDK whose own bootstrap classes it can instrument, regardless of which JDK
    // the Gradle daemon itself happens to be running on.
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    )
}
