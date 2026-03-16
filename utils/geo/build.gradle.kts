configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    // geocode: Google Maps Services
    compileOnly(project(":bluetape4k-jackson"))
    compileOnly(project(":bluetape4k-resilience4j"))
    compileOnly(project(":bluetape4k-feign"))
    compileOnly(Libs.feign_core)
    compileOnly(Libs.feign_kotlin)
    compileOnly(Libs.feign_slf4j)
    compileOnly(Libs.feign_jackson)
    compileOnly("com.google.maps:google-maps-services:2.2.0")
    compileOnly(Libs.httpclient5)
    compileOnly(Libs.httpclient5_cache)

    // geoip2: MaxMind GeoIP2
    compileOnly("com.maxmind.geoip2:geoip2:5.0.2")

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
