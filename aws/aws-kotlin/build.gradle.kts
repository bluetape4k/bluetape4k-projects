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
    api(libs.aws.kotlin.aws.core)
    api(libs.aws.kotlin.aws.config)
    api(libs.aws.kotlin.aws.endpoint)
    api(libs.aws.smithy.kotlin.http)
    api(libs.aws.smithy.kotlin.http.client.engine.crt)
    implementation(libs.aws.smithy.kotlin.http.client.engine.default)
    implementation(libs.aws.smithy.kotlin.http.client.engine.okhttp)

    // AWS Kotlin SDK Services (compileOnly - 사용자가 필요한 서비스만 런타임에 추가)
    compileOnly(libs.aws.kotlin.dynamodb)
    compileOnly(libs.aws.kotlin.s3)
    compileOnly(libs.aws.kotlin.ses)
    compileOnly(libs.aws.kotlin.sesv2)
    compileOnly(libs.aws.kotlin.sns)
    compileOnly(libs.aws.kotlin.sqs)
    compileOnly(libs.aws.kotlin.kms)
    compileOnly(libs.aws.kotlin.cloudwatch)
    compileOnly(libs.aws.kotlin.cloudwatchlogs)
    compileOnly(libs.aws.kotlin.kinesis)
    compileOnly(libs.aws.kotlin.sts)

    // Resilience4j
    compileOnly(libs.resilience4j.retry)
    compileOnly(libs.resilience4j.kotlin)

    // Jackson
    compileOnly(libs.jackson.module.kotlin)
    compileOnly(libs.jackson.module.blackbird)

    // Coroutines
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Testcontainers
    testImplementation(libs.testcontainers.localstack)
    testImplementation(libs.mockk)
}

tasks.test {
    systemProperty("bluetape4k.aws.emulator", System.getProperty("bluetape4k.aws.emulator", "localstack"))
}
