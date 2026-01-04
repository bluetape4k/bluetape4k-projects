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
    compileOnly(Libs.exposed_migration_jdbc)
    compileOnly(Libs.exposed_spring_boot_starter)
    compileOnly(project(":bluetape4k-exposed"))

    // Bluetape4k
    compileOnly(project(":bluetape4k-jdbc"))
    compileOnly(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-crypto"))

    api(project(":bluetape4k-junit5"))
    api(project(":bluetape4k-testcontainers"))
    api(Libs.testcontainers)
    api(Libs.testcontainers_mariadb)
    api(Libs.testcontainers_mysql)
    api(Libs.testcontainers_postgresql)
    // compileOnly(Libs.testcontainers_cockroachdb)

    // Database Drivers
    compileOnly(Libs.hikaricp)

    // Database Drivers
    compileOnly(Libs.h2_v2)
    compileOnly(Libs.mariadb_java_client)
    compileOnly(Libs.mysql_connector_j)
    compileOnly(Libs.postgresql_driver)
    compileOnly(Libs.pgjdbc_ng)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_debug)
    implementation(Libs.kotlinx_coroutines_test)

    // Id Generators
    compileOnly(project(":bluetape4k-idgenerators"))
    compileOnly(Libs.java_uuid_generator)
}
