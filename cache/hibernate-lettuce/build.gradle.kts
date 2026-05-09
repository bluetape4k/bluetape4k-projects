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

// Hibernate ORM 7.x requires Jakarta Persistence 3.2.0
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "jakarta.persistence") {
            useVersion("3.2.0")
            because("Hibernate ORM 7.x requires Jakarta Persistence 3.2.0")
        }
    }
}

dependencies {
    // 기존 near cache 모듈 재사용
    api(project(":bluetape4k-cache-lettuce"))

    // bluetape4k-io: BinarySerializers (Fory/Kryo 직렬화)
    api(project(":bluetape4k-io"))

    // Serializer runtime dependencies (bluetape4k-io의 선택적 의존성)
    implementation(libs.fory.kotlin)
    implementation(libs.lz4.java)

    // Compressor runtime dependencies (bluetape4k-io의 선택적 의존성)
    implementation(libs.snappy.java)
    implementation(libs.zstd.jni)

    // bluetape4k-redis: LettuceBinaryCodec
    api(project(":bluetape4k-lettuce"))

    // Hibernate
    api(libs.hibernate.core)

    // Test
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers)
    testImplementation(libs.h2.v2)
    testImplementation(libs.hikaricp)
}
