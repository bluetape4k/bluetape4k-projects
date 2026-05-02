configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-lettuce"))
    api(project(":bluetape4k-cache-lettuce"))
    api(project(":bluetape4k-exposed-r2dbc"))
    api(project(":bluetape4k-exposed-cache"))
    api(project(":bluetape4k-resilience4j"))
    api(libs.resilience4j.retry)

    // Exposed R2DBC
    api(libs.exposed.core)
    api(libs.exposed.r2dbc)
    compileOnly(libs.exposed.java.time)
    compileOnly(libs.exposed.kotlin.datetime)

    // Lettuce
    api(libs.lettuce.core)

    // Serializer (LettuceLoadedMap 코덱용)
    compileOnly(libs.fory.kotlin)
    compileOnly(libs.kryo5)

    // Compressor
    compileOnly(libs.snappy.java)
    compileOnly(libs.lz4.java)
    compileOnly(libs.zstd.jni)

    // Coroutines (R2DBC suspend 브리징)
    implementation(project(":bluetape4k-coroutines"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactive)

    // R2DBC drivers (test)
    testRuntimeOnly(libs.r2dbc.h2)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))
    testImplementation(testFixtures(project(":bluetape4k-exposed-cache")))
    testImplementation(libs.h2.v2)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":bluetape4k-idgenerators"))
}
