configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(Libs.kotlin_reflect)

    api(Libs.slf4j_api)
    implementation(Libs.jcl_over_slf4j)
    compileOnly(Libs.logback)

    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_slf4j)
    testImplementation(Libs.kotlinx_coroutines_test)
}
