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
    api(platform(libs.exposed.bom))
    api(libs.exposed.core)
    compileOnly(libs.exposed.jdbc)
    compileOnly(libs.exposed.dao)
    compileOnly(libs.exposed.java.time)

    // Coroutines
    compileOnly(libs.kotlinx.coroutines.core)

    // Test Fixtures
    testFixturesApi(project(":bluetape4k-logging"))
    testFixturesApi(platform(libs.exposed.bom))
    testFixturesApi(libs.exposed.core)
    testFixturesApi(libs.exposed.jdbc)
    testFixturesImplementation(libs.exposed.java.time)

    testFixturesImplementation(project(":bluetape4k-junit5"))
    testFixturesImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testFixturesImplementation(project(":bluetape4k-exposed-r2dbc-tests"))
    testFixturesCompileOnly(libs.exposed.r2dbc)

    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.kotlinx.coroutines.test)

    testFixturesImplementation(libs.kluent)
    testFixturesImplementation(libs.awaitility.kotlin)

    // Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.kluent)
}
