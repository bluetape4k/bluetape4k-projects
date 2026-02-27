plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}


dependencies {
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    api(Libs.jjwt_api)
    api(Libs.jjwt_impl)
    api(Libs.jjwt_jackson)

    // Jackson
    api(project(":bluetape4k-jackson"))
    api(Libs.jackson_module_kotlin)
    api(Libs.jackson_module_blackbird)

    // Serializer
    compileOnly(Libs.fory_kotlin)
    compileOnly(Libs.kryo5)

    // Compressor
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.zstd_jni)

    // Caching
    compileOnly(project(":bluetape4k-cache-local"))
    testImplementation(Libs.caffeine_jcache)
    testImplementation(Libs.ehcache)

    // Id Generators
    api(project(":bluetape4k-idgenerators"))
    api(Libs.java_uuid_generator)

    // KeyChain을 Redis 나 MongoDB에 저장하여, 다중서버가 공유하기 위한 KeyChainPersister 를 사용하기 위해
    compileOnly(Libs.redisson)
    compileOnly(Libs.mongodb_driver_sync)
    compileOnly(Libs.mongodb_driver_reactivestreams)
    compileOnly(Libs.mongodb_driver_kotlin_sync)
    compileOnly(Libs.mongodb_driver_kotlin_coroutine)

    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_mongodb)
}
