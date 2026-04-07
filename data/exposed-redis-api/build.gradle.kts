plugins {
    `java-test-fixtures`
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Bluetape4k
    api(project(":bluetape4k-logging"))

    // Exposed
    api(platform(Libs.exposed_bom))
    api(Libs.exposed_core)
    compileOnly(Libs.exposed_jdbc)
    compileOnly(Libs.exposed_dao)
    compileOnly(Libs.exposed_java_time)

    // Coroutines
    compileOnly(Libs.kotlinx_coroutines_core)

    // Test Fixtures
    testFixturesApi(project(":bluetape4k-logging"))
    testFixturesApi(platform(Libs.exposed_bom))
    testFixturesApi(Libs.exposed_core)
    testFixturesApi(Libs.exposed_jdbc)
    testFixturesImplementation(Libs.exposed_java_time)

    testFixturesImplementation(project(":bluetape4k-junit5"))
    testFixturesImplementation(project(":bluetape4k-exposed-jdbc-tests"))

    testFixturesImplementation(Libs.kotlinx_coroutines_core)
    testFixturesImplementation(Libs.kotlinx_coroutines_test)

    testFixturesImplementation(Libs.kluent)
    testFixturesImplementation(Libs.awaitility_kotlin)

    // Testing
    testImplementation(project(":bluetape4k-junit5"))
}
