configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    compileOnly(project(":bluetape4k-cache-core"))
    testImplementation(project(":bluetape4k-junit5"))

    api(bt4k.commons.math3)
    api(bt4k.commons.collections4)

    // Random Number Generator
    compileOnly(bt4k.commons.digest3)
    compileOnly(bt4k.commons.rng.simple)
}
