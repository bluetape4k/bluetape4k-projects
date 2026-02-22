configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.timefold_solver_bom))

    api(Libs.timefold_solver_core)
    api(Libs.timefold_solver_persistence_common)
    testImplementation(Libs.timefold_solver_test)

    api(Libs.exposed_core)
    compileOnly(project(":bluetape4k-exposed-jdbc"))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))

    // JDBC Drivers
    testImplementation(Libs.hikaricp)
    testRuntimeOnly(Libs.h2_v2)
    testRuntimeOnly(Libs.mariadb_java_client)
    testRuntimeOnly(Libs.mysql_connector_j)
    testRuntimeOnly(Libs.postgresql_driver)
    testRuntimeOnly(Libs.pgjdbc_ng)

    // Bluetape4k Modules for Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_mariadb)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)
}
