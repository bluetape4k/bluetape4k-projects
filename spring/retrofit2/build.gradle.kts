plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-micrometer"))
    testImplementation(project(":bluetape4k-junit5"))

    // Spring Framework
    api(project(":bluetape4k-spring-core"))
    api(Libs.spring("context-support"))
    compileOnly(Libs.micrometer_core)

    // Retrofit2
    api(project(":bluetape4k-retrofit2"))
    api(Libs.retrofit2)
    api(Libs.retrofit2_converter_jackson)
    api(Libs.retrofit2_converter_scalars)
    api(Libs.retrofit2_adapter_java8)
    compileOnly(Libs.retrofit2_adapter_reactor)
    // rxjava 는 더 이상 사용하지 않는다. (reactor 를 사용한다)
    // compileOnly(Libs.retrofit2_adapter_rxjava2)
    // compileOnly(Libs.retrofit2_adapter_rxjava3)
    testImplementation(Libs.retrofit2_mock)

    // OkHttp3
    api(Libs.okhttp3)
    compileOnly(Libs.okhttp3_logging_interceptor)

    // OkHttp3 MockWebServer
    testImplementation(Libs.okhttp3_mockwebserver)

    // Apache HttpCompoents HttpClient 5
    compileOnly(Libs.httpclient5)
    compileOnly(Libs.httpcore5_h2)
    compileOnly(Libs.httpcore5_reactive)

    // Vertx
    compileOnly(project(":bluetape4k-vertx-core"))
    compileOnly(Libs.vertx_core)
    compileOnly(Libs.vertx_lang_kotlin)
    compileOnly(Libs.vertx_lang_kotlin_coroutines)

    // Apache AsyncHttpClient
    compileOnly(Libs.async_http_client)
    compileOnly(Libs.async_http_client_extras_retrofit2)
    compileOnly(Libs.async_http_client_extras_rxjava2)

    // Jackson
    api(project(":bluetape4k-jackson"))
    api(Libs.jackson_core)
    api(Libs.jackson_databind)
    api(Libs.jackson_module_kotlin)

    // Gson
    compileOnly(Libs.gson)
    compileOnly(Libs.gson_javatime_serializers)

    // Resilience4j
    compileOnly(project(":bluetape4k-resilience4j"))
    compileOnly(Libs.resilience4j_all)
    compileOnly(Libs.resilience4j_kotlin)
    compileOnly(Libs.resilience4j_cache)
    compileOnly(Libs.resilience4j_retry)
    compileOnly(Libs.resilience4j_circuitbreaker)
    compileOnly(Libs.resilience4j_reactor)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactive)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Spring Cloud
    compileOnly(Libs.spring_cloud_starter_bootstrap)

    compileOnly(Libs.hibernate_validator)
    compileOnly(Libs.jakarta_el_api)

    // Spring Boot
    compileOnly(Libs.springBoot("autoconfigure"))
    compileOnly(Libs.springBoot("configuration-processor"))
    annotationProcessor(Libs.springBoot("configuration-processor"))

    testImplementation(Libs.springBootStarter("actuator"))
    testImplementation(Libs.micrometer_core)
    testImplementation(Libs.micrometer_registry_prometheus)

    testImplementation(Libs.springBootStarter("web"))
    testImplementation(Libs.springBootStarter("webflux"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
}
