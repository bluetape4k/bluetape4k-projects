@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(Libs.exposed_bom))

    api(project(":bluetape4k-exposed"))
    api(Libs.exposed_core)
    api(Libs.exposed_jdbc)
    api(Libs.exposed_dao)
    testImplementation(Libs.exposed_java_time)
    testImplementation(Libs.exposed_spring_boot_starter)
    testImplementation(project(":bluetape4k-exposed-tests"))

    // Redisson
    api(project(":bluetape4k-redis"))
    api(Libs.redisson)

    // Codecs
    implementation(project(":bluetape4k-io"))
    compileOnly(Libs.kryo)
    compileOnly(Libs.fury_kotlin)

    compileOnly(project(":bluetape4k-jackson"))
    compileOnly(project(":bluetape4k-jackson-binary"))
    compileOnly(Libs.jackson_module_kotlin)
    compileOnly(Libs.jackson_dataformat_cbor)

    // Compressor
    implementation(Libs.snappy_java)
    implementation(Libs.lz4_java)
    implementation(Libs.zstd_jni)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Bluetape4k Modules for Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_mariadb)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)

    testImplementation(project(":bluetape4k-idgenerators"))
    testImplementation(project(":bluetape4k-javatimes"))

    // Database Drivers
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.mariadb_java_client)
    testImplementation(Libs.mysql_connector_j)
    testImplementation(Libs.postgresql_driver)
    testImplementation(Libs.pgjdbc_ng)

    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}
