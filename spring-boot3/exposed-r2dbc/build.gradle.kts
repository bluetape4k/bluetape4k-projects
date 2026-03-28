plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(Libs.springData("commons"))

    api(project(":bluetape4k-spring-boot3-exposed-jdbc"))  // EntityInformation, ExposedMappingContext 재사용
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
