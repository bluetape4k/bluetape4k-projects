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
    api(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-jackson"))
    testImplementation(project(":bluetape4k-junit5"))

    api(Libs.jackson_core)
    api(Libs.jackson_databind)
    api(Libs.jackson_module_kotlin)
    api(Libs.jackson_module_parameter_names)
    api(Libs.jackson_module_blackbird)

    // Jackson Dataformats Text
    api(Libs.jackson_dataformat_csv)
    api(Libs.jackson_dataformat_properties)
    api(Libs.jackson_dataformat_yaml)

    testImplementation(Libs.jsonpath)
    testImplementation(Libs.jsonassert)
}
