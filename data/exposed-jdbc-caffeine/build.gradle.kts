configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-exposed-jdbc"))
    api(project(":bluetape4k-exposed-cache"))
    api(Libs.caffeine)

    api(Libs.exposed_core)
    api(Libs.exposed_jdbc)
    compileOnly(Libs.exposed_java_time)
    compileOnly(Libs.exposed_kotlin_datetime)

    api(Libs.kotlinx_coroutines_core)

    testImplementation(testFixtures(project(":bluetape4k-exposed-cache")))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.awaitility_kotlin)

    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_mariadb)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)
    testImplementation(Libs.mariadb_java_client)
    testImplementation(Libs.mysql_connector_j)
    testImplementation(Libs.postgresql_driver)
    testImplementation(Libs.pgjdbc_ng)
}
