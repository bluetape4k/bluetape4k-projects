tasks.test {
    // DuckDB JDBC uses System.load() for native library — required for Java 25+
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

dependencies {
    api(project(":bluetape4k-logging"))
    api(Libs.exposed_core)
    api(Libs.exposed_jdbc)
    api(Libs.exposed_java_time)
    api(Libs.kotlinx_coroutines_core)

    // DuckDB JDBC 드라이버
    api(Libs.duckdb_jdbc)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(Libs.kotlinx_coroutines_test)
}
