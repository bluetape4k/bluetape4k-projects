package io.bluetape4k.aws.kotlin.tests

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.testcontainers.containers.localstack.LocalStackContainer.Service.S3
import org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS

class LocalStackContainerExtensionsTest {

    companion object: KLoggingChannel()

    @Test
    fun `AWS S3, SQS 서비스를 제공하는 LocalStackServer를 실행합니다`() {
        log.debug { "LocalStackServer with S3, SQS Services" }

        getLocalStackServer(S3, SQS).use { server ->
            val s3EndpointUrl = server.endpointUrl
            log.debug { "S3 Endpoint URL: $s3EndpointUrl" }
            server.endpoint.shouldNotBeNull()

            val sqsEndpointUrl = server.endpointUrl
            log.debug { "SQS Endpoint URL: $sqsEndpointUrl" }

            log.debug { "Region: ${server.region}" }
            server.region.shouldNotBeEmpty()

            val credentialsProvider = server.getCredentialsProvider()

            log.debug {
                """
                    CredentialProvider:
                        AccessKey: ${credentialsProvider.credentials.accessKeyId}
                        SecretKey: ${credentialsProvider.credentials.secretAccessKey}
                """.trimIndent()
            }
            credentialsProvider.credentials.accessKeyId.shouldNotBeEmpty()
            credentialsProvider.credentials.secretAccessKey.shouldNotBeEmpty()
        }
    }
}
