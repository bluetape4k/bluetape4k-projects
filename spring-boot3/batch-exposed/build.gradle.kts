plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot3.dependencies))
    // Core
    api(libs.kotlin.reflect)
    api(project(":bluetape4k-exposed-jdbc"))
    api(project(":bluetape4k-exposed-core"))
    api(project(":bluetape4k-virtualthread-api"))

    // Exposed
    api(libs.exposed.spring.transaction)
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.java.time)

    // Spring Batch (Spring Boot BOM 버전 관리)
    api("org.springframework.boot:spring-boot-starter-batch")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    // Test
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(project(":bluetape4k-virtualthread-jdk21"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.spring.batch.test)
    testImplementation(libs.h2.v2)
    testImplementation(libs.hikaricp)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.postgresql.driver)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.mysql.connector.j)
}
