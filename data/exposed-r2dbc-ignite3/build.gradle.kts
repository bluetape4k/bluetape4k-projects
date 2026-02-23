
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed R2DBC
    implementation(platform(Libs.exposed_bom))

    api(Libs.exposed_core)
    api(Libs.exposed_r2dbc)
    api(Libs.exposed_dao)
    compileOnly(Libs.exposed_java_time)
    compileOnly(Libs.exposed_kotlin_datetime)

    api(project(":bluetape4k-exposed-r2dbc"))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))

    // Apache Ignite 3.x
    api(project(":bluetape4k-ignite3"))
    api(Libs.ignite3_client)

    // Front Cache (Near Cache 로컬 레이어)
    compileOnly(Libs.caffeine)

    testImplementation(project(":bluetape4k-io"))

    // Codecs
    compileOnly(Libs.kryo5)
    compileOnly(Libs.fory_kotlin)

    // Compressor
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.zstd_jni)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactive)
    testImplementation(Libs.kotlinx_coroutines_test)

    // R2DBC
    api(Libs.r2dbc_spi)
    api(Libs.r2dbc_pool)
    testRuntimeOnly(Libs.r2dbc_h2)
    testRuntimeOnly(Libs.r2dbc_postgresql)

    // Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_junit_jupiter)

    // Database Drivers
    testRuntimeOnly(Libs.h2_v2)
    testRuntimeOnly(Libs.postgresql_driver)

    testImplementation(project(":bluetape4k-idgenerators"))
}
