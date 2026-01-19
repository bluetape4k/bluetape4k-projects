plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-resilience4j"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Kafka
    api(Libs.kafka_clients)
    compileOnly(Libs.kafka_streams)
    compileOnly(Libs.kafka_generator)
    testImplementation(Libs.kafka_streams_test_utils)
    testImplementation(Libs.kafka_server_common)
    testImplementation(Libs.testcontainers_kafka)

    // Spring Kafka
    implementation(Libs.spring_kafka)
    compileOnly(Libs.spring_kafka_test)
    implementation(project(":bluetape4k-spring-core"))
    implementation(Libs.springData("commons"))

    // Jackson
    implementation(project(":bluetape4k-jackson"))
    implementation(Libs.jackson_databind)
    implementation(Libs.jackson_module_kotlin)
    implementation(Libs.jackson_module_blackbird)

    // Codecs
    compileOnly(Libs.kryo)
    compileOnly(Libs.fory_kotlin)  // new Apache Fory

    // Compressors
    compileOnly(Libs.commons_compress)
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.zstd_jni)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    implementation(Libs.reactor_kafka)
    implementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)

    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
}
