@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(Libs.exposed_bom))
    api(Libs.exposed_core)
    api(Libs.exposed_jdbc)
    api(Libs.exposed_dao)
    api(project(":bluetape4k-exposed"))
    testImplementation(project(":bluetape4k-exposed-tests"))

    /* Fastjson2 */
    api(project(":bluetape4k-fastjson2"))

    // Database Drivers
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.mariadb_java_client)
    testImplementation(Libs.mysql_connector_j)
    compileOnly(Libs.postgresql_driver)
    testImplementation(Libs.pgjdbc_ng)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_mariadb)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)
}
