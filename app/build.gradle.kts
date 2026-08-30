import com.android.build.api.artifact.SingleArtifact
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
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

    // Renders the Play listing graphics on the JVM - see the screenshots package under src/test.
    // The Roborazzi *Gradle plugin* is deliberately not applied: all it adds is golden-image
    // record/verify/compare tasks we don't want, and its AGP 9 support was written against
    // 9.0.0-rc02, well behind the AGP here. The library touches no Gradle API - the system
    // properties set further down are the whole contract with the build.
    // Note this drags espresso-core 3.5.1 onto the *unit test* runtime classpath, below the 3.7.0
    // pin in androidTest below. Nothing under Robolectric injects input events, so the
    // InputManager.getInstance call that pin exists for is never reached; don't "align" the two.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.roborazzi.compose)

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

// Play listing graphics. The renderers in src/test/.../screenshots draw the app's own composables
// through Roborazzi, which only writes files while roborazzi.test.record is set - so a plain
// testDebugUnitTest neither runs them nor drops PNGs into build/outputs. Gated on a Gradle property
// rather than a second Test task, so the render inherits the unit-test task's classpath, merged
// resources and JDK pin exactly.
val recordStoreGraphics =
    providers.gradleProperty("recordStoreGraphics").map(String::toBoolean).orElse(false)
val storeGraphicsOutputDir = layout.buildDirectory.dir("outputs/store-graphics")
val storeGraphicsCopyDir = layout.projectDirectory.dir("../distribution/screenshots")
val whatsNewDir = layout.projectDirectory.dir("../distribution/whatsnew")
val storeListingsDir = layout.projectDirectory.dir("../distribution/listings")

tasks.withType<Test>().configureEach {
    val recording = recordStoreGraphics.get()
    systemProperty("roborazzi.test.record", recording.toString())
    // Absolute paths on purpose: Roborazzi resolves a relative capture path against the test JVM's
    // working directory, and the store copy lives outside this module either way.
    systemProperty("storeGraphics.outputDir", storeGraphicsOutputDir.get().asFile.absolutePath)
    systemProperty("storeGraphics.copyDir", storeGraphicsCopyDir.asFile.absolutePath)
    // The Play release notes, read by WhatsNewFilesTest. Declared as an input as well as handed
    // over as a path: they're outside the module's own source, so without this an edit to a
    // whatsnew file would leave the test task UP-TO-DATE and the check unrun.
    systemProperty("whatsNew.dir", whatsNewDir.asFile.absolutePath)
    inputs.dir(whatsNewDir)
        .withPropertyName("whatsNewFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    // The Play listing copy, read by ListingFilesTest, declared as an input for the same reason.
    systemProperty("listings.dir", storeListingsDir.asFile.absolutePath)
    inputs.dir(storeListingsDir)
        .withPropertyName("storeListingFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    if (recording) {
        filter { includeTestsMatching("nl.pixento.betterhabits.screenshots.*") }
        // The PNGs are the point of the run and aren't declared task outputs, so neither an
        // up-to-date check nor a build-cache hit may skip it. cacheIf is the one that's easy to
        // forget: upToDateWhen alone still allows a from-cache hit under org.gradle.caching=true.
        outputs.upToDateWhen { false }
        outputs.cacheIf { false }
    } else {
        filter { excludeTestsMatching("nl.pixento.betterhabits.screenshots.*") }
    }
}

// Native debug symbols for Play. Every AAB upload otherwise warns that the bundle contains native
// code with no debug symbols. The only native code here is prebuilt AndroidX: graphics-path (via
// Compose ui-graphics) and datastore's shared counter, four ABIs each. Both are published *already
// stripped* - their pre-strip form under intermediates/merged_native_libs is byte-identical to the
// stripped one, and carries no .symtab or .debug_* section, only the exported .dynsym.
//
// That is why `ndk { debugSymbolLevel = ... }`, the fix Play's warning links to, is deliberately
// NOT set: AGP's ExtractNativeDebugMetadataTask skips any .so whose merged input has the same size
// as its stripped output ("already been stripped"), and the merge task that would write
// BUNDLE-METADATA is @SkipWhenEmpty - so it produces nothing, leaves the warning in place, and only
// adds an NDK to the build's requirements. Repackage the shipped .so as the symbol file instead:
// their exported symbol table is genuinely all the symbol information that exists for them.
abstract class PackageNativeDebugSymbols : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val bundle: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun packageSymbols() {
        val aab = bundle.get().asFile
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        ZipFile(aab).use { bundleZip ->
            val nativeLibs = bundleZip.entries()
                .asSequence()
                .filter { !it.isDirectory && NATIVE_LIB.matches(it.name) }
                .sortedBy { it.name }
                .toList()
            if (nativeLibs.isEmpty()) {
                throw GradleException(
                    "No native libraries in ${aab.name}, so Play has nothing left to warn about - " +
                        "drop this task and its wiring in .github/workflows/release.yml."
                )
            }
            ZipOutputStream(output.outputStream().buffered()).use { symbolsZip ->
                nativeLibs.forEach { entry ->
                    val (abi, name) = NATIVE_LIB.find(entry.name)!!.destructured
                    // Pinned local date-time rather than an mtime: the default would be "now", and
                    // ZipEntry.setTime would additionally read it back through the default time
                    // zone, so two builds of the same bundle would not produce the same zip.
                    symbolsZip.putNextEntry(
                        ZipEntry("$abi/$name.sym").apply { setTimeLocal(DOS_EPOCH) }
                    )
                    bundleZip.getInputStream(entry).use { it.copyTo(symbolsZip) }
                    symbolsZip.closeEntry()
                }
            }
        }
    }

    private companion object {
        /** How a bundle lays out the base module's packaged native libraries. */
        val NATIVE_LIB = Regex("""base/lib/([^/]+)/([^/]+\.so)""")
        val DOS_EPOCH: LocalDateTime = LocalDateTime.of(1980, 1, 1, 0, 0)
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        tasks.register<PackageNativeDebugSymbols>("packageReleaseNativeDebugSymbols") {
            description = "Packages the release bundle's native libraries as a Play symbols zip."
            // The public bundle artifact, so this can only ever describe what actually shipped -
            // and depending on it is what makes the task build the bundle first.
            bundle.set(variant.artifacts.get(SingleArtifact.BUNDLE))
            outputFile.set(
                layout.buildDirectory.file(
                    "outputs/native-debug-symbols/release/native-debug-symbols.zip"
                )
            )
        }
    }
}
