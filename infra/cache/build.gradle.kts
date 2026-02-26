configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-cache-core"))
    api(project(":bluetape4k-cache-local"))
    api(project(":bluetape4k-cache-redisson"))
    api(project(":bluetape4k-cache-hazelcast"))
    api(project(":bluetape4k-cache-ignite"))

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
    }
}

// Apache Ignite 2.x CachingProvider 로딩 시 Java 11+ 모듈 접근 허용이 필요합니다.
tasks.test {
    jvmArgs(
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
    )
}
