configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    // Jackson
    testImplementation(project(":bluetape4k-jackson2"))
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.jackson.module.blackbird)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
