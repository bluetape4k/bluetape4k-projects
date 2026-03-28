configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(Libs.exposed_bom))
    api(project(":bluetape4k-exposed-core"))
    compileOnly(Libs.exposed_jdbc)
    compileOnly(Libs.exposed_java_time)

    // Logging
    implementation(project(":bluetape4k-logging"))

    // PostgreSQL 전용 라이브러리 (사용자가 필요한 것만 런타임에 추가)
    compileOnly(Libs.postgis_jdbc)          // PostGIS 사용 시만
    compileOnly(Libs.pgvector)              // pgvector 사용 시만

    // Database Drivers
    compileOnly(Libs.postgresql_driver)

    // Testing
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_postgresql)

    testRuntimeOnly(Libs.h2_v2)
    testRuntimeOnly(Libs.postgresql_driver)
    testRuntimeOnly(Libs.hikaricp)
}
