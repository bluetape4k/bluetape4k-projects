dependencies {
    api(project(":bluetape4k-logging"))
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.java.time)
    api(libs.kotlinx.coroutines.core)

    // Trino JDBC 드라이버
    api(libs.trino.jdbc)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.testcontainers.trino)
}
