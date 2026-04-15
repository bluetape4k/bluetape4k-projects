plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.spring_boot3_dependencies))
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-netty"))
    api(project(":bluetape4k-idgenerators"))
    api(project(":bluetape4k-jackson2"))
    api(project(":bluetape4k-resilience4j"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // AWS SDK V2 Core (공통 필수)
    api(Libs.aws2_aws_core)
    api(Libs.aws2_apache_client)
    api(Libs.aws2_aws_crt_client)
    api(Libs.aws2_netty_nio_client)
    compileOnly(Libs.aws2_url_connection_client)

    // AWS SDK V2 Services (compileOnly - 사용자가 필요한 서비스만 런타임에 추가)
    compileOnly(Libs.aws2_dynamodb_enhanced)
    compileOnly(Libs.aws2_s3)
    compileOnly(Libs.aws2_s3_transfer_manager)
    compileOnly(Libs.aws2_aws_crt)
    compileOnly(Libs.aws2_ses)
    compileOnly(Libs.aws2_sns)
    compileOnly(Libs.aws2_sqs)
    compileOnly(Libs.aws2_kms)
    compileOnly(Libs.aws2_cloudwatch)
    compileOnly(Libs.aws2_cloudwatchlogs)
    compileOnly(Libs.aws2_kinesis)
    compileOnly(Libs.aws2_sts)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactive)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Test
    testImplementation(Libs.aws2_ec2)
    testImplementation(Libs.aws2_test_utils)
    testImplementation(Libs.testcontainers_localstack)
    testImplementation(Libs.mockk)
    testImplementation(Libs.awaitility_kotlin)

    // Spring Boot (dynamodb 테스트용)
    testImplementation(Libs.springBootStarter("webflux"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "junit", module = "junit")
        exclude(module = "mockito-core")
    }
    testImplementation(Libs.jakarta_el_api)
    testImplementation(Libs.hibernate_validator)
    compileOnly(Libs.commons_io)
}
