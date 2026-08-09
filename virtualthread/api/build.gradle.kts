configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(project(":bluetape4k-logging"))
    testImplementation(project(":bluetape4k-junit5"))
    // Java 21 compatibility island의 test runtime provider
    testRuntimeOnly(project(":bluetape4k-virtualthread-jdk21"))
}
