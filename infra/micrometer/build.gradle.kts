plugins {
    kotlin("plugin.spring")
}

configurations {
    // compileOnly 나 runtimeOnly로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.spring_boot3_dependencies))

    api(project(":bluetape4k-core"))
    implementation(project(":bluetape4k-cache-core"))
    testImplementation(project(":bluetape4k-http"))
    testImplementation(project(":bluetape4k-jackson2"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Micrometer
    api(Libs.micrometer_core)
    implementation(Libs.micrometer_registry_prometheus)
    implementation(Libs.micrometer_registry_datadog)
    testImplementation(Libs.micrometer_test)

    api(Libs.micrometer_observation)
    implementation(Libs.micrometer_observation_test)

    // Micrometer Tracing
    implementation(Libs.micrometer_tracing_bridge_otel)
    testImplementation(Libs.micrometer_tracing_test)
    testImplementation(Libs.micrometer_tracing_integeration_test)

    api(Libs.micrometer_context_propagation)  // thread local <-> reactor 등 상이한 환경에서 context 전파를 위해 사용

    // Instrumentations
    implementation(Libs.cache2k_core)
    // 이미 cache2k_micrometer에 instrument 가 있지만, 예제용으로 만들기 위해 직접 구현했습니다.
    // compileOnly(Libs.cache2k_micrometer)
    // compileOnly(Libs.ignite_core)

    // Retrofit2 Instrumentations
    implementation(project(":bluetape4k-retrofit2"))
    implementation(Libs.retrofit2)
    implementation(Libs.retrofit2_adapter_reactor)
    implementation(Libs.retrofit2_adapter_rxjava2)
    implementation(Libs.retrofit2_adapter_rxjava3)
    implementation(Libs.retrofit2_converter_jackson)
    implementation(Libs.okhttp3)

    implementation(Libs.async_http_client_extras_retrofit2)

    // Jackson 2
    implementation(project(":bluetape4k-jackson2"))
    implementation(Libs.jackson_module_kotlin)
    implementation(Libs.jackson_module_blackbird)

    implementation(Libs.vertx_core)
    testImplementation(project(":bluetape4k-vertx"))

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)
}
