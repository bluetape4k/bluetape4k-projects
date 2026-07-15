plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    api(project(":bluetape4k-core"))
    compileOnly(project(":bluetape4k-cache-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Bucket4j
    api(libs.bucket4j.core)
    compileOnly(libs.bucket4j.lettuce)
    compileOnly(libs.bucket4j.redisson)

    // Local Cache
    compileOnly(libs.caffeine)

    // Redis
    compileOnly(libs.lettuce.core)
    compileOnly(bt4k.redisson)

    // Codecs
    testImplementation(bt4k.fory.kotlin)
    testImplementation(libs.kryo5)

    // Compressor
    testImplementation(libs.lz4.java)
    testImplementation(libs.snappy.java)
    testImplementation(bt4k.zstd.jni)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Testcontainers for Redis
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers)

    // Spring Boot Example
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    // Reactor
    testImplementation(libs.reactor.netty)
    testImplementation(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)
}
