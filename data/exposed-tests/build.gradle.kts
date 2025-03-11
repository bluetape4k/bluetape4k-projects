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
    compileOnly(Libs.exposed_crypt)
    compileOnly(Libs.exposed_kotlin_datetime)
    compileOnly(Libs.exposed_java_time)
    compileOnly(Libs.exposed_json)
    compileOnly(Libs.exposed_money)
    compileOnly(Libs.exposed_migration)
    compileOnly(Libs.exposed_spring_boot_starter)
    compileOnly(project(":bluetape4k-exposed"))

    // Bluetape4k
    compileOnly(project(":bluetape4k-jdbc"))
    compileOnly(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-crypto"))

    implementation(project(":bluetape4k-junit5"))
    implementation(project(":bluetape4k-testcontainers"))
    api(Libs.testcontainers)
    api(Libs.testcontainers_junit_jupiter)
    api(Libs.testcontainers_mariadb)
    api(Libs.testcontainers_mysql)
    api(Libs.testcontainers_postgresql)
    implementation(Libs.testcontainers_cockroachdb)

    // Database Drivers
    compileOnly(Libs.hikaricp)

    // Database Drivers
    api(Libs.h2_v2)
    api(Libs.mariadb_java_client)
    api(Libs.mysql_connector_j)
    api(Libs.postgresql_driver)
    api(Libs.pgjdbc_ng)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    implementation(Libs.kotlinx_coroutines_debug)
    implementation(Libs.kotlinx_coroutines_test)

    // Id Generators
    implementation(project(":bluetape4k-idgenerators"))
    implementation(Libs.java_uuid_generator)
}
