tasks.test {
    // DuckDB JDBC uses System.load() for native library — required for Java 25+
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

dependencies {
    implementation(project(":bluetape4k-core"))
    api(project(":bluetape4k-logging"))
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.java.time)
    api(libs.kotlinx.coroutines.core)

    // DuckDB JDBC 드라이버
    api(libs.duckdb.jdbc)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.kotlinx.coroutines.test)
}
