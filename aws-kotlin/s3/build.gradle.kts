@Suppress("UnstableApiUsage")
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
    api(Libs.aws_kotlin_aws_endpoint)
    api(Libs.aws_kotlin_s3)

    // NOTE: AWS Kotlin이 OkHttp3 5.0.0-alpha 버전을 사용하는데, 기존 라이브러리들의 OkHttp3 4.0+ 과 충돌한다. 그래서 AWS 에서는 CRT 엔진을 사용하도록 권장한다.
    api(Libs.aws_smithy_kotlin_http)
    api(Libs.aws_smithy_kotlin_http_client_engine_crt)

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
