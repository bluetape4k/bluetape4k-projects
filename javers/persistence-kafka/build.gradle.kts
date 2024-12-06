configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-jackson"))
    api(project(":bluetape4k-kafka"))
    api(project(":bluetape4k-idgenerators"))

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_kafka)

    // Javers
    api(project(":bluetape4k-javers-core"))
    // bluetape4k-javers-core 의 테스트 코드를 재활용하기 위해 참조합니다.
    testImplementation(project(path = ":bluetape4k-javers-core", configuration = "testJar"))

    api(Libs.javers_core)
    testImplementation(Libs.guava)

    // Kafka
    api(Libs.kafka_clients)
    compileOnly(Libs.spring_kafka)

    // Codec
    api(Libs.kryo)
    api(Libs.fury)

    // Compressor
    api(Libs.lz4_java)
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.zstd_jni)
}
