configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(libs.exposed.bom))
    api(project(":bluetape4k-exposed-core"))
    compileOnly(libs.exposed.jdbc)
    compileOnly(libs.exposed.java.time)

    // Logging
    implementation(project(":bluetape4k-logging"))

    // PostgreSQL 전용 라이브러리 (사용자가 필요한 것만 런타임에 추가)
    compileOnly(libs.postgis.jdbc)          // PostGIS 사용 시만
    compileOnly(libs.pgvector)              // pgvector 사용 시만

    // Database Drivers
    compileOnly(libs.postgresql.driver)

    // Testing
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)

    testRuntimeOnly(libs.h2.v2)
    testRuntimeOnly(libs.postgresql.driver)
    testRuntimeOnly(libs.hikaricp)
}
