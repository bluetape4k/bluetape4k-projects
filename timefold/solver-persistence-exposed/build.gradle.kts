configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.timefold_solver_bom))

    api(Libs.timefold_solver_persistence_common)
    testImplementation(Libs.timefold_solver_test)

    api(Libs.exposed_core)
    compileOnly(project(":bluetape4k-exposed"))
    compileOnly(project(":bluetape4k-exposed-r2dbc"))
    testImplementation(project(":bluetape4k-exposed-tests"))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))

    // JDBC Drivers
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.mariadb_java_client)
    testImplementation(Libs.mysql_connector_j)
    testImplementation(Libs.postgresql_driver)
    testImplementation(Libs.pgjdbc_ng)

    // R2DBC Drivers
    compileOnly(Libs.r2dbc_spi)
    compileOnly(Libs.r2dbc_pool)
    testImplementation(Libs.r2dbc_h2)
    testImplementation(Libs.r2dbc_mariadb)
    testImplementation(Libs.r2dbc_mysql)
    testImplementation(Libs.r2dbc_postgresql)

    // Bluetape4k Modules for Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_mariadb)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)
}
