configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(libs.jakarta.json.api)
    implementation(project(":bluetape4k-core"))
}
