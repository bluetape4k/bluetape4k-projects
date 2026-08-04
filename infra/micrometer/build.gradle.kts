plugins {
    kotlin("plugin.spring")
}

configurations {
    // compileOnly 나 runtimeOnly로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(bt4k.spring.boot4.dependencies))

    api(project(":bluetape4k-core"))
    implementation(project(":bluetape4k-cache-core"))
    testImplementation(project(":bluetape4k-http"))
    testImplementation(project(":bluetape4k-jackson3"))
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
    implementation(bt4k.micrometer.tracing.bridge.otel)
    testImplementation(bt4k.micrometer.tracing.test)
    testImplementation(bt4k.micrometer.tracing.integration.test)

    api(bt4k.micrometer.context.propagation)  // thread local <-> reactor 등 상이한 환경에서 context 전파를 위해 사용

    // Instrumentations
    implementation(bt4k.cache2k.core)
    // 이미 cache2k_micrometer에 instrument 가 있지만, 예제용으로 만들기 위해 직접 구현했습니다.
    // compileOnly(bt4k.cache2k.micrometer)
    // compileOnly(libs.ignite.core)

    // Retrofit2 Instrumentations
    implementation(project(":bluetape4k-retrofit2"))
    implementation(bt4k.retrofit2)
    implementation(bt4k.retrofit2.adapter.reactor)
    implementation(bt4k.retrofit2.adapter.rxjava2)
    implementation(bt4k.retrofit2.adapter.rxjava3)
    implementation(bt4k.retrofit2.converter.jackson)
    implementation(bt4k.okhttp3)


    // Jackson 2
    implementation(project(":bluetape4k-jackson3"))
    implementation(libs.jackson3.module.kotlin)
    implementation(libs.jackson3.module.blackbird)

    implementation(bt4k.vertx.core)
    testImplementation(project(":bluetape4k-vertx"))

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)
}
