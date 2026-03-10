configurations {
    // compileOnly 나 runtimeOnly로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-cache-core"))
    api(project(":bluetape4k-lettuce"))

    // Lettuce JCache provider
    api(Libs.lettuce_core)

    // NearCache front tier (Caffeine)
    api(Libs.caffeine)

    implementation(project(":bluetape4k-coroutines"))
    implementation(project(":bluetape4k-resilience4j"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactive)
    testImplementation(Libs.kotlinx_coroutines_test)

    testImplementation(testFixtures(project(":bluetape4k-cache-core")))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    implementation(project(":bluetape4k-protobuf"))
    implementation(project(":bluetape4k-io"))

    testImplementation(Libs.awaitility_kotlin)

    testRuntimeOnly(Libs.fory_kotlin)
    testRuntimeOnly(Libs.kryo5)

    testRuntimeOnly(Libs.lz4_java)
    testRuntimeOnly(Libs.snappy_java)
    testRuntimeOnly(Libs.zstd_jni)
}
