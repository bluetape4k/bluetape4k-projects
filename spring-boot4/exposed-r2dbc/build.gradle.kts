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
    implementation(platform(Libs.spring_boot4_dependencies))

    api(Libs.springData("commons"))

    // JDBC 모듈 재사용: EntityInformation, ExposedMappingContext
    api(project(":bluetape4k-spring-boot4-exposed-jdbc"))

    api(Libs.kotlin_reflect)
    api(Libs.exposed_core)
    api(Libs.exposed_r2dbc)
    api(Libs.exposed_java_time)

    testImplementation(Libs.exposed_migration_r2dbc)
    testImplementation(Libs.flyway_core)
    testImplementation(project(":bluetape4k-junit5"))

    api(project(":bluetape4k-exposed-r2dbc"))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))

    testImplementation(project(":bluetape4k-virtualthread-jdk21"))

    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    api(Libs.kotlinx_coroutines_reactor)  // Spring Data 코루틴 지원 요구사항
    testImplementation(Libs.kotlinx_coroutines_test)

    compileOnly(Libs.springBoot("autoconfigure"))

    testImplementation(Libs.springBootStarter("test"))

    testImplementation(Libs.h2_v2)
    testImplementation(Libs.r2dbc_h2)
    testImplementation(Libs.hikaricp)

    // Multi-DB 테스트용 R2DBC 드라이버
    testImplementation(Libs.r2dbc_mysql)
    testImplementation(Libs.r2dbc_mariadb)
    testImplementation(Libs.r2dbc_postgresql)

    // Multi-DB 테스트용 JDBC 드라이버 (Testcontainers 컨테이너 연결용)
    testImplementation(Libs.mysql_connector_j)
    testImplementation(Libs.mariadb_java_client)
    testImplementation(Libs.postgresql_driver)
}
