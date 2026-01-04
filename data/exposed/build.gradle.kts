configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(Libs.exposed_bom))
    api(Libs.exposed_core)
    compileOnly(Libs.exposed_jdbc)
    compileOnly(Libs.exposed_dao)
    compileOnly(Libs.exposed_crypt)
    compileOnly(Libs.exposed_kotlin_datetime)
    compileOnly(Libs.exposed_java_time)
    compileOnly(Libs.exposed_json)
    compileOnly(Libs.exposed_money)
    compileOnly(Libs.exposed_migration_jdbc)
    compileOnly(Libs.exposed_spring_boot_starter)

    testImplementation(project(":bluetape4k-exposed-tests"))

    // Entity ID generators
    implementation(project(":bluetape4k-idgenerators"))
    implementation(Libs.java_uuid_generator)

    // JDBC
    // compileOnly(project(":bluetape4k-jdbc"))

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_mariadb)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)

    // Database Drivers
    compileOnly(Libs.hikaricp)
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.mariadb_java_client)
    testImplementation(Libs.mysql_connector_j)
    testImplementation(Libs.postgresql_driver)
    testImplementation(Libs.pgjdbc_ng)

    // Spring Boot
    testImplementation(Libs.springBootStarter("jdbc"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    //
    // Custom Column Types
    //

    compileOnly(project(":bluetape4k-io"))

    // Serializer
    compileOnly(Libs.kryo5)
    compileOnly(Libs.fory_kotlin)  // new Apache Fory
    compileOnly(Libs.fury_kotlin)  // old Apache Fury

    // Compressors
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.zstd_jni)

    // Encryption
    compileOnly(project(":bluetape4k-crypto"))
    compileOnly(Libs.jasypt)
}
