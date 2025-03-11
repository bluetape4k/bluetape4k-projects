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
    api(Libs.kafka_streams)
    compileOnly(Libs.kafka_generator)
    testImplementation(Libs.kafka_streams_test_utils)
    testImplementation(Libs.kafka_server_common)
    testImplementation(Libs.testcontainers_kafka)

    // Spring Kafka
    api(Libs.spring_kafka)
    compileOnly(Libs.spring_kafka_test)
    api(project(":bluetape4k-spring-core"))
    compileOnly(Libs.springData("commons"))

    // Jackson
    api(project(":bluetape4k-jackson"))
    api(Libs.jackson_databind)
    api(Libs.jackson_module_kotlin)
    api(Libs.jackson_module_blackbird)
    api(Libs.javax_xml_bind)         // jackson findModules 에서 xml 관련 모듈도 등록할 때 필요하다

    // Codecs
    compileOnly(Libs.kryo)
    compileOnly(Libs.fury_kotlin)

    // Compressors
    compileOnly(Libs.commons_compress)
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.zstd_jni)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    compileOnly(Libs.reactor_kafka)
    compileOnly(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)

    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
}
