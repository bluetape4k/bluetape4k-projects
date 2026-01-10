configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(Libs.exposed_bom))
    api(Libs.exposed_core)
    compileOnly(Libs.exposed_jdbc)
    compileOnly(Libs.exposed_dao)
    api(project(":bluetape4k-exposed"))
    testImplementation(project(":bluetape4k-exposed-tests"))

    /* Jackson */
    api(project(":bluetape4k-jackson"))
    api(Libs.jackson_module_kotlin)
    implementation(Libs.jackson_module_blackbird)

    // Database Drivers
    testRuntimeOnly(Libs.hikaricp)
    testRuntimeOnly(Libs.h2_v2)
    testRuntimeOnly(Libs.mariadb_java_client)
    testRuntimeOnly(Libs.mysql_connector_j)
    testRuntimeOnly(Libs.postgresql_driver)
    testRuntimeOnly(Libs.pgjdbc_ng)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_mariadb)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)
}
