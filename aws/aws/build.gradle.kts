plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot3.dependencies))

    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-netty"))
    api(project(":bluetape4k-idgenerators"))
    compileOnly(project(":bluetape4k-jackson2"))
    compileOnly(project(":bluetape4k-resilience4j"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // AWS SDK V2 Core (공통 필수)
    api(libs.aws2.aws.core)
    api(libs.aws2.apache.client)
    api(libs.aws2.aws.crt.client)
    api(libs.aws2.netty.nio.client)
    compileOnly(libs.aws2.url.connection.client)

    // AWS SDK V2 Services (compileOnly - 사용자가 필요한 서비스만 런타임에 추가)
    compileOnly(libs.aws2.dynamodb.enhanced)
    compileOnly(libs.aws2.s3)
    compileOnly(libs.aws2.s3.transfer.manager)
    compileOnly(libs.aws2.aws.crt)
    compileOnly(libs.aws2.ses)
    compileOnly(libs.aws2.sns)
    compileOnly(libs.aws2.sqs)
    compileOnly(libs.aws2.kms)
    compileOnly(libs.aws2.cloudwatch)
    compileOnly(libs.aws2.cloudwatchlogs)
    compileOnly(libs.aws2.kinesis)
    compileOnly(libs.aws2.sts)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactive)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Test
    testImplementation(libs.aws2.ec2)
    testImplementation(libs.aws2.test.utils)
    testImplementation(libs.testcontainers.localstack)
    testImplementation(libs.mockk)
    testImplementation(libs.awaitility.kotlin)

    // Spring Boot (dynamodb 테스트용)
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "junit", module = "junit")
        exclude(module = "mockito-core")
    }
    testImplementation(libs.jakarta.el.api)
    testImplementation(libs.hibernate.validator)
    compileOnly(libs.commons.io)
}

tasks.test {
    systemProperty("bluetape4k.aws.emulator", System.getProperty("bluetape4k.aws.emulator", "localstack"))
}
