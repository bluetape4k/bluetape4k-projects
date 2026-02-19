plugins {
    // Spring 관련 Plugin 은 spring-cloud-openfeign 예제를 위한 것입니다.
    kotlin("plugin.spring")
    // id(Plugins.spring_boot)
}

//tasks.bootJar {
//    enabled = false
//}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-http"))
    api(project(":bluetape4k-netty"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // https://mvnrepository.com/artifact/javax.ws.rs/javax.ws.rs-api
    // feign 12.3 에서는 아직 javax.ws.rs-api 를 사용합니다.
    // api(Libs.javax_ws_rs_api)

    // Feign
    api(Libs.feign_core)
    api(Libs.feign_hc5)
    api(Libs.feign_kotlin)
    api(Libs.feign_slf4j)
    api(Libs.feign_jackson)
    compileOnly(Libs.feign_reactive_wrappers)
    compileOnly(Libs.feign_micrometer)
    compileOnly(Libs.feign_jaxrs)
    compileOnly(Libs.feign_jaxrs2)
    testImplementation(Libs.feign_mock)

    // OkHttp3
    compileOnly(Libs.okhttp3)
    compileOnly(Libs.okhttp3_logging_interceptor)

    // OkHttp3 MockWebServer
    testImplementation(Libs.okhttp3_mockwebserver)

    // Apache HttpCompoents HttpClient 5
    // feign_hc5 를 사용하려면, httpcore5, httpcore5-h2 도 버전을 맞춰줘야 한다 
    api(Libs.httpclient5)
    api(Libs.httpclient5_cache)

    // Vertx
    compileOnly(project(":bluetape4k-vertx-core"))
    compileOnly(Libs.vertx_core)
    compileOnly(Libs.vertx_lang_kotlin)
    compileOnly(Libs.vertx_lang_kotlin_coroutines)

    // Jackson (2.14 와 2.13 이 혼용되어서 jackson-core, jackson-databind 를 모두 지정해주어야 한다)
    api(project(":bluetape4k-jackson"))
    api(Libs.jackson_core)
    api(Libs.jackson_databind)
    api(Libs.jackson_module_kotlin)
    compileOnly(Libs.jackson_module_blackbird)

    // Fastjson2
    compileOnly(project(":bluetape4k-fastjson2"))
    compileOnly(Libs.fastjson2)
    compileOnly(Libs.fastjson2_kotlin)

    // Gson
//    compileOnly(Libs.gson)
//    compileOnly(Libs.gson_javatime_serializers)

    // Resilience4j
    compileOnly(project(":bluetape4k-resilience4j"))
    compileOnly(Libs.resilience4j_all)
    compileOnly(Libs.resilience4j_kotlin)
    compileOnly(Libs.resilience4j_feign)
    compileOnly(Libs.resilience4j_cache)

    //
    // Spring Cloud OpenFeign 사용
    //
    testImplementation(Libs.springCloudStarter("openfeign"))
    testImplementation(Libs.springBootStarter("webflux"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
}
