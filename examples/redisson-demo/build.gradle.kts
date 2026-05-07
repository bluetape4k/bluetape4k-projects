plugins {
    idea
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(enforcedPlatform(libs.spring.boot3.dependencies))
    
    // Redisson
    testImplementation(project(":bluetape4k-redisson"))
    testImplementation(libs.redisson)
    testImplementation(libs.redisson.spring.boot.starter)

    // Codecs
    testImplementation(libs.kryo)
    testImplementation(libs.fory.kotlin)  // new Apache Fory

    // Compressor
    testImplementation(libs.lz4.java)
    testImplementation(libs.snappy.java)
    testImplementation(libs.zstd.jni)

    // Protobuf
    testImplementation(project(":bluetape4k-protobuf"))

    // Cache
    testImplementation(project(":bluetape4k-cache-redisson"))
    testImplementation(libs.caffeine)
    testImplementation(libs.caffeine.jcache)

    // JSON
    testImplementation(project(":bluetape4k-jackson2"))
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.jackson.module.blackbird)
    testImplementation(libs.jackson.dataformat.protobuf)

    // Coroutines
    testImplementation(project(":bluetape4k-coroutines"))
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Bluetape4k Modules for Testing
    testImplementation(project(":bluetape4k-idgenerators"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Redisson Map Read/Write Through 예제를 위해 
    testImplementation(project(":bluetape4k-jdbc"))
    testRuntimeOnly(libs.h2.v2)
    testImplementation(libs.hikaricp)
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")

    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    // Redisson Cache Strategy 예제 — raw JetBrains Exposed 직접 참조
    testImplementation(libs.exposed.core)
    testImplementation(libs.exposed.dao)
    testImplementation(libs.exposed.jdbc)
    testImplementation(libs.exposed.java.time)
    testImplementation(libs.exposed.spring.boot.starter)
    testImplementation("org.springframework.boot:spring-boot-autoconfigure")
}
