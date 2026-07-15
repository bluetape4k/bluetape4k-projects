configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.micrometer.bom))
    implementation(platform(bt4k.opentelemetry.instrumentation.bom.alpha))

    api(project(":bluetape4k-ktor-core"))
    api(libs.ktor.server.core)
    api(libs.ktor.server.call.id)
    api(libs.ktor.server.call.logging)
    api(libs.ktor.server.metrics.micrometer)
    api(libs.micrometer.core)
    api(bt4k.opentelemetry.api)

    compileOnly(libs.micrometer.registry.prometheus)
    compileOnly(libs.opentelemetry.ktor)
    testImplementation(libs.micrometer.registry.prometheus)
    testImplementation(libs.opentelemetry.ktor)
    testImplementation(libs.opentelemetry.sdk)
    testImplementation(libs.opentelemetry.sdk.testing)
    testImplementation(libs.opentelemetry.sdk.trace)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.ktor.server.test.host)
}
