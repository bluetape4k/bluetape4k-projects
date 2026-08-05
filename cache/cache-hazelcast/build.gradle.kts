configurations {
    // compileOnly 나 runtimeOnly로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-cache-core"))

    // Hazelcast JCache provider
    api(bt4k.hazelcast)

    // bluetape4k-resilience4j는 compileOnly(cache-redisson) 의존으로 순환 의존성 발생 → 직접 라이브러리 사용
    implementation(bt4k.resilience4j.retry)
    implementation(bt4k.resilience4j.kotlin)

    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.awaitility.kotlin)

    testImplementation(testFixtures(project(":bluetape4k-cache-core")))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
}
