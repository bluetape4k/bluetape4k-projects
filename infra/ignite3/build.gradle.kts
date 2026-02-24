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

    // Apache Ignite 3.x thin client
    api(Libs.ignite3_client)

    // Front Cache (Near Cache 로컬 캐시 레이어), JCache API (javax.cache)
    compileOnly(Libs.caffeine)
    compileOnly(Libs.caffeine_jcache)

    // Codecs
    compileOnly(Libs.fory_kotlin)
    compileOnly(Libs.kryo5)

    compileOnly(Libs.lz4_java)
    compileOnly(Libs.zstd_jni)

    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
