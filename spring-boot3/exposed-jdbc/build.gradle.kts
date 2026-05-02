plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot3.dependencies))
    api("org.springframework.data:spring-data-commons")

    api(libs.kotlin.reflect)
    api(project(":bluetape4k-logging"))
    api(libs.exposed.core)
    api(libs.exposed.dao)
    api(libs.exposed.jdbc)
    api(libs.exposed.java.time)
    api(libs.exposed.spring.transaction)

    testImplementation(libs.exposed.migration.jdbc)
    testImplementation(libs.flyway.core)
    testImplementation(project(":bluetape4k-junit5"))

    testImplementation(project(":bluetape4k-virtualthread-jdk21"))

    api(project(":bluetape4k-exposed-jdbc"))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))

    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework.boot:spring-boot-starter-data-jdbc")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.h2.v2)
    testImplementation(libs.hikaricp)

    // Multi-DB 테스트용 JDBC 드라이버
    testImplementation(libs.mysql.connector.j)
    testImplementation(libs.mariadb.java.client)
    testImplementation(libs.postgresql.driver)
}
