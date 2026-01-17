
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(Libs.aws_kotlin_aws_core)
    api(Libs.aws_kotlin_aws_config)
    api(Libs.aws_kotlin_aws_endpoint)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    api(Libs.kotlinx_coroutines_test)

    // Test
    api(project(":bluetape4k-junit5"))

    // Testcontainers
    api(project(":bluetape4k-testcontainers"))
    api(Libs.testcontainers_localstack)

    testImplementation(Libs.mockk)
}
