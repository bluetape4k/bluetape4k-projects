
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    // bluetape4k AWS Kotlin Modules
    api(project(":bluetape4k-aws-kotlin-core"))
    testImplementation(project(":bluetape4k-aws-kotlin-tests"))

    // AWS Kotlin SDK
    api(Libs.aws_kotlin_aws_core)
    api(Libs.aws_kotlin_aws_config)
    api(Libs.aws_kotlin_sns)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Testcontainers for AWS (LocalStack)
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_localstack)

    // bluetape4k
    testImplementation(project(":bluetape4k-idgenerators"))
}
