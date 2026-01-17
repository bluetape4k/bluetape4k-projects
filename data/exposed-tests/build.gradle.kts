
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(Libs.exposed_bom))
    api(Libs.exposed_core)
    api(Libs.exposed_jdbc)
    api(Libs.exposed_dao)
    implementation(Libs.exposed_crypt)
    implementation(Libs.exposed_kotlin_datetime)
    implementation(Libs.exposed_java_time)
    implementation(Libs.exposed_json)
    implementation(Libs.exposed_money)
    implementation(Libs.exposed_migration_jdbc)
    implementation(Libs.exposed_spring_boot_starter)

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
