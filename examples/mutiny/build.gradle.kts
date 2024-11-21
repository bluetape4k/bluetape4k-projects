configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(project(":bluetape4k-mutiny"))
    testImplementation(project(":bluetape4k-junit5"))

    implementation(Libs.kotlinx_atomicfu)

    // Smallrye Mutiny
    implementation(Libs.mutiny)
    implementation(Libs.mutiny_kotlin)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
