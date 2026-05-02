configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.jackson3.bom))
    implementation(platform(libs.spring.boot4.dependencies))

    api(libs.jackson3.core)
    api(libs.jackson3.databind)

    compileOnly(libs.jackson3.datatype.json.org)
    // compileOnly(libs.jackson3.datatype.jsr353)
    compileOnly(libs.jackson3.datatype.javax.money)
    compileOnly(libs.jackson3.datatype.moneta)

    api(libs.jackson3.module.kotlin)
    compileOnly(libs.jackson3.module.blackbird)
    compileOnly(libs.jackson3.module.no.ctor.deser)

    api(project(":bluetape4k-json"))
    api(project(":bluetape4k-io"))
    
    compileOnly(project(":bluetape4k-tink"))
    testImplementation(project(":bluetape4k-junit5"))

    testImplementation(libs.jsonpath)
    testImplementation(libs.jsonassert)

    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.coroutines.reactive)
    testImplementation("org.springframework:spring-context")
    testImplementation("org.springframework:spring-webflux")
    testRuntimeOnly(libs.reactor.netty)
    testImplementation(project(":bluetape4k-testcontainers"))

    // Jackson3 Dataformats Binary (from jackson3-binary)
    compileOnly(libs.jackson3.dataformat.avro)
    compileOnly(libs.jackson3.dataformat.cbor)
    compileOnly(libs.jackson3.dataformat.ion)
    compileOnly(libs.jackson3.dataformat.protobuf)
    compileOnly(libs.jackson3.dataformat.smile)

    // Jackson3 Dataformats Text (from jackson3-text)
    compileOnly(libs.jackson3.dataformat.csv)
    compileOnly(libs.jackson3.dataformat.properties)
    compileOnly(libs.jackson3.dataformat.yaml)
    compileOnly(libs.jackson3.dataformat.toml)
}
