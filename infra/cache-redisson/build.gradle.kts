configurations {
    // compileOnly 나 runtimeOnly로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.spring_boot3_dependencies))
    api(project(":bluetape4k-cache-core"))

    // Redisson JCache provider
    api(Libs.redisson)
    api(project(":bluetape4k-redisson"))
    // bluetape4k-resilience4j는 compileOnly(cache-redisson) 의존으로 순환 의존성 발생 → 직접 라이브러리 사용
    implementation(Libs.resilience4j_retry)
    implementation(Libs.resilience4j_kotlin)

    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.awaitility_kotlin)

    testImplementation(testFixtures(project(":bluetape4k-cache-core")))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.springBootStarter("cache"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude("org.junit.vintage", "junit-vintage-engine")
        exclude("junit", "junit")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    testRuntimeOnly(Libs.fory_kotlin)
    testRuntimeOnly(Libs.kryo5)

    testRuntimeOnly(Libs.lz4_java)
    testRuntimeOnly(Libs.snappy_java)
    testRuntimeOnly(Libs.zstd_jni)
}
