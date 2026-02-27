configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-cache-core"))
    api(project(":bluetape4k-cache-local"))
    api(project(":bluetape4k-cache-redisson"))
    api(project(":bluetape4k-cache-redisson-near"))
    api(project(":bluetape4k-cache-hazelcast"))
    api(project(":bluetape4k-cache-hazelcast-near"))
    api(project(":bluetape4k-cache-ignite"))
    api(project(":bluetape4k-cache-ignite-near"))

    testImplementation(testFixtures(project(":bluetape4k-cache-core")))
    testImplementation(project(":bluetape4k-netty"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Codecs
    testImplementation(Libs.fory_kotlin)
    testImplementation(Libs.kryo5)

    // Compressor
    testImplementation(Libs.lz4_java)
    testImplementation(Libs.snappy_java)
    testImplementation(Libs.zstd_jni)

    testImplementation(Libs.kotlinx_coroutines_test)

    testImplementation(Libs.springBootStarter("cache"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude("org.junit.vintage", "junit-vintage-engine")
        exclude("junit", "junit")
        exclude(group = "org.mockito", module = "mockito-core")
    }
}
