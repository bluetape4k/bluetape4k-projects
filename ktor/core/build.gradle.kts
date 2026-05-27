plugins {
    alias(libs.plugins.kotlin.serialization)
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(libs.ktor.server.core)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.server.status.pages)
    api(libs.ktor.serialization.kotlinx.json)
    api(libs.kotlinx.serialization.json)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.ktor.server.test.host)
}
