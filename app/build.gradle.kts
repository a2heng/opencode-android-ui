plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// ---------------------------------------------------------------------------
// 版本：只用语义化版本，不要日期序号。
//   本地默认 0.1.3 / 103；CI 从 tag（v0.1.3）推导后通过 -PomVersionName/-PomVersionCode 传入。
//   versionCode = major*10000 + minor*100 + patch（0.1.3 → 103），保证单调递增。
// ---------------------------------------------------------------------------
fun envValue(name: String): String? = System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }

val omVersionName: String =
    (findProperty("omVersionName") as String?) ?: envValue("OM_VERSION_NAME") ?: "0.3.5"
val omVersionCode: Int =
    ((findProperty("omVersionCode") as String?) ?: envValue("OM_VERSION_CODE") ?: "305").toInt()

// ---------------------------------------------------------------------------
// release 签名：凭据一律走环境变量，keystore 放在仓库外。
//   OM_KEYSTORE    keystore 绝对路径（CI 里 base64 还原到 $RUNNER_TEMP）
//   OM_KEY_ALIAS   密钥别名
//   OM_KEY_PASS    store/key 共用口令
//   OM_REQUIRE_SIGNING=1 时缺凭据直接失败（CI 发版必须签名）
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

android {
    namespace = "com.a2heng.opencode.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.a2heng.opencode.mobile"
        minSdk = 26
        targetSdk = 34
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
                // minSdk 26 → 不需要 v1；v2/v3 显式打开
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
            // 与正式包共存：不同 applicationId + 不同签名
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "OpenCode Dev")
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

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
