plugins {
    idea
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Redisson
    testImplementation(project(":bluetape4k-redis"))
    testImplementation(Libs.redisson)
    testImplementation(Libs.redisson_spring_boot_starter)

    // Codecs
    testImplementation(Libs.kryo)
    testImplementation(Libs.fory_kotlin)  // new Apache Fory
    testImplementation(Libs.fury_kotlin)  // old Apache Fury

    // Compressor
    testImplementation(Libs.lz4_java)
    testImplementation(Libs.snappy_java)
    testImplementation(Libs.zstd_jni)

    // Protobuf
    testImplementation(Libs.protobuf_java)
    testImplementation(Libs.protobuf_java_util)
    testImplementation(Libs.protobuf_kotlin)

    // Cache
    testImplementation(project(":bluetape4k-cache"))
    testImplementation(Libs.caffeine)
    testImplementation(Libs.caffeine_jcache)

    // JSON
    testImplementation(project(":bluetape4k-jackson"))
    testImplementation(Libs.jackson_module_kotlin)
    testImplementation(Libs.jackson_module_blackbird)
    testImplementation(Libs.jackson_dataformat_protobuf)

    // Grpc
    testImplementation(project(":bluetape4k-grpc"))

    // Coroutines
    testImplementation(project(":bluetape4k-coroutines"))
    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Bluetape4k Modules for Testing
    testImplementation(project(":bluetape4k-idgenerators"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Redisson Map Read/Write Through 예제를 위해 
    testImplementation(project(":bluetape4k-jdbc"))
    testRuntimeOnly(Libs.h2_v2)
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.springBootStarter("jdbc"))

    testImplementation(Libs.springBootStarter("data-redis"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    // Redisson Cache Strategy 예제를 위해
    testImplementation(project(":bluetape4k-exposed"))
    testImplementation(project(":bluetape4k-exposed-tests"))
    testImplementation(Libs.exposed_java_time)
    testImplementation(project(":bluetape4k-javatimes"))
    testImplementation(Libs.exposed_spring_boot_starter)
}
