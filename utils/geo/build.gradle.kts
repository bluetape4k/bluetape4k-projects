configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    // geocode: Google Maps Services
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(project(":bluetape4k-resilience4j"))
    compileOnly(project(":bluetape4k-feign"))
    compileOnly(libs.feign.core)
    compileOnly(libs.feign.kotlin)
    compileOnly(libs.feign.slf4j)
    compileOnly(libs.feign.jackson)
    compileOnly(bt4k.google.maps.services)
    compileOnly(bt4k.httpclient5)
    compileOnly(libs.httpclient5.cache)

    // geoip2: MaxMind GeoIP2
    compileOnly(bt4k.maxmind.geoip2)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
