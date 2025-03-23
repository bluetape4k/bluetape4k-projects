dependencyManagement {
    imports {
        mavenBom(Libs.jackson_bom)
        mavenBom(Libs.testcontainers_bom)
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.jackson_bom))
    
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    // Jackson
    api(project(":bluetape4k-jackson"))
    // Jackson Dataformats Text
    compileOnly(Libs.jackson_dataformat_csv)
    compileOnly(Libs.jackson_dataformat_properties)
    compileOnly(Libs.jackson_dataformat_yaml)
    compileOnly(Libs.jackson_dataformat_toml)

    testImplementation(Libs.jsonpath)
    testImplementation(Libs.jsonassert)
}
