configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-protobuf"))
    api(project(":bluetape4k-jackson2"))
    api(project(":bluetape4k-idgenerators"))
    compileOnly(project(":bluetape4k-hibernate"))
    compileOnly(project(":bluetape4k-cache-core"))

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Javers
    api("org.javers:javers-core:7.7.0")
    api(project(":bluetape4k-javers-core"))
    // bluetape4k-javers-core 의 테스트 코드를 재활용하기 위해 참조합니다.
    testImplementation(project(path = ":bluetape4k-javers-core", configuration = "testJar"))

    // Redis
    compileOnly(project(":bluetape4k-lettuce"))
    compileOnly(project(":bluetape4k-redisson"))

    // Codec
    compileOnly(libs.fory.kotlin)  // new Apache Fory
    compileOnly(libs.kryo5)

    // Compressor
    compileOnly(libs.lz4.java)
    compileOnly(libs.snappy.java)
    compileOnly(libs.zstd.jni)
}
