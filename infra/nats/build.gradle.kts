configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // NATS
    api(libs.jnats)
    // nats-spring은 Spring Boot 3/4 통합 시 사용자가 직접 선언 (compileOnly로 API만 노출)
    compileOnly(libs.nats.spring)
    // nats_spring_cloud_stream_binder: 사용하지 않으므로 제외

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Jackson (테스트 페이로드 인코딩용) — bluetape4k-jackson2가 나머지 전이 포함
    testImplementation(project(":bluetape4k-jackson2"))
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.module.kotlin)
    compileOnly(libs.jackson.module.blackbird)

    // Compressors / Serializers (테스트 페이로드용)
    testImplementation(libs.lz4.java)
    testImplementation(libs.snappy.java)
    testImplementation(libs.zstd.jni)
    testImplementation(libs.kryo5)
    testImplementation(libs.fory.kotlin)
}
