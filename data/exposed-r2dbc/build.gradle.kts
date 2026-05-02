configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(libs.exposed.bom))

    api(libs.exposed.core)
    api(libs.exposed.r2dbc)
    compileOnly(libs.exposed.migration.r2dbc)
    testImplementation(libs.exposed.java.time)

    api(project(":bluetape4k-exposed-core"))
    api(project(":bluetape4k-exposed-dao"))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))

    api(project(":bluetape4k-r2dbc"))
    api(libs.r2dbc.spi)
    testRuntimeOnly(libs.r2dbc.h2)
    testRuntimeOnly(libs.r2dbc.mariadb)
    testRuntimeOnly(libs.r2dbc.mysql)
    testRuntimeOnly(libs.r2dbc.postgresql)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    compileOnly(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-idgenerators"))

    // Bluetape4k Modules for Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.postgresql)

    // Database Drivers for Testcontainers Databases
    testRuntimeOnly(libs.h2.v2)
    testRuntimeOnly(libs.mariadb.java.client)
    testRuntimeOnly(libs.mysql.connector.j)
    testRuntimeOnly(libs.postgresql.driver)
}
