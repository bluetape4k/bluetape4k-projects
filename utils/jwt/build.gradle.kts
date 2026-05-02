plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}


dependencies {
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    api(libs.jjwt.api)
    api(libs.jjwt.impl)
    api(libs.jjwt.jackson)

    // Jackson
    api(project(":bluetape4k-jackson2"))
    api(libs.jackson.module.kotlin)
    api(libs.jackson.module.blackbird)

    // Serializer
    compileOnly(libs.fory.kotlin)
    compileOnly(libs.kryo5)

    // Compressor
    compileOnly(libs.lz4.java)
    compileOnly(libs.snappy.java)
    compileOnly(libs.zstd.jni)

    // Caching
    compileOnly(project(":bluetape4k-cache-redisson"))
    testImplementation(libs.caffeine.jcache)
    testImplementation(libs.ehcache)

    // Id Generators
    api(project(":bluetape4k-idgenerators"))
    api(libs.java.uuid.generator)

    // KeyChain을 Redis 나 MongoDB에 저장하여, 다중서버가 공유하기 위한 KeyChainPersister 를 사용하기 위해
    compileOnly(libs.redisson)
    compileOnly(libs.mongodb.driver.sync)
    compileOnly(libs.mongodb.driver.reactivestreams)
    compileOnly(libs.mongodb.driver.kotlin.sync)
    compileOnly(libs.mongodb.driver.kotlin.coroutine)

    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.mongodb)
}
