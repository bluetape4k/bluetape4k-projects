configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(libs.fastjson2)
    api(libs.fastjson2.kotlin)

    api(project(":bluetape4k-json"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    testImplementation(libs.jsonpath)
    testImplementation(libs.jsonassert)

    testImplementation(project(":bluetape4k-coroutines"))
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
