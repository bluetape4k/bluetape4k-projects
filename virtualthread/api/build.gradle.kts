configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(project(":bluetape4k-logging"))
    testImplementation(project(":bluetape4k-junit5"))
}
