@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(Libs.exposed_bom))

    api(Libs.exposed_core)
    api(Libs.exposed_r2dbc)
    api(Libs.exposed_migration_r2dbc)
    compileOnly(Libs.exposed_java_time)

    // R2DBC
    api(Libs.r2dbc_spi)
    api(Libs.r2dbc_pool)
    api(Libs.r2dbc_h2)
    api(Libs.r2dbc_mariadb)
    api(Libs.r2dbc_mysql)
    api(Libs.r2dbc_postgresql)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Bluetape4k Modules for Testing
    api(project(":bluetape4k-junit5"))
    api(project(":bluetape4k-testcontainers"))
    api(Libs.testcontainers_mariadb)
    api(Libs.testcontainers_mysql)
    api(Libs.testcontainers_postgresql)

    implementation(project(":bluetape4k-idgenerators"))
    implementation(project(":bluetape4k-javatimes"))

    // Database Drivers
    compileOnly(Libs.h2_v2)
    compileOnly(Libs.mariadb_java_client)
    compileOnly(Libs.mysql_connector_j)
    compileOnly(Libs.postgresql_driver)

    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
}
