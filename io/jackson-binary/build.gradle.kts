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
    
    api(project(":bluetape4k-core"))
    compileOnly(project(":bluetape4k-jackson"))
    testImplementation(project(":bluetape4k-junit5"))

    // Jackson Dataformats Binary
    compileOnly(Libs.jackson_dataformat_avro)
    compileOnly(Libs.jackson_dataformat_cbor)
    compileOnly(Libs.jackson_dataformat_ion)
    compileOnly(Libs.jackson_dataformat_protobuf)
    compileOnly(Libs.jackson_dataformat_smile)

    compileOnly(Libs.jackson_module_kotlin)
    compileOnly(Libs.jackson_module_blackbird)

    testImplementation(Libs.jsonpath)
    testImplementation(Libs.jsonassert)
}
