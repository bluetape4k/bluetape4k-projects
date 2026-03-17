configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-lettuce"))
    api(project(":bluetape4k-cache-lettuce"))
    api(project(":bluetape4k-exposed-r2dbc"))
    api(project(":bluetape4k-resilience4j"))
    api(Libs.resilience4j_retry)

    // Exposed R2DBC
    api(Libs.exposed_core)
    api(Libs.exposed_r2dbc)
    compileOnly(Libs.exposed_java_time)
    compileOnly(Libs.exposed_kotlin_datetime)

    // Lettuce
    api(Libs.lettuce_core)

    // Serializer (LettuceLoadedMap 코덱용)
    compileOnly(Libs.fory_kotlin)
    compileOnly(Libs.kryo5)

    // Compressor
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.zstd_jni)

    // Coroutines (R2DBC suspend 브리징)
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactive)

    // R2DBC drivers (test)
    testRuntimeOnly(Libs.r2dbc_h2)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(project(":bluetape4k-idgenerators"))
}
