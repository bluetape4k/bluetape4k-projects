configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    testImplementation(project(":bluetape4k-mutiny"))
    testImplementation(project(":bluetape4k-junit5"))

    // Smallrye Mutiny
    testImplementation(libs.mutiny)
    testImplementation(libs.mutiny.kotlin)

    // Coroutines
    testImplementation(project(":bluetape4k-coroutines"))
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
