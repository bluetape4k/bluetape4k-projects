configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-tenant"))
    api(libs.reactor.core)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.reactor.test)
}
