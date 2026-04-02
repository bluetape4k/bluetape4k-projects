dependencies {
    api(project(":bluetape4k-logging"))
    api(Libs.exposed_core)
    api(Libs.exposed_jdbc)
    api(Libs.exposed_java_time)
    api(Libs.kotlinx_coroutines_core)

    // Trino JDBC 드라이버
    api(Libs.trino_jdbc)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.testcontainers_trino)
}
