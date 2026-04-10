configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-coroutines"))
    api(project(":bluetape4k-logging"))
    api(project(":bluetape4k-workflow"))

    implementation(project(":bluetape4k-virtualthread-api"))
    runtimeOnly(project(":bluetape4k-virtualthread-jdk21"))

    // Exposed JDBC/R2DBC — 선택적 백엔드 (compileOnly)
    compileOnly(project(":bluetape4k-exposed-jdbc"))
    compileOnly(project(":bluetape4k-exposed-r2dbc"))
    compileOnly(Libs.exposed_java_time)

    // Checkpoint JSON 직렬화 — bluetape4k-jackson3 선택 의존
    compileOnly(project(":bluetape4k-jackson3"))

    // Coroutines
    implementation(Libs.kotlinx_coroutines_core)

    // Test
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-jackson3"))
    testImplementation(Libs.kotlinx_coroutines_test)

    // JDBC/R2DBC 통합 테스트 인프라
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))
    testImplementation(project(":bluetape4k-virtualthread-jdk21"))

    // Test DB — H2 (내장)
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.r2dbc_h2)
    testImplementation(Libs.r2dbc_pool)

    // Test DB — PostgreSQL (Testcontainers)
    testImplementation(Libs.testcontainers_postgresql)
    testImplementation(Libs.postgresql_driver)
    testImplementation(Libs.r2dbc_postgresql)

    // Test DB — MySQL (Testcontainers)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.mysql_connector_j)
    testImplementation(Libs.r2dbc_mysql)
}
