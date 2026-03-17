configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-cache-lettuce"))
    api(project(":bluetape4k-lettuce"))
    api(project(":bluetape4k-exposed-jdbc"))
    api(project(":bluetape4k-resilience4j"))
    api(Libs.resilience4j_retry)

    // Exposed
    api(Libs.exposed_core)
    api(Libs.exposed_dao)
    api(Libs.exposed_jdbc)
    api(Libs.exposed_java_time)

    // Lettuce
    api(Libs.lettuce_core)

    // Serializer (LettuceLoadedMap에서 사용하는 codec용)
    compileOnly(Libs.fory_kotlin)
    compileOnly(Libs.kryo5)

    // Compressor
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.zstd_jni)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))

    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.kotlinx_coroutines_test)
}
