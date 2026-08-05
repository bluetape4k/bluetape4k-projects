// bluetape4k-* 모듈 의존 금지 (ADR-9): 순환 의존 방지 및 최소 의존성 유지
plugins {
    `java-library`
    kotlin("jvm")
}

description = "Bluetape4k testing assertions — bluetape4k-assertions compatible, JUnit 5 native"

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(platform(bt4k.junit.bom))

    // JUnit 5 — AssertionFailedError, MultipleFailuresError 타입 소비자 classpath 필요
    api(bt4k.junit.jupiter.api)

    // Coroutines — FlowAssertions 가 main 소스에 있으므로 소비자 classpath 필요
    api(libs.kotlinx.coroutines.core)

    // Turbine — TurbineSupport 사용 시 소비자가 직접 추가 필요
    compileOnly(bt4k.turbine)

    // Test
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(bt4k.junit.jupiter.engine)
    testImplementation(bt4k.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(bt4k.mockk)
    testImplementation(bt4k.datafaker)
    testRuntimeOnly(bt4k.logback)
}
