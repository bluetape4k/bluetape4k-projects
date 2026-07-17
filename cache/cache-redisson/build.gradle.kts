configurations {
    // compileOnly 나 runtimeOnly로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(bt4k.spring.boot4.dependencies))
    api(project(":bluetape4k-cache-core"))

    // Redisson JCache provider
    api(bt4k.redisson)
    api(project(":bluetape4k-redisson"))
    // bluetape4k-resilience4j는 compileOnly(cache-redisson) 의존으로 순환 의존성 발생 → 직접 라이브러리 사용
    implementation(libs.resilience4j.retry)
    implementation(libs.resilience4j.kotlin)

    implementation(project(":bluetape4k-coroutines"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.awaitility.kotlin)

    testImplementation(testFixtures(project(":bluetape4k-cache-core")))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation("org.springframework.boot:spring-boot-starter-cache")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude("org.junit.vintage", "junit-vintage-engine")
        exclude("junit", "junit")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    testRuntimeOnly(bt4k.fory.kotlin)
    testRuntimeOnly(libs.kryo5)

    testRuntimeOnly(libs.lz4.java)
    testRuntimeOnly(libs.snappy.java)
    testRuntimeOnly(bt4k.zstd.jni)
}
