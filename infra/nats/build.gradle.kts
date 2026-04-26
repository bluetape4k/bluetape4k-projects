configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // NATS
    api(Libs.jnats)
    // nats-spring은 Spring Boot 3/4 통합 시 사용자가 직접 선언 (compileOnly로 API만 노출)
    compileOnly(Libs.nats_spring)
    // nats_spring_cloud_stream_binder: 사용하지 않으므로 제외

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Jackson (테스트 페이로드 인코딩용) — bluetape4k-jackson2가 나머지 전이 포함
    testImplementation(project(":bluetape4k-jackson2"))
    testImplementation(Libs.jackson_databind)
    testImplementation(Libs.jackson_module_kotlin)
    compileOnly(Libs.jackson_module_blackbird)

    // Compressors / Serializers (테스트 페이로드용)
    testImplementation(Libs.lz4_java)
    testImplementation(Libs.snappy_java)
    testImplementation(Libs.zstd_jni)
    testImplementation(Libs.kryo5)
    testImplementation(Libs.fory_kotlin)
}
