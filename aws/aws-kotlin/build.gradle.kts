configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // bluetape4k
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-coroutines"))
    compileOnly(project(":bluetape4k-jackson2"))
    compileOnly(project(":bluetape4k-resilience4j"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(project(":bluetape4k-idgenerators"))

    // AWS Kotlin SDK Core (공통 필수)
    api(Libs.aws_kotlin_aws_core)
    api(Libs.aws_kotlin_aws_config)
    api(Libs.aws_kotlin_aws_endpoint)
    api(Libs.aws_smithy_kotlin_http)
    api(Libs.aws_smithy_kotlin_http_client_engine_crt)
    implementation(Libs.aws_smithy_kotlin_http_client_engine_default)
    implementation(Libs.aws_smithy_kotlin_http_client_engine_okhttp)

    // AWS Kotlin SDK Services (compileOnly - 사용자가 필요한 서비스만 런타임에 추가)
    compileOnly(Libs.aws_kotlin_dynamodb)
    compileOnly(Libs.aws_kotlin_s3)
    compileOnly(Libs.aws_kotlin_ses)
    compileOnly(Libs.aws_kotlin_sesv2)
    compileOnly(Libs.aws_kotlin_sns)
    compileOnly(Libs.aws_kotlin_sqs)
    compileOnly(Libs.aws_kotlin_kms)
    compileOnly(Libs.aws_kotlin_cloudwatch)
    compileOnly(Libs.aws_kotlin_cloudwatchlogs)
    compileOnly(Libs.aws_kotlin_kinesis)
    compileOnly(Libs.aws_kotlin_sts)

    // Resilience4j
    compileOnly(Libs.resilience4j_retry)
    compileOnly(Libs.resilience4j_kotlin)

    // Jackson
    compileOnly(Libs.jackson_module_kotlin)
    compileOnly(Libs.jackson_module_blackbird)

    // Coroutines
    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Testcontainers
    testImplementation(Libs.testcontainers_localstack)
    testImplementation(Libs.mockk)
}
