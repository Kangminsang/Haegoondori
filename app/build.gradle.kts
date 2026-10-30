import java.util.Properties

// app 모듈: Android(Compose + Hilt + Room). core에 의존한다.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.kangminsang.hagoondori"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kangminsang.hagoondori"
        minSdk = 33
        targetSdk = 35
        versionCode = 4
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 릴리스 서명은 환경변수가 모두 있을 때만 구성한다 (비밀 값은 저장소에 두지 않는다).
    val releaseKeystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
    val releaseKeystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
    val releaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")
    if (!releaseKeystorePath.isNullOrBlank() &&
        !releaseKeystorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()
    ) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    // CI에서 디버그 키를 고정해 PC 빌드와 서로 덮어 설치할 수 있게 한다. 없으면 기본 디버그 키를 쓴다.
    System.getenv("DEBUG_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }?.let {
        signingConfigs.getByName("debug").storeFile = file(it)
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    // HagoondoriDatabase(exportSchema = true)가 요구하는 스키마 히스토리 출력 위치.
    // 향후 마이그레이션을 작성/검증할 때 이 폴더의 버전별 스키마 JSON을 참고한다.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.datetime)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
