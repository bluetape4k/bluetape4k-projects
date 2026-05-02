configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(libs.lingua)
    testImplementation(project(":bluetape4k-junit5"))
}
