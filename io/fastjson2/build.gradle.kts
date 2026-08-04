configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(bt4k.fastjson2)
    api(bt4k.fastjson2.kotlin)

    api(project(":bluetape4k-json"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    testImplementation(bt4k.jsonpath.v3)
    testImplementation(bt4k.jsonassert.v1)

    testImplementation(project(":bluetape4k-coroutines"))
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
