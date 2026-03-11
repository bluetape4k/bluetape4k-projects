configurations {
    // compileOnly 나 runtimeOnly로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-cache-core"))

    // Apache Ignite 2.x JCache provider
    api(Libs.ignite_core)
    api(Libs.ignite_clients)

    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)

    testImplementation(testFixtures(project(":bluetape4k-cache-core")))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
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
        // Ignite 2.x thin client 가 synchronized 블록을 사용하여 VirtualThread가 carrier thread에
        // pin될 수 있으므로, VirtualThread 스케줄러 parallelism 을 늘려 carrier thread 고갈을 방지합니다.
        "-Djdk.virtualThreadScheduler.parallelism=64",
    )
}
