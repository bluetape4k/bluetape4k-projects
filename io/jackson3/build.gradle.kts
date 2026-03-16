configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.jackson3_bom))

    api(Libs.jackson3_core)
    api(Libs.jackson3_databind)

    compileOnly(Libs.jackson3_datatype_json_org)
    // compileOnly(Libs.jackson3_datatype_jsr353)
    compileOnly(Libs.jackson3_datatype_javax_money)
    compileOnly(Libs.jackson3_datatype_moneta)

    api(Libs.jackson3_module_kotlin)
    compileOnly(Libs.jackson3_module_blackbird)
    compileOnly(Libs.jackson3_module_no_ctor_deser)

    api(project(":bluetape4k-json"))
    api(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-crypto"))
    compileOnly(project(":bluetape4k-tink"))
    testImplementation(project(":bluetape4k-junit5"))

    testImplementation(Libs.jsonpath)
    testImplementation(Libs.jsonassert)

    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Jackson3 Dataformats Binary (from jackson3-binary)
    compileOnly(Libs.jackson3_dataformat_avro)
    compileOnly(Libs.jackson3_dataformat_cbor)
    compileOnly(Libs.jackson3_dataformat_ion)
    compileOnly(Libs.jackson3_dataformat_protobuf)
    compileOnly(Libs.jackson3_dataformat_smile)

    // Jackson3 Dataformats Text (from jackson3-text)
    compileOnly(Libs.jackson3_dataformat_csv)
    compileOnly(Libs.jackson3_dataformat_properties)
    compileOnly(Libs.jackson3_dataformat_yaml)
    compileOnly(Libs.jackson3_dataformat_toml)
}
