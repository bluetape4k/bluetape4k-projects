@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(Libs.exposed_bom))

    api(Libs.exposed_core)
    api(Libs.exposed_r2dbc)
    implementation(Libs.exposed_java_time)
    implementation(Libs.exposed_kotlin_datetime)

    api(project(":bluetape4k-exposed-r2dbc"))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))

    // Redisson
    api(project(":bluetape4k-redis"))
    api(Libs.redisson)

    // Codecs
    api(project(":bluetape4k-io"))

    compileOnly(Libs.kryo5)
    compileOnly(Libs.fory_kotlin)  // new Apache Fory
    compileOnly(Libs.fury_kotlin)  // old Apache Fury

    // Compressor
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.zstd_jni)

    // R2DBC
    api(Libs.r2dbc_spi)
    api(Libs.r2dbc_pool)
    implementation(Libs.r2dbc_h2)
    implementation(Libs.r2dbc_mariadb)
    implementation(Libs.r2dbc_mysql)
    implementation(Libs.r2dbc_postgresql)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactive)
    testImplementation(Libs.kotlinx_coroutines_test)

    testImplementation(project(":bluetape4k-idgenerators"))
    testImplementation(project(":bluetape4k-javatimes"))

    // Bluetape4k Modules for Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_mariadb)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)

    // Database Drivers
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.mariadb_java_client)
    testImplementation(Libs.mysql_connector_j)
    testImplementation(Libs.postgresql_driver)

}
