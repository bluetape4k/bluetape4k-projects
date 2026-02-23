configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(Libs.exposed_bom))

    api(Libs.exposed_core)
    api(Libs.exposed_jdbc)
    api(Libs.exposed_dao)
    implementation(Libs.exposed_java_time)
    implementation(Libs.exposed_kotlin_datetime)

    api(project(":bluetape4k-exposed-jdbc"))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))

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
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_junit_jupiter)

    // Database Drivers
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.postgresql_driver)

    testImplementation(project(":bluetape4k-idgenerators"))
}
