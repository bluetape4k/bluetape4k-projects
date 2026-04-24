plugins {
    `java-test-fixtures`
}

// testFixtures 시나리오 패키지는 다른 모듈의 통합 테스트 지원용 코드이므로
// 이 모듈의 커버리지 측정에서 제외한다.
kover {
    reports {
        filters {
            excludes {
                packages("io.bluetape4k.exposed.cache.scenarios")
            }
        }
    }
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
    testFixturesImplementation(project(":bluetape4k-exposed-r2dbc-tests"))
    testFixturesCompileOnly(Libs.exposed_r2dbc)

    testFixturesImplementation(Libs.kotlinx_coroutines_core)
    testFixturesImplementation(Libs.kotlinx_coroutines_test)

    testFixturesImplementation(Libs.kluent)
    testFixturesImplementation(Libs.awaitility_kotlin)

    // Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(Libs.kluent)
}
