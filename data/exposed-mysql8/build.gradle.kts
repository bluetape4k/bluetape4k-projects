configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(Libs.exposed_bom))
    api(project(":bluetape4k-exposed-core"))
    compileOnly(Libs.exposed_jdbc)
    compileOnly(Libs.exposed_java_time)  // 현재 미사용, exposed-postgresql 패턴과 일관성 위해 포함

    // Logging
    implementation(project(":bluetape4k-logging"))

    // MySQL 8 GIS 전용 라이브러리 (사용자가 필요한 것만 런타임에 추가)
    api(Libs.jts_core)                   // JTS Core (Geometry 타입)

    // Database Drivers
    compileOnly(Libs.mysql_connector_j)

    // Testing
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_mysql)

    testRuntimeOnly(Libs.mysql_connector_j)
    testRuntimeOnly(Libs.hikaricp)
}
