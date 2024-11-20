configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-netty"))
    testImplementation(project(":bluetape4k-junit5"))

    // AWS SDK V2
    api(Libs.aws2_aws_core)

    // AWS HTTP Clients
    compileOnly(Libs.aws2_aws_crt_client)
    compileOnly(Libs.aws2_netty_nio_client)
    compileOnly(Libs.aws2_apache_client)
    compileOnly(Libs.aws2_url_connection_client)

    testImplementation(Libs.aws2_ec2)
    testImplementation(Libs.aws2_s3)
    testImplementation(Libs.aws2_test_utils)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    testImplementation(Libs.mockk)
}
