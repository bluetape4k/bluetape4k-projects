configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Bluetape4k AWS Kotlin Modules
    api(project(":bluetape4k-aws-kotlin-core"))
    testImplementation(project(":bluetape4k-aws-kotlin-tests"))

    // Jackson
    implementation(project(":bluetape4k-jackson"))
    implementation(Libs.jackson_module_kotlin)
    implementation(Libs.jackson_module_blackbird)

    // Resilience4j
    implementation(project(":bluetape4k-resilience4j"))
    implementation(Libs.resilience4j_retry)
    implementation(Libs.resilience4j_kotlin)

    // AWS Kotlin SDK
    api(Libs.aws_kotlin_aws_core)
    api(Libs.aws_kotlin_aws_config)
    api(Libs.aws_kotlin_dynamodb)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // IdGenerators
    testImplementation(project(":bluetape4k-idgenerators"))

}
