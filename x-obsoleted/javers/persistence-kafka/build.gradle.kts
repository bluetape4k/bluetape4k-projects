configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-jackson2"))
    api(project(":bluetape4k-kafka"))
    api(project(":bluetape4k-idgenerators"))

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.kafka)

    // Javers
    api(project(":bluetape4k-javers-core"))
    // bluetape4k-javers-core 의 테스트 코드를 재활용하기 위해 참조합니다.
    testImplementation(project(path = ":bluetape4k-javers-core", configuration = "testJar"))

    api("org.javers:javers-core:7.7.0")

    // Kafka
    api(libs.kafka.clients)
    compileOnly(libs.spring.kafka)

    // Codec
    compileOnly(libs.fory.kotlin)  // new Apache Fory
    compileOnly(libs.kryo5)

    // Compressor
    compileOnly(libs.lz4.java)
    compileOnly(libs.snappy.java)
    compileOnly(libs.zstd.jni)
}
