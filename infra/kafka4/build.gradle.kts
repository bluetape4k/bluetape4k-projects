plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
}

tasks.withType<Test> {
    systemProperty("java.io.tmpdir", temporaryDir)
    systemProperty("spring.kafka.streams.state-dir", temporaryDir.resolve("kafka-streams"))
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
    all {
        // CVE-2025-12183 (CVSS 8.8) + CVE-2025-66566 (CVSS 8.2):
        // kafka-clients/spring-kafka/reactor-kafka 가 org.lz4:lz4-java 를 transitively 가져온다.
        // at.yawk.lz4:lz4-java:1.11.0 (net.jpountz.lz4.* 동일 namespace) 로 대체한다.
        exclude(group = "org.lz4", module = "lz4-java")

        resolutionStrategy.eachDependency {
            if (requested.group == "org.apache.kafka") {
                useVersion(bt4k.versions.kafka4.get())
                because("kafka4 모듈: root BOM의 kafka3.x 다운그레이드 방지")
            }
        }
    }
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    api(project(":bluetape4k-annotations"))
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-resilience4j"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Kafka (kafka4: 4.2.x — spring-kafka 4.x compatible)
    api(bt4k.kafka4.clients)
    compileOnly(bt4k.kafka4.streams)
    compileOnly(bt4k.kafka4.generator)
    testImplementation(bt4k.kafka4.streams.test.utils)
    testRuntimeOnly(bt4k.kafka4.server.common)
    testImplementation(libs.testcontainers.kafka)

    // Spring Kafka
    implementation(bt4k.spring.kafka4)
    compileOnly(bt4k.spring.kafka4.test)
    implementation(project(":bluetape4k-spring-boot-core"))
    implementation("org.springframework.data:spring-data-commons")

    // Jackson 3
    implementation(project(":bluetape4k-jackson3"))
    implementation(libs.jackson3.databind)
    implementation(libs.jackson3.module.kotlin)
    implementation(libs.jackson3.module.blackbird)

    // Codecs
    compileOnly(libs.kryo5)
    compileOnly(bt4k.fory.kotlin)  // new Apache Fory

    // Compressors
    compileOnly(bt4k.commons.compress)
    compileOnly(libs.snappy.java)
    // at.yawk.lz4:lz4-java 를 api 로 노출: exclude 로 org.lz4 를 제거했으므로
    // 소비자 classpath 에 at.yawk.lz4:lz4-java:1.11.0 가 반드시 있어야 kafka LZ4 codec 이 동작한다.
    api(libs.lz4.java)
    compileOnly(bt4k.zstd.jni)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Reactor
    implementation(libs.reactor.kafka4)
    implementation(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
}
