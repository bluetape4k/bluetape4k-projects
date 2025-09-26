plugins {
    idea
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Redisson
    api(project(":bluetape4k-redis"))
    api(Libs.redisson)
    api(Libs.redisson_spring_boot_starter)

    // Codecs
    implementation(Libs.kryo)
    implementation(Libs.fory_kotlin)  // new Apache Fory
    implementation(Libs.fury_kotlin)  // old Apache Fury

    // Compressor
    implementation(Libs.snappy_java)
    implementation(Libs.lz4_java)
    implementation(Libs.zstd_jni)

    // Protobuf
    implementation(Libs.protobuf_java)
    implementation(Libs.protobuf_java_util)
    implementation(Libs.protobuf_kotlin)

    // Cache
    implementation(project(":bluetape4k-cache"))
    implementation(Libs.caffeine)
    implementation(Libs.caffeine_jcache)

    // JSON
    implementation(project(":bluetape4k-jackson"))
    implementation(Libs.jackson_module_kotlin)
    implementation(Libs.jackson_module_blackbird)
    implementation(Libs.jackson_dataformat_protobuf)

    // Grpc
    implementation(project(":bluetape4k-grpc"))

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
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
