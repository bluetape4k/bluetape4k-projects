configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Temporal
    api(platform(bt4k.temporal.bom))
    api(bt4k.temporal.sdk)
    api(bt4k.temporal.kotlin)
    testImplementation(bt4k.temporal.testing)

    // Bluetape4k
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
