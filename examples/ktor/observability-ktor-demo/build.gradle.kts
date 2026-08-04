plugins {
    application
    alias(bt4k.plugins.kotlin.serialization)
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

application {
    mainClass.set("io.bluetape4k.examples.ktor.observability.ObservabilityKtorApplicationKt")
}

dependencies {
    implementation(project(":bluetape4k-ktor-core"))
    implementation(project(":bluetape4k-ktor-observability"))
    implementation(project(":bluetape4k-micrometer"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.micrometer.registry.prometheus)
    implementation(bt4k.opentelemetry.api)

    runtimeOnly(bt4k.logback)
    runtimeOnly(libs.opentelemetry.ktor)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-ktor-testing"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.opentelemetry.sdk)
    testImplementation(libs.opentelemetry.sdk.testing)
    testImplementation(libs.opentelemetry.sdk.trace)
}
