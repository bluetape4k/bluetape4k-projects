plugins {
    `java-test-fixtures`
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Bluetape4k Core
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-logging"))

    // Elasticsearch Java Client
    api(bt4k.elasticsearch.java)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(libs.kotlinx.coroutines.core)

    // Jackson 2 & 3 (optional)
    compileOnly(project(":bluetape4k-jackson2"))
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.module.kotlin)
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(libs.jackson3.databind)
    compileOnly(libs.jackson3.module.kotlin)

    // Test Fixtures
    testFixturesApi(project(":bluetape4k-junit5"))
    testFixturesApi(project(":bluetape4k-testcontainers"))
    testFixturesApi(
        platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:${bt4k.versions.kotlinx.coroutines.get()}"),
    )
    testFixturesApi(
        platform("org.testcontainers:testcontainers-bom:${bt4k.versions.testcontainers.get()}"),
    )
    testFixturesApi(bt4k.elasticsearch.java)
    testFixturesApi(libs.kotlinx.coroutines.core)
    testFixturesApi(libs.kotlinx.coroutines.test)
    testFixturesApi(libs.testcontainers.elasticsearch)

    // Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-jackson3"))
    testImplementation(libs.jackson3.databind)
    testImplementation(libs.jackson3.module.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.testcontainers.elasticsearch)
    testImplementation(bt4k.mockk)
}
