plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---------------------------------------------------------------------------
// 版本：只用语义化版本，不要日期序号。
//   本地默认 0.4.3 / 403；CI 从 tag（v0.3.11）推导后通过 -PomVersionName/-PomVersionCode 传入。
//   versionCode = major*10000 + minor*100 + patch（0.4.3 → 403）。
// ---------------------------------------------------------------------------
fun envValue(name: String): String? = System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }

val omVersionName: String =
    (findProperty("omVersionName") as String?) ?: envValue("OM_VERSION_NAME") ?: "0.4.3"
val omVersionCode: Int =
    ((findProperty("omVersionCode") as String?) ?: envValue("OM_VERSION_CODE") ?: "403").toInt()

// ---------------------------------------------------------------------------
// release 签名：凭据一律走环境变量，keystore 放在仓库外。
// ---------------------------------------------------------------------------
val omStoreFile = envValue("OM_KEYSTORE")
val omKeyAlias = envValue("OM_KEY_ALIAS")
val omKeyPass = envValue("OM_KEY_PASS")
val omHasSigning = listOf(omStoreFile, omKeyAlias, omKeyPass).all { it != null }
val omRequireSigning = envValue("OM_REQUIRE_SIGNING") == "1"

if (omRequireSigning && !omHasSigning) {
    val missing = listOf(
        "OM_KEYSTORE" to omStoreFile,
        "OM_KEY_ALIAS" to omKeyAlias,
        "OM_KEY_PASS" to omKeyPass,
    ).filter { it.second == null }.map { it.first }
    throw GradleException("缺签名凭据：${missing.joinToString()}（CI 发版不允许未签名 release）")
}

// 独立 APK 的薄壳：只留 MainActivity、启动画面和分享入口。
// 全部界面与数据层在 :client（Adia 也依赖它）。
android {
    namespace = "com.a2heng.opencode.mobile.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.a2heng.opencode.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = omVersionCode
        versionName = omVersionName
    }

    signingConfigs {
        if (omHasSigning) {
            create("release") {
                storeFile = rootProject.file(omStoreFile!!)
                keyAlias = omKeyAlias
                storePassword = omKeyPass
                keyPassword = omKeyPass
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (omHasSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "OpenCode Dev")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(project(":client"))

    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
