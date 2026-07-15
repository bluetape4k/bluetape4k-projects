plugins {
    alias(bt4k.plugins.kotlin.serialization)
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-ktor-core"))
    api(project(":bluetape4k-assertions"))
    api(libs.ktor.server.test.host)
    api(libs.ktor.client.core)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.client.mock)

    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(bt4k.kotlinx.serialization.json)

    testImplementation(project(":bluetape4k-junit5"))
}
