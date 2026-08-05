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
    compileOnly(bt4k.feign.core)
    compileOnly(bt4k.feign.kotlin)
    compileOnly(bt4k.feign.slf4j)
    compileOnly(bt4k.feign.jackson)
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
