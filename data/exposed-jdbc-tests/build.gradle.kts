configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(libs.exposed.bom))
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.dao)
    implementation(libs.exposed.crypt)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.json)
    implementation(libs.exposed.money)
    implementation(libs.exposed.migration.jdbc)
    implementation(libs.exposed.spring.boot.starter)

    // Bluetape4k
    compileOnly(project(":bluetape4k-jdbc"))
    compileOnly(project(":bluetape4k-io"))
    

    api(project(":bluetape4k-junit5"))
    api(project(":bluetape4k-testcontainers"))
    api(libs.testcontainers)
    api(libs.testcontainers.mariadb)
    api(libs.testcontainers.mysql)
    api(libs.testcontainers.postgresql)
    // compileOnly(libs.testcontainers.cockroachdb)

    // Database Drivers
    compileOnly(libs.hikaricp)

    // Database Drivers
    compileOnly(libs.h2.v2)
    compileOnly(libs.mariadb.java.client)
    compileOnly(libs.mysql.connector.j)
    compileOnly(libs.postgresql.driver)
    compileOnly(libs.pgjdbc.ng)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.debug)
    implementation(libs.kotlinx.coroutines.test)

    // Id Generators
    compileOnly(project(":bluetape4k-idgenerators"))
    compileOnly(libs.java.uuid.generator)
}
