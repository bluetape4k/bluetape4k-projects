configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Nats
    api(Libs.jnats)
    api(Libs.nats_spring)
    compileOnly(Libs.nats_spring_cloud_stream_binder)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Json
    testImplementation(project(":bluetape4k-jackson"))
    testImplementation(Libs.jackson_databind)
    testImplementation(Libs.jackson_module_kotlin)
    testImplementation(Libs.jackson_module_parameter_names)
    testImplementation(Libs.jackson_module_blackbird)

    // Compressors
    testImplementation(Libs.lz4_java)
    testImplementation(Libs.snappy_java)
    testImplementation(Libs.zstd_jni)

    // Serializers
    testImplementation(Libs.kryo)
    testImplementation(Libs.fury_kotlin)

}
