plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot3.dependencies))
    api("org.springframework.data:spring-data-commons")

    api(project(":bluetape4k-spring-boot3-exposed-jdbc"))  // EntityInformation, ExposedMappingContext 재사용
    api(libs.kotlin.reflect)
    api(libs.exposed.core)
    api(libs.exposed.r2dbc)
    api(libs.exposed.java.time)

    testImplementation(libs.exposed.migration.r2dbc)
    testImplementation(libs.flyway.core)
    testImplementation(project(":bluetape4k-junit5"))

    api(project(":bluetape4k-exposed-r2dbc"))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))

    testImplementation(project(":bluetape4k-virtualthread-jdk21"))

    api(project(":bluetape4k-coroutines"))
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.reactor)  // Spring Data 코루틴 지원 요구사항
    testImplementation(libs.kotlinx.coroutines.test)

    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation(libs.h2.v2)
    testImplementation(libs.r2dbc.h2)
    testImplementation(libs.hikaricp)

    // Multi-DB 테스트용 R2DBC 드라이버
    testImplementation(libs.r2dbc.mysql)
    testImplementation(libs.r2dbc.mariadb)
    testImplementation(libs.r2dbc.postgresql)

    // Multi-DB 테스트용 JDBC 드라이버 (Testcontainers 컨테이너 연결용)
    testImplementation(libs.mysql.connector.j)
    testImplementation(libs.mariadb.java.client)
    testImplementation(libs.postgresql.driver)
}
