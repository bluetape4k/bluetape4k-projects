plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}


dependencies {
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    api(bt4k.jjwt.api)
    api(bt4k.jjwt.impl)
    api(bt4k.jjwt.jackson)

    // Jackson
    api(project(":bluetape4k-jackson2"))
    api(libs.jackson.module.kotlin)
    api(libs.jackson.module.blackbird)

    // Serializer
    compileOnly(bt4k.fory.kotlin)
    compileOnly(bt4k.kryo5)

    // Compressor
    compileOnly(bt4k.at.yawk.lz4.java)
    compileOnly(bt4k.snappy.java)
    compileOnly(bt4k.zstd.jni)

    // Caching
    compileOnly(project(":bluetape4k-cache-redisson"))
    testImplementation(bt4k.caffeine.jcache)
    testImplementation(bt4k.ehcache)

    // Id Generators
    api(project(":bluetape4k-idgenerators"))
    api(bt4k.java.uuid.generator)

    // KeyChain을 Redis 나 MongoDB에 저장하여, 다중서버가 공유하기 위한 KeyChainPersister 를 사용하기 위해
    compileOnly(bt4k.redisson)
    compileOnly(bt4k.mongodb.driver.sync)
    compileOnly(bt4k.mongodb.driver.reactivestreams)
    compileOnly(bt4k.mongodb.driver.kotlin.sync)
    compileOnly(bt4k.mongodb.driver.kotlin.coroutine)

    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.toxiproxy)
    testImplementation(libs.testcontainers.mongodb)
}
