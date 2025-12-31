plugins {
    kotlin("plugin.serialization")
}

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

    api(Libs.jackson_core)
    api(Libs.jackson_databind)
    api(Libs.jackson_datatype_jdk8)
    api(Libs.jackson_datatype_jsr310)
    api(Libs.jackson_module_kotlin)
    api(Libs.jackson_module_parameter_names)
    api(Libs.jackson_module_blackbird)

    compileOnly(Libs.jackson_dataformat_properties)
    compileOnly(Libs.jackson_dataformat_yaml)

    api(project(":bluetape4k-json"))
    api(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-crypto"))
    testImplementation(project(":bluetape4k-junit5"))

    // api(Libs.javax_json_api)
    api(Libs.jakarta_json_api)

    compileOnly(Libs.kotlinx_serialization_json_jvm)

    // Gson
    compileOnly(Libs.gson)
    compileOnly(Libs.gson_javatime_serializers)

    testImplementation(Libs.jsonpath)
    testImplementation(Libs.jsonassert)

    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
