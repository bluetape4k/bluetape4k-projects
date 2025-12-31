configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.jackson3_bom))

    api(project(":bluetape4k-jackson3"))
    testImplementation(project(":bluetape4k-junit5"))

    // Jackson Dataformats Binary
    compileOnly(Libs.jackson3_dataformat_avro)
    compileOnly(Libs.jackson3_dataformat_cbor)
    compileOnly(Libs.jackson3_dataformat_ion)
    compileOnly(Libs.jackson3_dataformat_protobuf)
    compileOnly(Libs.jackson3_dataformat_smile)

    testImplementation(Libs.jsonpath)
    testImplementation(Libs.jsonassert)
}
