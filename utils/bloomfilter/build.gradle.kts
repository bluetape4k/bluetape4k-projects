configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    // Hashing
    api(Libs.zero_allocation_hashing)

    // Redis Drivers
    compileOnly(project(":bluetape4k-redis"))
    compileOnly(Libs.lettuce_core)
    compileOnly(Libs.redisson)

    // Codecs
    testImplementation(Libs.fory_kotlin)
    testImplementation(Libs.kryo5)

    // Compressor
    testImplementation(Libs.lz4_java)
    testImplementation(Libs.snappy_java)
    testImplementation(Libs.zstd_jni)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // TestContainers
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers)
}
