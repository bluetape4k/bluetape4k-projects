configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    // Hashing
    api(libs.zero.allocation.hashing)

    // Redis Drivers
    compileOnly(project(":bluetape4k-lettuce"))
    compileOnly(project(":bluetape4k-redisson"))
    compileOnly(libs.lettuce.core)
    compileOnly(libs.redisson)

    // Codecs
    testImplementation(libs.fory.kotlin)
    testImplementation(libs.kryo5)

    // Compressor
    testImplementation(libs.lz4.java)
    testImplementation(libs.snappy.java)
    testImplementation(libs.zstd.jni)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // TestContainers
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers)
}
