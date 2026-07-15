plugins {
    application
    alias(bt4k.plugins.kotlin.serialization)
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

application {
    mainClass.set("io.bluetape4k.examples.ktor.idgenerator.IdGeneratorKtorApplicationKt")
}

dependencies {
    implementation(project(":bluetape4k-idgenerators"))
    implementation(project(":bluetape4k-ktor-core"))
    implementation(project(":bluetape4k-ktor-observability"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)

    runtimeOnly(libs.logback.classic)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-ktor-testing"))
    testImplementation(libs.ktor.server.test.host)
}
