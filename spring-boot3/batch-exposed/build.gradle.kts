plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.spring_boot3_dependencies))
    // Core
    api(Libs.kotlin_reflect)
    api(project(":bluetape4k-exposed-jdbc"))
    api(project(":bluetape4k-exposed-core"))
    api(project(":bluetape4k-virtualthread-api"))

    // Exposed
    api(Libs.exposed_spring_transaction)
    api(Libs.exposed_core)
    api(Libs.exposed_jdbc)
    api(Libs.exposed_java_time)

    // Spring Batch (Spring Boot BOM 버전 관리)
    api(Libs.springBootStarter("batch"))
    compileOnly(Libs.springBoot("autoconfigure"))

    // Test
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(project(":bluetape4k-virtualthread-jdk21"))
    testImplementation(Libs.springBootStarter("test"))
    testImplementation(Libs.spring_batch_test)
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.testcontainers_postgresql)
    testImplementation(Libs.postgresql_driver)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.mysql_connector_j)
}
