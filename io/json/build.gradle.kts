configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(bt4k.jakarta.json.api)
    implementation(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))
}
