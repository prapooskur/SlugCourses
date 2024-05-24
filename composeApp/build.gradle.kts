import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqlDelight)
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.pras")
            // needed for js
            generateAsync.set(true)
        }
    }
}

compose.experimental { web.application {} }

kotlin {

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    js(IR) {
        browser()
        binaries.executable()
    }

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            binaryOption("bundleId", "com.pras.slugcourses.SlugCoursesiOS")
            isStatic = false
        }
    }

    jvm("desktop")
    
    sourceSets {
        
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.ktor.cio)

            implementation(libs.sqldelight.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.darwin)
            implementation(libs.sqldelight.native)
        }

        val desktopMain by getting
        val jsMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // Multiplatform

            // Navigator
            implementation(libs.voyager.navigator)
            // Screen Model
            implementation(libs.voyager.screenmodel)
            // Transitions
            implementation(libs.voyager.transitions)

            implementation(libs.kermit)

            implementation(libs.supabase.postgrest)

            implementation(libs.ktor.core)
            implementation(libs.ktor.negotiation)
            implementation(libs.ktor.serialization)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            
            implementation(libs.markdown.renderer)
            implementation(libs.markdown.renderer.m3)

        }

        desktopMain.dependencies {
            implementation(libs.ktor.cio)
            implementation(libs.sqldelight.jvm)
            //implementation(compose.desktop.currentOs)
            implementation(compose.desktop.linux_x64)
            implementation(compose.desktop.linux_arm64)
            implementation(compose.desktop.windows_x64)
            implementation(compose.desktop.macos_x64)
            implementation(compose.desktop.macos_arm64)
            implementation(libs.coroutines.swing)
        }

        jsMain.dependencies {
            implementation(libs.ktor.js)
            implementation(libs.sqldelight.js)
            implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.0.2"))
            implementation(npm("sql.js", "1.10.3"))
            implementation(devNpm("copy-webpack-plugin", "9.1.0"))
        }
    }
}

android {
    namespace = "com.pras.slugcourses"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.pras.slugcourses"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "common-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildToolsVersion = "35.0.0 rc4"
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Rpm, TargetFormat.Exe)
            packageName = "com.pras.slugcourses"
            packageVersion = "1.0.0"

            // needed for sqldelight
            modules("java.sql")
        }

        buildTypes.release.proguard {
            // enabling proguard causes ktor to fail?
            isEnabled.set(false)
//            obfuscate.set(true)
            configurationFiles.from(project.file("common-rules.pro"), project.file("desktop-rules.pro"))
        }
    }
}