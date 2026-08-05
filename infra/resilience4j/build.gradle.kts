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
    api(bt4k.resilience4j.all)
    api(bt4k.resilience4j.cache)
    api(bt4k.resilience4j.kotlin)
    compileOnly(bt4k.resilience4j.reactor)
    compileOnly(bt4k.resilience4j.micrometer)

    // Coroutines
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // JCache for Resilience4j Cache
    testImplementation(bt4k.caffeine.jcache)
    testImplementation(bt4k.cache2k.jcache)
    testImplementation(bt4k.redisson)

    // Serializer
    testImplementation(bt4k.fory.kotlin)
    testImplementation(bt4k.kryo5)

    // Compressor
    testImplementation(bt4k.at.yawk.lz4.java)
    testImplementation(bt4k.snappy.java)
    testImplementation(bt4k.zstd.jni)
}
