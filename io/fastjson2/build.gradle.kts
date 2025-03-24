configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(Libs.fastjson2)
    api(Libs.fastjson2_kotlin)

    api(project(":bluetape4k-json"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    testImplementation(Libs.jsonpath)
    testImplementation(Libs.jsonassert)

    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
