plugins {
    idea
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(enforcedPlatform(bt4k.spring.boot4.dependencies))
    
    // Redisson
    testImplementation(project(":bluetape4k-redisson"))
    testImplementation(bt4k.redisson)
    testImplementation(libs.redisson.spring.boot.starter)

    // Codecs
    testImplementation(libs.kryo)
    testImplementation(bt4k.fory.kotlin)  // new Apache Fory

    // Compressor
    testImplementation(libs.lz4.java)
    testImplementation(libs.snappy.java)
    testImplementation(bt4k.zstd.jni)

    // Protobuf
    testImplementation(project(":bluetape4k-protobuf"))

    // Cache
    testImplementation(project(":bluetape4k-cache-redisson"))
    testImplementation(libs.caffeine)
    testImplementation(libs.caffeine.jcache)

    // JSON
    testImplementation(project(":bluetape4k-jackson3"))
    testImplementation(libs.jackson3.module.kotlin)
    testImplementation(libs.jackson3.module.blackbird)
    testImplementation(libs.jackson3.dataformat.protobuf)

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
    testImplementation(bt4k.hikaricp)
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")

    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    // Redisson Cache Strategy 예제 — raw JetBrains Exposed 직접 참조
    testImplementation(bt4k.exposed.core)
    testImplementation(libs.exposed.dao)
    testImplementation(bt4k.exposed.jdbc)
    testImplementation(bt4k.exposed.java.time)
    testImplementation(bt4k.exposed.spring.boot4.starter)
    testImplementation("org.springframework.boot:spring-boot-autoconfigure")
}
