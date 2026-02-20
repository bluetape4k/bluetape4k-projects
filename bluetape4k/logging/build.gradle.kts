configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(Libs.kotlin_reflect)

    api(Libs.slf4j_api)
    implementation(Libs.jcl_over_slf4j)
    implementation(Libs.logback)

    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_slf4j)
    testImplementation(Libs.kotlinx_coroutines_test)
}
