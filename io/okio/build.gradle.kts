configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    
    compileOnly(project(":bluetape4k-tink"))
    testImplementation(project(":bluetape4k-junit5"))

    // Okio
    api(libs.okio)

    // Apache Commons (base64)
    compileOnly(libs.commons.codec)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Compression (compress/tink 테스트에서 Compressors 사용)
    testImplementation(libs.commons.compress)
    testImplementation(libs.lz4.java)
    testImplementation(libs.snappy.java)
    testImplementation(libs.zstd.jni)
}
