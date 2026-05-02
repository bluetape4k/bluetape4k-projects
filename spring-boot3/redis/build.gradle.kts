plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot3.dependencies))

    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))

    // Spring Data Redis
    api("org.springframework.boot:spring-boot-starter-data-redis")

    // Codecs
    compileOnly(libs.fory.kotlin)
    compileOnly(libs.kryo5)

    // Compressor
    compileOnly(libs.lz4.java)
    compileOnly(libs.zstd.jni)
    compileOnly(libs.snappy.java)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
}
