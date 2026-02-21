configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    testImplementation(project(":bluetape4k-junit5"))
}

