plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
    all {
        // CVE-2025-12183 (CVSS 8.8) + CVE-2025-66566 (CVSS 8.2):
        // kafka-clients/spring-kafka/reactor-kafka 가 org.lz4:lz4-java 를 transitively 가져온다.
        // at.yawk.lz4:lz4-java:1.11.0 (net.jpountz.lz4.* 동일 namespace) 로 대체한다.
        exclude(group = "org.lz4", module = "lz4-java")
    }
}

dependencies {
    implementation(platform(Libs.spring_boot3_dependencies))
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
    implementation(project(":bluetape4k-spring-boot3-core"))
    implementation(Libs.springData("commons"))

    // Jackson
    implementation(project(":bluetape4k-jackson2"))
    implementation(Libs.jackson_databind)
    implementation(Libs.jackson_module_kotlin)
    implementation(Libs.jackson_module_blackbird)

    // Codecs
    compileOnly(Libs.kryo)
    compileOnly(Libs.fory_kotlin)  // new Apache Fory

    // Compressors
    compileOnly(Libs.commons_compress)
    compileOnly(Libs.snappy_java)
    // at.yawk.lz4:lz4-java 를 api 로 노출: exclude 로 org.lz4 를 제거했으므로
    // 소비자 classpath 에 at.yawk.lz4:lz4-java:1.11.0 가 반드시 있어야 kafka LZ4 codec 이 동작한다.
    api(Libs.lz4_java)
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
