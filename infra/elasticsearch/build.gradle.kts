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
    api(Libs.elasticsearch_java)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)

    // Jackson 2 & 3 (optional)
    compileOnly(project(":bluetape4k-jackson2"))
    compileOnly(Libs.jackson_databind)
    compileOnly(Libs.jackson_module_kotlin)
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(Libs.jackson3_databind)
    compileOnly(Libs.jackson3_module_kotlin)

    // Test Fixtures
    testFixturesApi(project(":bluetape4k-junit5"))
    testFixturesApi(project(":bluetape4k-testcontainers"))
    testFixturesApi(Libs.elasticsearch_java)
    testFixturesApi(Libs.kotlinx_coroutines_core)
    testFixturesApi(Libs.kotlinx_coroutines_test)
    testFixturesApi(Libs.testcontainers_elasticsearch)

    // Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-jackson3"))
    testImplementation(Libs.jackson3_databind)
    testImplementation(Libs.jackson3_module_kotlin)
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.testcontainers_elasticsearch)
    testImplementation(Libs.mockk)
    testImplementation(Libs.kluent)
}
