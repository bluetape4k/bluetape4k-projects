package io.bluetape4k.testcontainers.aws

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider

/**
 * 현재 AWS 에뮬레이터 서버의 접속 정보로 [StaticCredentialsProvider]를 생성합니다.
 *
 * AWS SDK v2의 `software.amazon.awssdk.auth.credentials` 패키지가 classpath에 있을 때만
 * 사용 가능합니다 (`compileOnly` 의존성으로 분리됨).
 *
 * ```kotlin
 * val provider = flociServer.getCredentialProvider()
 * val s3Client = S3Client.builder()
 *     .endpointOverride(flociServer.awsEndpoint)
 *     .credentialsProvider(provider)
 *     .build()
 * ```
 */
fun AwsEmulatorServer.getCredentialProvider(): StaticCredentialsProvider {
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(this.awsAccessKey, this.awsSecretKey)
    )
}
