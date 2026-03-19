configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    testImplementation(project(":bluetape4k-mutiny"))
    testImplementation(project(":bluetape4k-junit5"))

    // Smallrye Mutiny
    testImplementation(Libs.mutiny)
    testImplementation(Libs.mutiny_kotlin)

    // Coroutines
    testImplementation(project(":bluetape4k-coroutines"))
    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
