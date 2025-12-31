configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.jackson3_bom))

    api(project(":bluetape4k-jackson3"))
    testImplementation(project(":bluetape4k-junit5"))

    // Jackson Dataformats Text
    compileOnly(Libs.jackson3_dataformat_csv)
    compileOnly(Libs.jackson3_dataformat_properties)
    compileOnly(Libs.jackson3_dataformat_yaml)
    compileOnly(Libs.jackson3_dataformat_toml)

    testImplementation(Libs.jsonpath)
    testImplementation(Libs.jsonassert)
}
