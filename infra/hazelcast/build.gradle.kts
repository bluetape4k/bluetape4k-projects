configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-cache"))
    compileOnly(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Hazelcast 5.x 오픈소스
    api(Libs.hazelcast)

    // Codecs
    compileOnly(Libs.fory_kotlin)
    compileOnly(Libs.kryo5)

    // Compressor
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.zstd_jni)

    // Cache
    compileOnly(Libs.caffeine)
    compileOnly(Libs.caffeine_jcache)

    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
