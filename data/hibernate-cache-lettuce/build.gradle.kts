plugins {
    kotlin("plugin.jpa")
    kotlin("plugin.allopen")
}

// JPA 엔티티 클래스 open 처리
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    // 기존 near cache 모듈 재사용
    api(project(":bluetape4k-cache-lettuce"))

    // bluetape4k-io: BinarySerializers (Fory/Kryo 직렬화)
    api(project(":bluetape4k-io"))

    // Serializer runtime dependencies (bluetape4k-io의 선택적 의존성)
    implementation(Libs.fory_kotlin)
    implementation(Libs.lz4_java)

    // Compressor runtime dependencies (bluetape4k-io의 선택적 의존성)
    implementation(Libs.snappy_java)
    implementation(Libs.zstd_jni)

    // bluetape4k-redis: LettuceBinaryCodec
    api(project(":bluetape4k-lettuce"))

    // Hibernate
    api(Libs.hibernate_core)

    // Test
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)
}
