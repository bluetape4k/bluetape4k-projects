configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-cache"))
    compileOnly(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Apache Ignite 2.x (임베디드 + 씬 클라이언트 모두 포함)
    api(Libs.ignite_core)

    // Front Cache (씬 클라이언트 모드의 Near Cache용)
    compileOnly(Libs.caffeine)

    // Codecs
    compileOnly(Libs.fory_kotlin)
    compileOnly(Libs.kryo5)

    compileOnly(Libs.lz4_java)
    compileOnly(Libs.zstd_jni)

    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
