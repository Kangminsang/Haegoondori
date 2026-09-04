// core 모듈: 순수 Kotlin/JVM. Android 의존성이 전혀 없다.
// 스펙 3장(출타 계산), 4장(데이터 모델), 6장(ExportAdapter), 7장(페이로드 인코딩)이 여기 산다.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // jvmToolchain(17)이 아니라 컴파일러 타겟만 17로 고정한다: 이 편이 "정확히 JDK 17
    // 실행 파일"을 요구하는 toolchain 자동 프로비저닝(네트워크 필요) 없이도, 현재 사용 가능한
    // JDK(21 등)로 Android(JDK 17 호환)와 맞는 바이트코드를 생성할 수 있다.
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.core)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
