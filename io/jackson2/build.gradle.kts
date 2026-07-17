configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform("com.fasterxml.jackson:jackson-bom:${bt4k.versions.jackson.asProvider().get()}"))
    implementation(platform(bt4k.spring.boot4.dependencies))

    api(libs.jackson.core)
    api(libs.jackson.databind)
    api(libs.jackson.datatype.jdk8)
    api(libs.jackson.datatype.jsr310)
    api(libs.jackson.module.kotlin)
    api(libs.jackson.module.parameter.names)
    api(libs.jackson.module.blackbird)

    compileOnly(libs.jackson.dataformat.properties)
    compileOnly(libs.jackson.dataformat.yaml)

    // Jackson Dataformats Binary (from jackson-binary)
    compileOnly(libs.jackson.dataformat.avro)
    compileOnly(libs.jackson.dataformat.cbor)
    compileOnly(libs.jackson.dataformat.ion)
    compileOnly(libs.jackson.dataformat.protobuf)
    compileOnly(libs.jackson.dataformat.smile)

    // Jackson Dataformats Text (from jackson-text)
    compileOnly(libs.jackson.dataformat.csv)
    compileOnly(libs.jackson.dataformat.toml)

    api(project(":bluetape4k-json"))
    api(project(":bluetape4k-io"))
    
    compileOnly(project(":bluetape4k-tink"))
    testImplementation(project(":bluetape4k-junit5"))

    // api(libs.jakarta.json.api)
    api(libs.jakarta.json.api)

    // Gson
    compileOnly(libs.gson)
    compileOnly(libs.gson.javatime.serializers)

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
}
