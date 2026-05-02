configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    compileOnly(project(":bluetape4k-cache-core"))
    testImplementation(project(":bluetape4k-junit5"))

    api(libs.commons.math3)
    api(libs.commons.collections4)

    // Random Number Generator
    compileOnly(libs.commons.digest3)
    compileOnly(libs.commons.rng.simple)
}
