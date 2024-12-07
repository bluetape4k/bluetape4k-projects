plugins {
    id(Plugins.quarkus)
    kotlin("plugin.allopen")
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // NOTE: Quarkus 는 꼭 gradle platform 으로 참조해야 제대로 빌드가 된다.
    implementation(enforcedPlatform(Libs.quarkus_bom))
    implementation(platform(Libs.quarkus_universe_bom))
    implementation(platform(Libs.resteasy_bom))

    api(project(":bluetape4k-quarkus-core"))
    testImplementation(project(":bluetape4k-quarkus-tests"))
    testImplementation(project(":bluetape4k-junit5"))


    // Quarkus 라이브러리 (https://quarkus.io/extensions/)
    // rest
    implementation(Libs.quarkus("rest"))
    implementation(Libs.quarkus("rest-kotlin"))
    implementation(Libs.quarkus("rest-jackson"))

    // rest client
    implementation(Libs.quarkus("rest-client"))
    implementation(Libs.quarkus("rest-client-jackson"))

    testImplementation(Libs.quarkus_junit5)
    testImplementation(Libs.rest_assured_kotlin)

    // coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactive)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Bluetape4k
    implementation(project(":bluetape4k-jackson"))
    testImplementation(project(":bluetape4k-idgenerators"))
}
