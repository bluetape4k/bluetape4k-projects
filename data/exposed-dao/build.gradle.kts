configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(libs.exposed.bom))
    api(project(":bluetape4k-exposed-core"))
    api(libs.exposed.dao)
    compileOnly(libs.exposed.jdbc)

    // Entity ID generators
    api(project(":bluetape4k-idgenerators"))
    api(libs.java.uuid.generator)

    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.postgresql)

    // Database Drivers
    testRuntimeOnly(libs.h2.v2)
    testRuntimeOnly(libs.mariadb.java.client)
    testRuntimeOnly(libs.mysql.connector.j)
    testRuntimeOnly(libs.postgresql.driver)
    testRuntimeOnly(libs.pgjdbc.ng)
}
