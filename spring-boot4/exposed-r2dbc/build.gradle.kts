plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Spring Boot 4 BOM: platform()을 사용하면 compileClasspath/runtimeClasspath에만 적용되고
    // kotlinBuildToolsApiClasspath 같은 내부 Gradle 설정에는 영향을 주지 않음
    // (dependencyManagement 플러그인은 ALL configurations에 적용되어 kotlin-stdlib 버전 충돌 유발)
    implementation(platform(libs.spring.boot4.dependencies))

    api("org.springframework.data:spring-data-commons")

    // JDBC 모듈 재사용: EntityInformation, ExposedMappingContext
    api(project(":bluetape4k-spring-boot4-exposed-jdbc"))

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
