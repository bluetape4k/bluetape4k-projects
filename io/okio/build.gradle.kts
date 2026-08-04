configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))

    compileOnly(project(":bluetape4k-tink"))
    testImplementation(project(":bluetape4k-junit5"))

    // Okio
    api(bt4k.okio)

    // Apache Commons (base64)
    compileOnly(bt4k.commons.codec)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Compression (compress/tink 테스트에서 Compressors 사용)
    testImplementation(bt4k.commons.compress)
    testImplementation(bt4k.at.yawk.lz4.java)
    testImplementation(bt4k.snappy.java)
    testImplementation(bt4k.zstd.jni)
}
