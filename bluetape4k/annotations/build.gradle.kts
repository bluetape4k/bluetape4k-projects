plugins {
    `java-library`
    kotlin("jvm")
}

description = "Bluetape API maturity annotations"

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    testImplementation(project(":bluetape4k-assertions"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.logback.classic)
}
