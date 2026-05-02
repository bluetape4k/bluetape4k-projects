plugins {
    kotlin("plugin.spring")
}

configurations {
    // compileOnly 나 runtimeOnly로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot3.dependencies))

    api(project(":bluetape4k-core"))
    implementation(project(":bluetape4k-cache-core"))
    testImplementation(project(":bluetape4k-http"))
    testImplementation(project(":bluetape4k-jackson2"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Micrometer
    api(libs.micrometer.core)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.micrometer.registry.datadog)
    testImplementation(libs.micrometer.test)

    api(libs.micrometer.observation)
    implementation(libs.micrometer.observation.test)

    // Micrometer Tracing
    implementation(libs.micrometer.tracing.bridge.otel)
    testImplementation(libs.micrometer.tracing.test)
    testImplementation(libs.micrometer.tracing.integration.test)

    api(libs.micrometer.context.propagation)  // thread local <-> reactor 등 상이한 환경에서 context 전파를 위해 사용

    // Instrumentations
    implementation(libs.cache2k.core)
    // 이미 cache2k_micrometer에 instrument 가 있지만, 예제용으로 만들기 위해 직접 구현했습니다.
    // compileOnly(libs.cache2k.micrometer)
    // compileOnly(libs.ignite.core)

    // Retrofit2 Instrumentations
    implementation(project(":bluetape4k-retrofit2"))
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.adapter.reactor)
    implementation(libs.retrofit2.adapter.rxjava2)
    implementation(libs.retrofit2.adapter.rxjava3)
    implementation(libs.retrofit2.converter.jackson)
    implementation(libs.okhttp3)


    // Jackson 2
    implementation(project(":bluetape4k-jackson2"))
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.module.blackbird)

    implementation(libs.vertx.core)
    testImplementation(project(":bluetape4k-vertx"))

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)
}
