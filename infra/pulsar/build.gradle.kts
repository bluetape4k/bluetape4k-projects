configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-coroutines"))

    // Pulsar — pulsar_client(구현체)를 사용해야 PulsarClient.builder().build() 가능
    api(libs.pulsar.client)

    // Jackson2 (compileOnly — 사용 모듈이 runtime에 implementation으로 선언 필요)
    compileOnly(project(":bluetape4k-jackson2"))
    compileOnly(libs.jackson.databind)

    // Jackson3 (compileOnly — 사용 모듈이 runtime에 implementation으로 선언 필요)
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(libs.jackson3.databind)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Testcontainers
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.pulsar)

    // 테스트용 Jackson — Schema 라운드트립
    testImplementation(project(":bluetape4k-jackson2"))
    testImplementation(project(":bluetape4k-jackson3"))
    testImplementation(libs.jackson.module.kotlin)
}
