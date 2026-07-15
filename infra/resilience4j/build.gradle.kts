configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(project(":bluetape4k-cache-redisson"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Resilience4j
    api(libs.resilience4j.all)
    api(libs.resilience4j.cache)
    api(libs.resilience4j.kotlin)
    compileOnly(libs.resilience4j.reactor)
    compileOnly(libs.resilience4j.micrometer)

    // Coroutines
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // JCache for Resilience4j Cache
    testImplementation(libs.caffeine.jcache)
    testImplementation(libs.cache2k.jcache)
    testImplementation(bt4k.redisson)

    // Serializer
    testImplementation(bt4k.fory.kotlin)
    testImplementation(libs.kryo5)

    // Compressor
    testImplementation(libs.lz4.java)
    testImplementation(libs.snappy.java)
    testImplementation(bt4k.zstd.jni)
}
