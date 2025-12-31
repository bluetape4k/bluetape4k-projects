configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(Libs.jakarta_json_api)
    implementation(project(":bluetape4k-core"))
}
