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
    testImplementation(bt4k.junit.jupiter.engine)
    testImplementation(bt4k.junit.platform.launcher)
    testRuntimeOnly(bt4k.logback)
}
