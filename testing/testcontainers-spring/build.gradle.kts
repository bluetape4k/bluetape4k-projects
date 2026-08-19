plugins {
    `java-library`
    kotlin("jvm")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(platform(bt4k.spring.boot4.dependencies))
    api(project(":bluetape4k-testcontainers"))
    api("org.springframework:spring-test")

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(bt4k.junit.jupiter.engine)
    testImplementation(bt4k.junit.platform.launcher)
    testRuntimeOnly(bt4k.logback)
}
