configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-jackson"))
    api(project(":bluetape4k-resilience4j"))
    testImplementation(project(":bluetape4k-junit5"))

    // Google Maps Services
    // https://github.com/googlemaps/google-maps-services-java
    // https://mvnrepository.com/artifact/com.google.maps/google-maps-services
    api("com.google.maps:google-maps-services:2.2.0")

    // Bing Map Services (REST API로 사용하므로 feign 을 사용한다)
    api(project(":bluetape4k-feign"))
    api(Libs.feign_core)
    api(Libs.feign_kotlin)
    api(Libs.feign_slf4j)
    api(Libs.feign_jackson)

    api(Libs.httpclient5)
    api(Libs.httpclient5_cache)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
