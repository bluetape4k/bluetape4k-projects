plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.spring_boot3_dependencies))
    api(Libs.springData("commons"))

    api(Libs.kotlin_reflect)
    api(project(":bluetape4k-logging"))
    api(Libs.exposed_core)
    api(Libs.exposed_dao)
    api(Libs.exposed_jdbc)
    api(Libs.exposed_java_time)
    api(Libs.exposed_spring_transaction)

    testImplementation(Libs.exposed_migration_jdbc)
    testImplementation(Libs.flyway_core)
    testImplementation(project(":bluetape4k-junit5"))

    testImplementation(project(":bluetape4k-virtualthread-jdk21"))

    api(project(":bluetape4k-exposed-jdbc"))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))

    compileOnly(Libs.springBoot("autoconfigure"))
    compileOnly(Libs.springBootStarter("data-jdbc"))
    testImplementation(Libs.springBootStarter("test"))
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)

    // Multi-DB 테스트용 JDBC 드라이버
    testImplementation(Libs.mysql_connector_j)
    testImplementation(Libs.mariadb_java_client)
    testImplementation(Libs.postgresql_driver)
}
