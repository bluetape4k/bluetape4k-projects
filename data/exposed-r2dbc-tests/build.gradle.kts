configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot3.dependencies))
    // Exposed
    implementation(platform(libs.exposed.bom))

    api(libs.exposed.core)
    api(libs.exposed.r2dbc)
    implementation(libs.exposed.migration.r2dbc)
    implementation(libs.exposed.java.time)

    implementation(project(":bluetape4k-idgenerators"))

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // R2DBC
    api(libs.r2dbc.spi)
    api(libs.r2dbc.pool)
    implementation(libs.r2dbc.h2)
    implementation(libs.r2dbc.mariadb)
    implementation(libs.r2dbc.mysql)
    implementation(libs.r2dbc.postgresql)

    // Bluetape4k Modules for Testing
    api(project(":bluetape4k-junit5"))
    api(project(":bluetape4k-testcontainers"))
    api(libs.testcontainers.mariadb)
    api(libs.testcontainers.mysql)
    api(libs.testcontainers.postgresql)

    // Database Drivers for Testcontainers Database
    compileOnly(libs.h2.v2)
    compileOnly(libs.mariadb.java.client)
    compileOnly(libs.mysql.connector.j)
    compileOnly(libs.postgresql.driver)

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
}
