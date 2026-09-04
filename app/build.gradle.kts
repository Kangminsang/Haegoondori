import java.util.Properties

// app 모듈: Android(Compose + Hilt + Room). core에 의존한다.
//
// 이 모듈은 Android SDK가 필요하므로, dl.google.com이 차단된 이 개발 환경에서는
// 실제로 빌드/실행 검증을 할 수 없다 (README.md의 "이 세션에서 검증 불가" 절 참고).
// 코드는 신중하게 작성했지만, 로컬 Android Studio에서 최종 빌드 확인이 필요하다.
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
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 공공데이터포털 "한국천문연구원 특일 정보" 서비스키. local.properties에
        // holiday.api.key=... 로 직접 넣는다(레포에 커밋되지 않음, .gitignore 대상).
        // 키가 없어도 앱은 정상 동작한다 - HolidayRemoteRepository가 빈 문자열을
        // 감지하면 네트워크 호출을 생략하고 내장 데이터+수동입력 경로로 넘어간다.
        val localProperties = Properties().apply {
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { load(it) }
            }
        }
        buildConfigField(
            "String",
            "HOLIDAY_API_KEY",
            "\"${localProperties.getProperty("holiday.api.key", "")}\"",
        )
    }

    buildTypes {
        release {
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
