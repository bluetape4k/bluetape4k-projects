configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed + PostgreSQL (OLTP)
    testImplementation(project(":bluetape4k-exposed-clickhouse"))
    testImplementation(Libs.exposed_core)
    testImplementation(Libs.exposed_jdbc)
    testImplementation(Libs.exposed_java_time)
    testImplementation(Libs.postgresql_driver)
    testImplementation(Libs.hikaricp)

    // ClickHouse (OLAP)
    testImplementation(Libs.clickhouse_jdbc)

    // Coroutines
    testImplementation(project(":bluetape4k-coroutines"))
    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_clickhouse)
    testImplementation(Libs.testcontainers_postgresql)
}
