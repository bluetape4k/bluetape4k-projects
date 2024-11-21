plugins {
    kotlin("plugin.spring")
    id(Plugins.spring_boot)
}

springBoot {
    mainClass.set("io.bluetape4k.workshop.redis.cache.RedisCacheApplicationKt")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(project(":bluetape4k-redis"))
    implementation(project(":bluetape4k-spring-core"))
    testImplementation(project(":bluetape4k-junit5"))
    implementation(project(":bluetape4k-testcontainers"))

    // Codecs
    implementation(Libs.kryo)
    implementation(Libs.fury)

    // Compressor
    implementation(Libs.commons_compress)
    implementation(Libs.lz4_java)
    implementation(Libs.snappy_java)
    implementation(Libs.zstd_jni)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    implementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)

    // Lettuce
    implementation(Libs.lettuce_core)
    implementation(Libs.commons_pool2)

    // Spring Boot
    implementation(Libs.springBoot("autoconfigure"))
    annotationProcessor(Libs.springBoot("autoconfigure-processor"))
    annotationProcessor(Libs.springBoot("configuration-processor"))

    implementation(Libs.springBootStarter("cache"))
    implementation(Libs.springBootStarter("data-redis"))

    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
}
