configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencyManagement {
    imports {
        mavenBom(bt4k.opentelemetry.alpha.bom.get().toString())
        mavenBom(bt4k.opentelemetry.instrumentation.bom.alpha.get().toString())
        mavenBom(bt4k.opentelemetry.bom.get().toString())
    }
}

dependencies {
    implementation(platform(bt4k.micrometer.bom))
    implementation(platform(bt4k.opentelemetry.instrumentation.bom.alpha))
    implementation(enforcedPlatform(bt4k.opentelemetry.bom))
    implementation(enforcedPlatform(bt4k.opentelemetry.alpha.bom))

    api(project(":bluetape4k-ktor-core"))
    api(libs.ktor.server.core)
    api(libs.ktor.server.call.id)
    api(libs.ktor.server.call.logging)
    api(libs.ktor.server.metrics.micrometer)
    api(libs.micrometer.core)
    api(bt4k.opentelemetry.api)

    compileOnly(libs.micrometer.registry.prometheus)
    compileOnly(libs.opentelemetry.ktor)
    compileOnly(libs.opentelemetry.sdk)
    testImplementation(libs.opentelemetry.sdk.testing)
    testImplementation(libs.opentelemetry.sdk.trace)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.ktor.server.test.host)
}
