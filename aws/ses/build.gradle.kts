configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-aws-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // AWS SDK V2
    api(Libs.aws2_aws_core)
    api(Libs.aws2_ses)
    api(Libs.aws2_netty_nio_client)
    testImplementation(Libs.aws2_test_utils)

    // Coroutines
    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_jdk8)
    testImplementation(Libs.kotlinx_coroutines_test)

    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_localstack)
}
